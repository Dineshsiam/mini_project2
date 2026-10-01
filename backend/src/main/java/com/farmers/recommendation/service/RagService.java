package com.farmers.recommendation.service;

import com.farmers.recommendation.dto.ChatFarmerProfile;
import com.farmers.recommendation.dto.SchemeRecommendationDTO;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieves relevant schemes using pgvector similarity and optional state filtering.
 * Reuses the existing EmbeddingService and SchemeRepository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final SchemeRepository schemeRepository;
    private final EmbeddingService embeddingService;

    private static final int TOP_K = 10;
    private static final int CANDIDATE_POOL = 50;

    /**
     * Retrieves top-K relevant schemes for the given farmer profile.
     * Applies a soft state filter (Central schemes always pass through).
     */
    public List<SchemeRecommendationDTO> retrieveRelevantSchemes(ChatFarmerProfile profile) {
        String queryText = buildQueryText(profile);
        if (queryText.isBlank()) {
            log.debug("RAG: empty query text, returning empty results");
            return Collections.emptyList();
        }

        float[] embedding = embeddingService.getEmbedding(queryText);
        String vectorStr = embeddingService.getVectorString(embedding);

        List<Object[]> nearestResults = schemeRepository.findNearestNeighborIdsWithSimilarity(vectorStr, CANDIDATE_POOL);
        if (nearestResults.isEmpty()) return Collections.emptyList();

        Map<UUID, Double> similarityMap = new LinkedHashMap<>();
        List<UUID> candidateIds = new ArrayList<>();
        for (Object[] row : nearestResults) {
            UUID id = (UUID) row[0];
            Double similarity = (Double) row[1];
            similarityMap.put(id, similarity);
            candidateIds.add(id);
        }

        List<Scheme> candidates = schemeRepository.findAllById(candidateIds);

        // Sort by similarity (descending) and apply state filter
        return candidates.stream()
                .filter(s -> passesStateFilter(s, profile))
                .sorted(Comparator.comparingDouble(
                        (Scheme s) -> similarityMap.getOrDefault(s.getId(), 0.0)).reversed())
                .limit(TOP_K)
                .map(s -> toDTO(s, similarityMap.getOrDefault(s.getId(), 0.0)))
                .collect(Collectors.toList());
    }

    /**
     * Builds a descriptive search query from whatever profile fields we have.
     */
    private String buildQueryText(ChatFarmerProfile p) {
        StringBuilder sb = new StringBuilder();
        if (p.getOccupation() != null) sb.append(p.getOccupation()).append(" ");
        else sb.append("farmer ");
        if (p.getState() != null) sb.append("from ").append(p.getState()).append(" ");
        if (p.getCrop() != null) sb.append("grows ").append(p.getCrop()).append(" ");
        if (p.getLandHolding() != null) sb.append("land ").append(p.getLandHolding()).append(" acres ");
        if (p.getAnnualIncome() != null) sb.append("income ").append(p.getAnnualIncome()).append(" INR ");
        if (p.getCategory() != null) sb.append("category ").append(p.getCategory()).append(" ");
        if (p.getGender() != null) sb.append("gender ").append(p.getGender()).append(" ");
        if (p.getFarmerType() != null) sb.append(p.getFarmerType()).append(" farmer ");
        return sb.toString().trim();
    }

    private boolean passesStateFilter(Scheme scheme, ChatFarmerProfile profile) {
        if ("Central".equalsIgnoreCase(scheme.getState())) return true;
        if (profile.getState() == null) return true; // No state known yet → include all
        return scheme.getState().equalsIgnoreCase(profile.getState());
    }

    private SchemeRecommendationDTO toDTO(Scheme scheme, double similarity) {
        // Build a brief reason mentioning key fields
        String reason = buildReason(scheme, similarity);
        String sourceUrl = scheme.getSourceUrl() != null ? scheme.getSourceUrl()
                : (scheme.getApplicationLink() != null ? scheme.getApplicationLink() : null);
        return SchemeRecommendationDTO.builder()
                .schemeId(scheme.getId())
                .name(scheme.getName())
                .reason(reason)
                .benefits(truncate(scheme.getBenefits(), 300))
                .eligibility(truncate(scheme.getEligibility(), 300))
                .sourceUrl(sourceUrl)
                .build();
    }

    private String buildReason(Scheme scheme, double similarity) {
        int pct = (int) Math.round(similarity * 100);
        return String.format("Semantically matched at %d%% confidence. State: %s. Level: %s.",
                pct, scheme.getState(), scheme.getLevel() != null ? scheme.getLevel() : "N/A");
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
