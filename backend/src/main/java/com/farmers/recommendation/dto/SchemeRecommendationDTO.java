package com.farmers.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeRecommendationDTO {

    private UUID schemeId;
    private String name;
    private String reason;
    private String benefits;
    private String eligibility;
    private String sourceUrl;
}
