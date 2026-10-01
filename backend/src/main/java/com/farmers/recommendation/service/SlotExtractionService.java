package com.farmers.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmers.recommendation.dto.ChatFarmerProfile;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Extracts structured farmer profile attributes from a natural language message.
 * Uses LLM with a strict JSON-output prompt to avoid hallucination.
 * Merges new slots with the existing profile (never overwrites non-null with null).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotExtractionService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EXTRACTION_PROMPT_TEMPLATE = """
            You are a precise information extractor for an Indian agricultural scheme recommendation system.
            
            Extract ONLY the explicitly mentioned details from the farmer's message.
            Do NOT infer or guess any value that is not clearly stated.
            
            Return a JSON object with ONLY the fields that are explicitly mentioned.
            Omit any field that is not clearly stated in the message.
            
            Possible fields:
            - state: Indian state name (e.g. "Tamil Nadu", "Maharashtra")
            - district: District name (e.g. "Salem", "Pune")
            - landHolding: Land in acres (number only, e.g. 3.0)
            - crop: Main crop grown (e.g. "rice", "wheat", "cotton")
            - annualIncome: Annual income in INR (number only, e.g. 200000 for "2 lakh")
            - category: Social category (one of: "SC", "ST", "OBC", "General")
            - farmerType: Type (one of: "marginal", "small", "large")
            - age: Age in years (number only)
            - gender: "Male" or "Female"
            - occupation: Occupation (e.g. "farmer", "agricultural laborer")
            
            Rules:
            - Convert "2 lakh" to 200000, "5 lakh" to 500000, etc.
            - If someone says "Salem" infer state as "Tamil Nadu" ONLY if Salem is unambiguously in TN.
            - If someone says "Coimbatore" → state = "Tamil Nadu", district = "Coimbatore"
            - Recognize common Indian district→state mappings.
            - Return only valid JSON. No explanation. No markdown. No code fences.
            - If nothing can be extracted, return: {}
            
            Farmer's message:
            {{message}}
            
            JSON:
            """;

    /**
     * Extracts slots from the message and merges them into the existing profile.
     * Non-null existing values are NEVER overwritten by null extracted values.
     */
    public ChatFarmerProfile extractAndMerge(String message, ChatFarmerProfile existing) {
        ChatFarmerProfile extracted = extractSlots(message);
        return merge(existing, extracted);
    }

    /**
     * Calls LLM and parses JSON output into a ChatFarmerProfile.
     * Returns empty profile on LLM error (fail-safe).
     */
    public ChatFarmerProfile extractSlots(String message) {
        try {
            String prompt = EXTRACTION_PROMPT_TEMPLATE.replace("{{message}}", message);
            String response = chatLanguageModel.generate(prompt);
            log.debug("Slot extraction LLM response: {}", response);
            return parseJson(response.trim());
        } catch (Exception e) {
            log.warn("Slot extraction failed gracefully: {}", e.getMessage());
            return new ChatFarmerProfile();
        }
    }

    /**
     * Merges two profiles. Non-null values from 'existing' are preserved.
     * New non-null values from 'extracted' fill in gaps.
     */
    public ChatFarmerProfile merge(ChatFarmerProfile existing, ChatFarmerProfile extracted) {
        if (existing == null) return extracted != null ? extracted : new ChatFarmerProfile();
        if (extracted == null) return existing;

        return ChatFarmerProfile.builder()
                .state(firstNonNull(existing.getState(), extracted.getState()))
                .district(firstNonNull(existing.getDistrict(), extracted.getDistrict()))
                .landHolding(firstNonNull(existing.getLandHolding(), extracted.getLandHolding()))
                .crop(firstNonNull(existing.getCrop(), extracted.getCrop()))
                .annualIncome(firstNonNull(existing.getAnnualIncome(), extracted.getAnnualIncome()))
                .category(firstNonNull(existing.getCategory(), extracted.getCategory()))
                .farmerType(firstNonNull(existing.getFarmerType(), extracted.getFarmerType()))
                .age(firstNonNull(existing.getAge(), extracted.getAge()))
                .gender(firstNonNull(existing.getGender(), extracted.getGender()))
                .occupation(firstNonNull(existing.getOccupation(), extracted.getOccupation()))
                .build();
    }

    private <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private ChatFarmerProfile parseJson(String json) {
        // Strip markdown code fences if LLM returned them despite instructions
        if (json.startsWith("```")) {
            json = json.replaceAll("```[a-z]*", "").replace("```", "").trim();
        }
        if (json.isEmpty() || json.equals("{}")) {
            return new ChatFarmerProfile();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            ChatFarmerProfile profile = new ChatFarmerProfile();
            if (node.has("state")) profile.setState(node.get("state").asText(null));
            if (node.has("district")) profile.setDistrict(node.get("district").asText(null));
            if (node.has("landHolding") && !node.get("landHolding").isNull())
                profile.setLandHolding(node.get("landHolding").asDouble());
            if (node.has("crop")) profile.setCrop(node.get("crop").asText(null));
            if (node.has("annualIncome") && !node.get("annualIncome").isNull())
                profile.setAnnualIncome(node.get("annualIncome").asDouble());
            if (node.has("category")) profile.setCategory(node.get("category").asText(null));
            if (node.has("farmerType")) profile.setFarmerType(node.get("farmerType").asText(null));
            if (node.has("age") && !node.get("age").isNull())
                profile.setAge(node.get("age").asInt());
            if (node.has("gender")) profile.setGender(node.get("gender").asText(null));
            if (node.has("occupation")) profile.setOccupation(node.get("occupation").asText(null));
            return profile;
        } catch (Exception e) {
            log.warn("Failed to parse slot extraction JSON: '{}' — {}", json, e.getMessage());
            return new ChatFarmerProfile();
        }
    }
}
