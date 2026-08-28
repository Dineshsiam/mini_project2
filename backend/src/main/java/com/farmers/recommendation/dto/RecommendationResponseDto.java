package com.farmers.recommendation.dto;

import com.farmers.recommendation.entity.Scheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponseDto {
    private int score;             // 0-100 scale score
    private String scheme;         // Scheme name
    private String reason;         // Detailed explanation for why the scheme is recommended
    private Scheme schemeDetails;  // Full scheme object details for the React frontend
}
