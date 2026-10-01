package com.farmers.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotNull(message = "sessionId is required")
    private UUID sessionId;

    @NotBlank(message = "message cannot be blank")
    private String message;

    @Builder.Default
    private String language = "en";
}
