package com.farmers.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conversational farmer profile — all fields are optional.
 * Fields are progressively populated from conversation context.
 * Intentionally separate from the old FarmerProfileDto (used by /api/recommend).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFarmerProfile {

    private String state;
    private String district;
    private Double landHolding;   // in acres
    private String crop;
    private Double annualIncome;  // in INR
    private String category;      // SC/ST/OBC/General
    private String farmerType;    // marginal/small/large
    private Integer age;
    private String gender;
    private String occupation;
}
