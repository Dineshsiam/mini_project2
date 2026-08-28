package com.farmers.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsDto {
    private long totalSchemes;
    private long centralSchemes;
    private long stateSchemes;
    private Map<String, Long> schemesByState;
    private Map<String, Long> schemesByCategory;
}
