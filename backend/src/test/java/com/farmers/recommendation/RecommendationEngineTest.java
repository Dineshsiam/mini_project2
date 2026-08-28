package com.farmers.recommendation;

import com.farmers.recommendation.dto.FarmerProfileDto;
import com.farmers.recommendation.dto.RecommendationResponseDto;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.repository.SchemeRepository;
import com.farmers.recommendation.service.EmbeddingService;
import com.farmers.recommendation.service.RecommendationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class RecommendationEngineTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private RecommendationEngine recommendationEngine;

    private Scheme centralScheme;
    private Scheme stateSchemeTamilNadu;
    private Scheme femaleOnlyScheme;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Define mock embedding return
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(embeddingService.getVectorString(any())).thenReturn("[]");

        // Create dummy candidate schemes
        centralScheme = Scheme.builder()
                .id(UUID.randomUUID())
                .slug("central-pm-kisan")
                .name("PM Kisan Income Support")
                .state("Central")
                .level("Central")
                .category(List.of("Agriculture"))
                .description("Provides income support of 6000 per year to landholding farmers.")
                .eligibility("All landholding farmer families are eligible.")
                .build();

        stateSchemeTamilNadu = Scheme.builder()
                .id(UUID.randomUUID())
                .slug("tn-drip-subsidy")
                .name("Tamil Nadu Drip Irrigation Subsidy")
                .state("Tamil Nadu")
                .level("State")
                .category(List.of("Irrigation"))
                .description("Provides 100 percent subsidy for drip irrigation system.")
                .eligibility("Small and marginal farmers of Tamil Nadu are eligible.")
                .build();

        femaleOnlyScheme = Scheme.builder()
                .id(UUID.randomUUID())
                .slug("women-horticulture")
                .name("Mahila Horticulturist Assistance")
                .state("Central")
                .level("Central")
                .category(List.of("Horticulture"))
                .description("Assists women farmers in setting up kitchen gardens.")
                .eligibility("Female horticulturists and women farmers are eligible.")
                .build();
    }

    @Test
    void testRecommendationFiltersByState() {
        // Farmer profile is from Karnataka
        FarmerProfileDto profile = FarmerProfileDto.builder()
                .state("Karnataka")
                .gender("Male")
                .age(35)
                .occupation("Farmer")
                .category("General")
                .income(150000)
                .landHolding(3.0)
                .build();

        // Stub DB to return central and Tamil Nadu schemes
        List<Object[]> nearestIds = List.of(
                new Object[]{centralScheme.getId(), 0.8},
                new Object[]{stateSchemeTamilNadu.getId(), 0.75}
        );
        when(schemeRepository.findNearestNeighborIdsWithSimilarity(anyString(), anyInt())).thenReturn(nearestIds);
        when(schemeRepository.findAllById(any())).thenReturn(List.of(centralScheme, stateSchemeTamilNadu));

        List<RecommendationResponseDto> results = recommendationEngine.recommendSchemes(profile);

        // Karnataka farmer should get the Central scheme but NOT the Tamil Nadu scheme
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScheme()).isEqualTo("PM Kisan Income Support");
    }

    @Test
    void testRecommendationFiltersByGender() {
        // Farmer profile is Male
        FarmerProfileDto profile = FarmerProfileDto.builder()
                .state("Tamil Nadu")
                .gender("Male")
                .age(40)
                .occupation("Farmer")
                .category("General")
                .income(180000)
                .landHolding(2.0)
                .build();

        // Stub DB to return central and female-only schemes
        List<Object[]> nearestIds = List.of(
                new Object[]{centralScheme.getId(), 0.85},
                new Object[]{femaleOnlyScheme.getId(), 0.80}
        );
        when(schemeRepository.findNearestNeighborIdsWithSimilarity(anyString(), anyInt())).thenReturn(nearestIds);
        when(schemeRepository.findAllById(any())).thenReturn(List.of(centralScheme, femaleOnlyScheme));

        List<RecommendationResponseDto> results = recommendationEngine.recommendSchemes(profile);

        // Male farmer should get centralScheme but femaleOnlyScheme must be rejected
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScheme()).isEqualTo("PM Kisan Income Support");
    }

    @Test
    void testHybridRecommendationKeywordBoost() {
        // Farmer profile searching for irrigation
        FarmerProfileDto profile = FarmerProfileDto.builder()
                .state("Tamil Nadu")
                .gender("Male")
                .age(42)
                .occupation("Farmer")
                .category("General")
                .income(100000)
                .landHolding(2.0)
                .keywords("irrigation")
                .build();

        List<Object[]> nearestIds = List.of(
                new Object[]{centralScheme.getId(), 0.70},
                new Object[]{stateSchemeTamilNadu.getId(), 0.70}
        );
        when(schemeRepository.findNearestNeighborIdsWithSimilarity(anyString(), anyInt())).thenReturn(nearestIds);
        when(schemeRepository.findAllById(any())).thenReturn(List.of(centralScheme, stateSchemeTamilNadu));

        List<RecommendationResponseDto> results = recommendationEngine.recommendSchemes(profile);

        // Both schemes have same semantic score (0.70), but stateSchemeTamilNadu matches the keyword "irrigation"
        // in its title/description, giving it a rule boost. Hence it should rank higher!
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getScheme()).isEqualTo("Tamil Nadu Drip Irrigation Subsidy");
        assertThat(results.get(0).getScore()).isGreaterThan(results.get(1).getScore());
    }
}
