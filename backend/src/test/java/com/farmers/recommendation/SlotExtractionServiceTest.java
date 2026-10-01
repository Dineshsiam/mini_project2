package com.farmers.recommendation;

import com.farmers.recommendation.dto.ChatFarmerProfile;
import com.farmers.recommendation.service.SlotExtractionService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SlotExtractionServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private SlotExtractionService slotExtractionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExtractSlots_parsesJsonCorrectly() {
        // Mock the LLM to return valid JSON
        String mockJsonResponse = "{\"state\":\"Tamil Nadu\",\"district\":\"Salem\",\"landHolding\":3.0,\"crop\":\"rice\"}";
        when(chatLanguageModel.generate(anyString())).thenReturn(mockJsonResponse);

        ChatFarmerProfile profile = slotExtractionService.extractSlots("I have 3 acres in Salem and grow rice");

        assertThat(profile.getState()).isEqualTo("Tamil Nadu");
        assertThat(profile.getDistrict()).isEqualTo("Salem");
        assertThat(profile.getLandHolding()).isEqualTo(3.0);
        assertThat(profile.getCrop()).isEqualTo("rice");
        assertThat(profile.getAnnualIncome()).isNull();
    }

    @Test
    void testMerge_doesNotOverwriteNonNullValues() {
        ChatFarmerProfile existing = ChatFarmerProfile.builder()
                .state("Tamil Nadu")
                .landHolding(5.0)
                .build();

        ChatFarmerProfile extracted = ChatFarmerProfile.builder()
                .landHolding(3.0) // Should not overwrite existing 5.0
                .crop("wheat")    // Should be added
                .build();

        ChatFarmerProfile merged = slotExtractionService.merge(existing, extracted);

        assertThat(merged.getState()).isEqualTo("Tamil Nadu");
        assertThat(merged.getLandHolding()).isEqualTo(5.0);
        assertThat(merged.getCrop()).isEqualTo("wheat");
    }

    @Test
    void testExtractSlots_handlesInvalidJsonGracefully() {
        when(chatLanguageModel.generate(anyString())).thenReturn("Sorry, I'm an AI and I cannot help with that.");

        ChatFarmerProfile profile = slotExtractionService.extractSlots("Random string");

        assertThat(profile).isNotNull();
        assertThat(profile.getState()).isNull(); // Falls back to empty profile
    }
}
