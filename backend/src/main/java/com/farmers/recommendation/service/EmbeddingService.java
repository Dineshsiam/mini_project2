package com.farmers.recommendation.service;

import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService() {
        // Initializing the in-process, CPU-based ONNX embedding model.
        // It runs in the Java application without needing any external server or Python wrapper.
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Generates a 384-dimensional dense float vector for the input text using all-MiniLM-L6-v2.
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[384];
        }
        
        // Clean text (remove multiple newlines/spaces)
        String cleanedText = text.replaceAll("\\s+", " ").trim();
        
        try {
            return embeddingModel.embed(cleanedText).content().vector();
        } catch (Exception e) {
            throw new RuntimeException("Error generating text embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a float array vector to pgvector string format (e.g. "[0.12, -0.45, 0.88, ...]")
     */
    public String getVectorString(float[] vector) {
        if (vector == null) {
            return "[]";
        }
        return Arrays.toString(vector);
    }
}
