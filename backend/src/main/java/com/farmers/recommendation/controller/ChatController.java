package com.farmers.recommendation.controller;

import com.farmers.recommendation.dto.ChatFarmerProfile;
import com.farmers.recommendation.dto.ChatRequest;
import com.farmers.recommendation.dto.ChatResponse;
import com.farmers.recommendation.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // In production, restrict this
@RequiredArgsConstructor
@Tag(name = "Chat API", description = "Endpoints for conversational AI schema recommendations")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    @Operation(summary = "Send a message (blocking)", description = "Sends a message and returns the full JSON response")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message (streaming)", description = "Sends a message and streams the AI's response via Server-Sent Events (SSE)")
    public SseEmitter sendMessageStream(@Valid @RequestBody ChatRequest request) {
        return chatService.processMessageStreaming(request);
    }

    @GetMapping("/session/{sessionId}/profile")
    @Operation(summary = "Get extracted farmer profile", description = "Gets the structured profile extracted from the current chat session")
    public ResponseEntity<ChatFarmerProfile> getSessionProfile(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatService.getProfile(sessionId));
    }
}
