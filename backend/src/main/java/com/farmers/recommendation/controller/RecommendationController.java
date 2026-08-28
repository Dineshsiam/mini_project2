package com.farmers.recommendation.controller;

import com.farmers.recommendation.dto.FarmerProfileDto;
import com.farmers.recommendation.dto.RecommendationResponseDto;
import com.farmers.recommendation.service.RecommendationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@CrossOrigin(origins = "*")
@Tag(name = "Recommendation API", description = "Endpoint for triggering the hybrid recommendation engine")
public class RecommendationController {

    @Autowired
    private RecommendationEngine recommendationEngine;

    @PostMapping
    @Operation(summary = "Get scheme recommendations", description = "Submits a farmer profile to compute eligible schemes using a hybrid rule-matching and vector search algorithm")
    public ResponseEntity<List<RecommendationResponseDto>> getRecommendations(@Valid @RequestBody FarmerProfileDto profile) {
        List<RecommendationResponseDto> recommendations = recommendationEngine.recommendSchemes(profile);
        return ResponseEntity.ok(recommendations);
    }
}
