package com.farmers.recommendation;

import com.farmers.recommendation.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    private final EmbeddingService embeddingService = new EmbeddingService();

    @Test
    void testEmbeddingDimensions() {
        String testText = "Farmer from Tamil Nadu needing drip irrigation support";
        float[] embedding = embeddingService.getEmbedding(testText);
        
        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(384);
        
        // Print vector details for logs
        System.out.println("Generated embedding successfully! Dimensions: " + embedding.length);
        System.out.println("First 5 values: " + embedding[0] + ", " + embedding[1] + ", " + embedding[2] + ", " + embedding[3] + ", " + embedding[4]);
    }
}
