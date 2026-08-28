package com.farmers.recommendation.service;

import com.farmers.recommendation.dto.FarmerProfileDto;
import com.farmers.recommendation.dto.RecommendationResponseDto;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.repository.SchemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationEngine {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * Recommends Top 5 schemes for a farmer based on rules and semantic embedding search.
     */
    public List<RecommendationResponseDto> recommendSchemes(FarmerProfileDto profile) {
        log.info("Processing recommendation request for profile: {}", profile);

        // 1. Convert Farmer Profile into a search description string
        String searchDescription = buildSearchText(profile);
        log.debug("Generated search text: {}", searchDescription);

        // 2. Generate embedding vector
        float[] queryEmbedding = embeddingService.getEmbedding(searchDescription);
        String vectorStr = embeddingService.getVectorString(queryEmbedding);

        // 3. Query pgvector for Top 50 similar candidate schemes
        // Returns list of [UUID id, Double similarity]
        List<Object[]> nearestResults = schemeRepository.findNearestNeighborIdsWithSimilarity(vectorStr, 50);
        log.debug("Found {} potential candidates from vector search", nearestResults.size());

        if (nearestResults.isEmpty()) {
            return Collections.emptyList();
        }

        // Map UUIDs to similarity scores
        Map<UUID, Double> similarityMap = new HashMap<>();
        List<UUID> candidateIds = new ArrayList<>();
        for (Object[] row : nearestResults) {
            UUID id = (UUID) row[0];
            Double similarity = (Double) row[1];
            similarityMap.put(id, similarity);
            candidateIds.add(id);
        }

        // Fetch Scheme objects for the candidates
        List<Scheme> candidates = schemeRepository.findAllById(candidateIds);

        // 4. Score and filter candidates using Hybrid Scoring
        List<RecommendationResponseDto> recommendations = new ArrayList<>();

        for (Scheme scheme : candidates) {
            double similarity = similarityMap.getOrDefault(scheme.getId(), 0.0);
            
            // Calculate Rule Score (0.0 to 1.0) and gather reasoning
            RuleScoreResult ruleResult = evaluateRules(profile, scheme);
            
            if (ruleResult.ruleScore <= 0.0) {
                // Reject candidate if it fails hard constraints (like state or gender mismatch)
                log.debug("Candidate scheme {} rejected: {}", scheme.getName(), ruleResult.reason);
                continue;
            }

            // Calculate final hybrid score:
            // Score = 70% Rule Score + 30% Semantic Similarity
            // Rescaled to 0-100
            double hybridScore = (0.7 * ruleResult.ruleScore + 0.3 * similarity) * 100;
            int finalScoreInt = (int) Math.round(hybridScore);

            recommendations.add(RecommendationResponseDto.builder()
                    .score(Math.min(100, Math.max(0, finalScoreInt)))
                    .scheme(scheme.getName())
                    .reason(ruleResult.reason)
                    .schemeDetails(scheme)
                    .build());
        }

        // Sort by final score descending and pick Top 5
        return recommendations.stream()
                .sorted(Comparator.comparingInt(RecommendationResponseDto::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Builds a comprehensive text description of the farmer profile for semantic matching.
     */
    private String buildSearchText(FarmerProfileDto profile) {
        StringBuilder sb = new StringBuilder();
        sb.append(profile.getOccupation()).append(" farmer from ").append(profile.getState()).append(". ");
        sb.append("Gender: ").append(profile.getGender()).append(". ");
        sb.append("Age: ").append(profile.getAge()).append(" years old. ");
        sb.append("Social Category: ").append(profile.getCategory()).append(". ");
        sb.append("Annual income: Rs. ").append(Math.round(profile.getIncome())).append(". ");
        sb.append("Land holding: ").append(profile.getLandHolding()).append(" acres. ");
        
        if (profile.isDisability()) {
            sb.append("Farmer has physical disability. ");
        }
        if (profile.getEducation() != null && !profile.getEducation().trim().isEmpty()) {
            sb.append("Education level: ").append(profile.getEducation()).append(". ");
        }
        if (profile.getKeywords() != null && !profile.getKeywords().trim().isEmpty()) {
            sb.append("Needs support for: ").append(profile.getKeywords()).append(". ");
        }
        
        return sb.toString();
    }

    /**
     * Evaluation result holding score and reasoning text.
     */
    private static class RuleScoreResult {
        double ruleScore;
        String reason;

        RuleScoreResult(double ruleScore, String reason) {
            this.ruleScore = ruleScore;
            this.reason = reason;
        }
    }

    /**
     * Validates farmer profile eligibility against scheme characteristics (Rules Engine).
     */
    private RuleScoreResult evaluateRules(FarmerProfileDto profile, Scheme scheme) {
        double score = 1.0;
        List<String> reasonNotes = new ArrayList<>();

        String eligibilityText = (scheme.getEligibility() != null ? scheme.getEligibility() : "").toLowerCase();
        String descriptionText = (scheme.getDescription() != null ? scheme.getDescription() : "").toLowerCase();
        String benefitsText = (scheme.getBenefits() != null ? scheme.getBenefits() : "").toLowerCase();
        String nameText = scheme.getName().toLowerCase();
        
        String fullSchemeText = nameText + " " + descriptionText + " " + eligibilityText + " " + benefitsText;

        // RULE 1: State Filter (Hard constraint)
        // Scheme must be Central or match the farmer's state
        if (!scheme.getState().equalsIgnoreCase("Central") && !scheme.getState().equalsIgnoreCase(profile.getState())) {
            return new RuleScoreResult(0.0, "This state-specific scheme is restricted to " + scheme.getState() + " residents.");
        }
        reasonNotes.add("Matches state residency requirements (" + scheme.getState() + ").");

        // RULE 2: Gender Constraint (Hard constraint)
        // If scheme is specific to females/girls and farmer is male, reject
        boolean femaleScheme = fullSchemeText.contains("women") || fullSchemeText.contains("female") || 
                               fullSchemeText.contains("girl") || fullSchemeText.contains("mahila");
        if (femaleScheme && profile.getGender().equalsIgnoreCase("Male")) {
            return new RuleScoreResult(0.0, "This scheme is specifically targeted towards women/female beneficiaries.");
        }
        if (femaleScheme && profile.getGender().equalsIgnoreCase("Female")) {
            score += 0.1; // Boost for matching target demographic
            reasonNotes.add("Matches targeted gender demographic.");
        }

        // RULE 3: Category Filtering (SC/ST/OBC/General)
        // Check if scheme mentions SC/ST limits
        boolean scstScheme = fullSchemeText.contains("sc/st") || fullSchemeText.contains("scheduled caste") || 
                             fullSchemeText.contains("scheduled tribe");
        if (scstScheme) {
            if (profile.getCategory().equalsIgnoreCase("SC") || profile.getCategory().equalsIgnoreCase("ST")) {
                score += 0.15; // Higher match score
                reasonNotes.add("Eligible under SC/ST special provisions.");
            } else if (profile.getCategory().equalsIgnoreCase("General")) {
                // If it's SC/ST only, penalize or filter. Let's penalize soft limits.
                score -= 0.3;
                reasonNotes.add("Priority is given to SC/ST categories; lower match probability for General category.");
            }
        }

        // RULE 4: Income Constraints (Below Poverty Line, Income thresholds)
        // Look for typical annual income ceilings, e.g. "below poverty line" or "bpl"
        boolean bplScheme = fullSchemeText.contains("bpl") || fullSchemeText.contains("below poverty line") || 
                            fullSchemeText.contains("marginalized");
        if (bplScheme) {
            if (profile.getIncome() <= 120000) {
                score += 0.1;
                reasonNotes.add("Eligible under low-income/BPL thresholds.");
            } else {
                score -= 0.4;
                reasonNotes.add("Scheme targets BPL/low-income families. Your income may exceed the threshold.");
            }
        }

        // RULE 5: Land Holding constraints (Small & Marginal Farmers)
        // Marginal: < 2.5 acres (1 hectare), Small: 2.5 - 5 acres (2 hectares), Large: > 5 acres
        boolean smallMarginalTargeted = fullSchemeText.contains("small and marginal") || 
                                        fullSchemeText.contains("marginal farmer") ||
                                        fullSchemeText.contains("landless");
        if (smallMarginalTargeted) {
            if (profile.getLandHolding() <= 2.5) {
                score += 0.15;
                reasonNotes.add("Eligible as a marginal landholder (<= 2.5 acres).");
            } else if (profile.getLandHolding() <= 5.0) {
                score += 0.05;
                reasonNotes.add("Eligible as a small landholder (<= 5 acres).");
            } else {
                score -= 0.5; // Big penalty
                reasonNotes.add("This scheme is designed for small/marginal farmers; large landholdings are generally excluded.");
            }
        }

        // RULE 6: Disability check
        boolean disabilityScheme = fullSchemeText.contains("handicapped") || fullSchemeText.contains("disab") || 
                                   fullSchemeText.contains("divyang");
        if (disabilityScheme) {
            if (profile.isDisability()) {
                score += 0.2;
                reasonNotes.add("Matches disability support criteria (Divyangjan).");
            } else {
                score -= 0.4;
                reasonNotes.add("Priority is given to physically challenged beneficiaries.");
            }
        }

        // RULE 7: Keyword boost (Enhances score if search queries match scheme content)
        if (profile.getKeywords() != null && !profile.getKeywords().trim().isEmpty()) {
            String[] keywords = profile.getKeywords().toLowerCase().split("\\s+");
            int matches = 0;
            for (String kw : keywords) {
                if (kw.length() > 2 && fullSchemeText.contains(kw)) {
                    matches++;
                }
            }
            if (matches > 0) {
                score += Math.min(0.2, matches * 0.05); // Boost score up to 0.2
                reasonNotes.add("Directly matches keywords for: " + profile.getKeywords());
            }
        }

        // Construct final explanation reason
        String reason = reasonNotes.isEmpty() ? 
                "Matches standard eligibility criteria." : 
                String.join(" ", reasonNotes);

        return new RuleScoreResult(Math.max(0.1, score), reason);
    }
}
