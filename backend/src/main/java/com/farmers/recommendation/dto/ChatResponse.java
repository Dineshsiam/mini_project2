package com.farmers.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private UUID sessionId;

    /** The assistant's text reply */
    private String message;

    /** Updated farmer profile extracted from this and previous messages */
    private ChatFarmerProfile farmerProfile;

    /** Scheme recommendations grounded in retrieved data */
    private List<SchemeRecommendationDTO> recommendations;
}
