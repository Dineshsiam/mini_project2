package com.farmers.recommendation.service;

import com.farmers.recommendation.dto.ChatFarmerProfile;
import com.farmers.recommendation.dto.ChatRequest;
import com.farmers.recommendation.dto.ChatResponse;
import com.farmers.recommendation.dto.SchemeRecommendationDTO;
import com.farmers.recommendation.entity.ChatMessage;
import com.farmers.recommendation.entity.Conversation;
import com.farmers.recommendation.repository.ChatMessageRepository;
import com.farmers.recommendation.repository.ConversationRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Core chat orchestration service.
 * Pipeline: load session → save user msg → extract slots → RAG → LLM prompt → save assistant msg → return
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SlotExtractionService slotExtractionService;
    private final RagService ragService;
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;

    // In-memory profile store keyed by conversationId
    // For a real production app this would be in Redis, but for a mini-project
    // keeping it in-memory is fine (server restart resets profile, which is acceptable).
    private final Map<UUID, ChatFarmerProfile> profileStore = new java.util.concurrent.ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${app.llm.api-key:}")
    private String llmApiKey;

    private static final String SYSTEM_PROMPT = """
            You are Kisan Setu AI, a helpful and friendly government agricultural scheme assistant for Indian farmers.
            Your goal is to help farmers discover relevant central and state government schemes.
            
            Rules:
            1. Always answer based ONLY on the retrieved scheme information provided below.
            2. Never invent, guess, or fabricate eligibility criteria, benefits, deadlines, amounts, or URLs.
            3. If the provided scheme data does not contain the answer, say so honestly.
            4. Ask for missing profile information naturally — but only one question at a time.
            5. Be concise, warm, and use simple language a farmer can understand.
            6. After providing scheme information, briefly explain why each scheme may be relevant.
            7. If the user writes in Tamil, respond in Tamil.
            """;

    /**
     * Process a chat message and return a full (non-streaming) response.
     */
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        UUID sessionId = request.getSessionId();

        // 1. Load or create conversation
        Conversation conversation = conversationRepository.findById(sessionId)
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .id(sessionId)
                            .language(request.getLanguage() != null ? request.getLanguage() : "en")
                            .build();
                    return conversationRepository.save(c);
                });

        // Update language if changed
        if (request.getLanguage() != null && !request.getLanguage().equals(conversation.getLanguage())) {
            conversation.setLanguage(request.getLanguage());
            conversationRepository.save(conversation);
        }

        // 2. Save user message
        saveMessage(sessionId, "USER", request.getMessage());

        // 3. Extract and merge farmer profile
        ChatFarmerProfile existingProfile = profileStore.getOrDefault(sessionId, new ChatFarmerProfile());
        ChatFarmerProfile updatedProfile = slotExtractionService.extractAndMerge(request.getMessage(), existingProfile);
        profileStore.put(sessionId, updatedProfile);
        log.debug("Updated profile for session {}: {}", sessionId, updatedProfile);

        // 4. Retrieve relevant schemes via RAG
        List<SchemeRecommendationDTO> schemes = ragService.retrieveRelevantSchemes(updatedProfile);

        // 5. Build LLM prompt
        List<ChatMessage> history = chatMessageRepository.findTop20ByConversationIdOrderByCreatedAtAsc(sessionId);
        String prompt = buildLlmPrompt(request.getMessage(), updatedProfile, schemes, history, conversation.getLanguage());

        // 6. Call LLM
        String assistantReply;
        if (isLlmDisabled()) {
            log.info("LLM configuration is missing; using local fallback response for session {}", sessionId);
            assistantReply = buildFallbackReply(request.getMessage(), updatedProfile, schemes, conversation.getLanguage());
        } else {
            try {
                assistantReply = chatLanguageModel.generate(prompt);
            } catch (Exception e) {
                log.warn("LLM call failed, using local fallback response: {}", e.getMessage());
                assistantReply = buildFallbackReply(request.getMessage(), updatedProfile, schemes, conversation.getLanguage());
            }
        }

        // 7. Save assistant message
        saveMessage(sessionId, "ASSISTANT", assistantReply);

        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(assistantReply)
                .farmerProfile(updatedProfile)
                .recommendations(schemes.stream().limit(5).collect(Collectors.toList()))
                .build();
    }

    /**
     * Process a chat message and stream the LLM response via SSE.
     * Returns the SseEmitter immediately; the response streams asynchronously.
     */
    public SseEmitter processMessageStreaming(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 minute timeout

        executor.submit(() -> {
            UUID sessionId = request.getSessionId();
            try {
                // Load/create conversation
                Conversation conversation = conversationRepository.findById(sessionId)
                        .orElseGet(() -> conversationRepository.save(
                                Conversation.builder()
                                        .id(sessionId)
                                        .language(request.getLanguage() != null ? request.getLanguage() : "en")
                                        .build()));

                // Save user message
                saveMessage(sessionId, "USER", request.getMessage());

                // Extract slots
                ChatFarmerProfile existing = profileStore.getOrDefault(sessionId, new ChatFarmerProfile());
                ChatFarmerProfile updated = slotExtractionService.extractAndMerge(request.getMessage(), existing);
                profileStore.put(sessionId, updated);

                // RAG retrieval
                List<SchemeRecommendationDTO> schemes = ragService.retrieveRelevantSchemes(updated);

                // Build prompt
                List<ChatMessage> history = chatMessageRepository.findTop20ByConversationIdOrderByCreatedAtAsc(sessionId);
                String prompt = buildLlmPrompt(request.getMessage(), updated, schemes, history, conversation.getLanguage());

                // Send profile update immediately (before streaming text starts)
                ChatResponse profileEvent = ChatResponse.builder()
                        .sessionId(sessionId)
                        .farmerProfile(updated)
                        .recommendations(schemes.stream().limit(5).collect(Collectors.toList()))
                        .message("") // text will come via "chunk" events
                        .build();
                emitter.send(SseEmitter.event().name("profile").data(profileEvent));

                // Stream LLM response
                StringBuilder fullResponse = new StringBuilder();
                Object lock = new Object();
                boolean[] done = {false};

                if (isLlmDisabled()) {
                    String fallback = buildFallbackReply(request.getMessage(), updated, schemes, conversation.getLanguage());
                    fullResponse.append(fallback);
                    saveMessage(sessionId, "ASSISTANT", fallback);
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(fallback));
                        emitter.send(SseEmitter.event().name("done").data(""));
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("SSE fallback send failed: {}", e.getMessage());
                    }
                    synchronized (lock) { done[0] = true; lock.notifyAll(); }
                } else {
                    streamingChatLanguageModel.generate(prompt, new StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            fullResponse.append(token);
                            try {
                                emitter.send(SseEmitter.event().name("chunk").data(token));
                            } catch (IOException e) {
                                log.warn("SSE send failed: {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                            // Save the complete assistant message
                            saveMessage(sessionId, "ASSISTANT", fullResponse.toString());
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                                emitter.complete();
                            } catch (IOException e) {
                                log.warn("SSE complete failed: {}", e.getMessage());
                            }
                            synchronized (lock) { done[0] = true; lock.notifyAll(); }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.warn("LLM streaming error, using local fallback response: {}", error.getMessage());
                            String fallback = buildFallbackReply(request.getMessage(), updated, schemes, conversation.getLanguage());
                            try {
                                fullResponse.append(fallback);
                                emitter.send(SseEmitter.event().name("chunk").data(fallback));
                                emitter.send(SseEmitter.event().name("done").data(""));
                                emitter.complete();
                            } catch (IOException e) {
                                log.warn("SSE fallback send failed: {}", e.getMessage());
                            }
                            synchronized (lock) { done[0] = true; lock.notifyAll(); }
                        }
                    });
                }

            } catch (Exception e) {
                log.error("Streaming chat error: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("Internal server error"));
                    emitter.completeWithError(e);
                } catch (IOException ioe) {
                    log.warn("Failed to send error event: {}", ioe.getMessage());
                }
            }
        });

        return emitter;
    }

    /**
     * Returns the current profile for a session (for sidebar polling).
     */
    public ChatFarmerProfile getProfile(UUID sessionId) {
        return profileStore.getOrDefault(sessionId, new ChatFarmerProfile());
    }

    private void saveMessage(UUID conversationId, String role, String content) {
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
        chatMessageRepository.save(msg);
    }

    private boolean isLlmDisabled() {
        return llmApiKey == null || llmApiKey.isBlank() || "demo-key".equalsIgnoreCase(llmApiKey.trim());
    }

    private String buildFallbackReply(String userMessage, ChatFarmerProfile profile,
                                      List<SchemeRecommendationDTO> schemes, String language) {
        boolean tamil = "ta".equalsIgnoreCase(language);
        StringBuilder sb = new StringBuilder();

        if (tamil) {
            sb.append("வணக்கம்! உங்கள் விவரங்களின் அடிப்படையில், பின்வரும் திட்டங்கள் தொடர்பானதாகத் தோன்றுகின்றன:\n\n");
        } else {
            sb.append("Here are the most relevant schemes based on the details shared so far:\n\n");
        }

        if (schemes == null || schemes.isEmpty()) {
            if (tamil) {
                sb.append("தயவுசெய்து உங்கள் மாகாணம், பயிர், நிலப்பரப்பு மற்றும் வருமானம் பற்றி இன்னும் கொஞ்சம் கூறுங்கள். அதன் பிறகு நான் சிறந்த திட்டங்களை பரிந்துரைப்பேன்.");
            } else {
                sb.append("Please share a bit more about your state, crop, landholding, and income so I can suggest the best scheme options.");
            }
            return sb.toString();
        }

        for (int i = 0; i < Math.min(3, schemes.size()); i++) {
            SchemeRecommendationDTO scheme = schemes.get(i);
            sb.append(i + 1).append(". ").append(scheme.getName()).append("\n");
            if (tamil) {
                sb.append("   - ஏன் பொருத்தம்: ").append(scheme.getReason() != null ? scheme.getReason() : "இந்தத் திட்டம் உங்கள் விவசாய விவரங்களுடன் ஒத்துப்போகிறது.").append("\n");
            } else {
                sb.append("   - Why it may fit: ").append(scheme.getReason() != null ? scheme.getReason() : "This scheme aligns with the farmer profile provided.").append("\n");
            }
        }

        if (profile == null || profileSummary(profile).equals("Profile not yet populated.")) {
            if (tamil) {
                sb.append("\nஅடுத்ததாக, உங்கள் மாவட்டம், பயிர், மற்றும் ஆண்டு வருமானத்தை சொல்லுங்கள். இதை அறிந்தால், பரிந்துரைகளை இன்னும் துல்லியமாகச் சொல்ல முடியும்.");
            } else {
                sb.append("\nTo narrow this down further, please share your district, crop, and annual income. That helps refine the recommendations.");
            }
        }

        return sb.toString();
    }

    private String buildLlmPrompt(String userMessage, ChatFarmerProfile profile,
                                   List<SchemeRecommendationDTO> schemes,
                                   List<ChatMessage> history, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");

        // Farmer profile context
        sb.append("=== FARMER PROFILE ===\n");
        sb.append(profileSummary(profile)).append("\n\n");

        // Retrieved schemes context
        if (!schemes.isEmpty()) {
            sb.append("=== RETRIEVED SCHEME INFORMATION ===\n");
            sb.append("The following schemes were retrieved from the database and may be relevant:\n\n");
            for (int i = 0; i < schemes.size(); i++) {
                SchemeRecommendationDTO s = schemes.get(i);
                sb.append(i + 1).append(". SCHEME: ").append(s.getName()).append("\n");
                if (s.getEligibility() != null) sb.append("   Eligibility: ").append(s.getEligibility()).append("\n");
                if (s.getBenefits() != null) sb.append("   Benefits: ").append(s.getBenefits()).append("\n");
                if (s.getSourceUrl() != null) sb.append("   Source: ").append(s.getSourceUrl()).append("\n");
                sb.append("\n");
            }
        } else {
            sb.append("=== RETRIEVED SCHEME INFORMATION ===\n");
            sb.append("No schemes retrieved yet. Ask the farmer for more details about their location, crops, and income.\n\n");
        }

        // Conversation history (last few messages for context)
        if (!history.isEmpty()) {
            sb.append("=== CONVERSATION HISTORY ===\n");
            List<ChatMessage> recentHistory = history.stream()
                    .filter(m -> !"SYSTEM".equals(m.getRole()))
                    .collect(Collectors.toList());
            int start = Math.max(0, recentHistory.size() - 8); // last 8 messages
            for (ChatMessage msg : recentHistory.subList(start, recentHistory.size())) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("LANGUAGE: Respond in ").append("ta".equals(language) ? "Tamil" : "English").append(".\n\n");
        sb.append("USER: ").append(userMessage).append("\n");
        sb.append("ASSISTANT:");
        return sb.toString();
    }

    private String profileSummary(ChatFarmerProfile p) {
        List<String> parts = new ArrayList<>();
        if (p.getState() != null) parts.add("State: " + p.getState());
        if (p.getDistrict() != null) parts.add("District: " + p.getDistrict());
        if (p.getLandHolding() != null) parts.add("Land: " + p.getLandHolding() + " acres");
        if (p.getCrop() != null) parts.add("Crop: " + p.getCrop());
        if (p.getAnnualIncome() != null) parts.add("Annual Income: ₹" + p.getAnnualIncome().longValue());
        if (p.getCategory() != null) parts.add("Category: " + p.getCategory());
        if (p.getGender() != null) parts.add("Gender: " + p.getGender());
        if (p.getAge() != null) parts.add("Age: " + p.getAge());
        return parts.isEmpty() ? "Profile not yet populated." : String.join(", ", parts);
    }
}
