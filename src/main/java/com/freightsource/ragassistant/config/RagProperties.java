package com.freightsource.ragassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(double similarityThreshold, int topK) {

    public RagProperties {
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("app.rag.similarity-threshold must be between 0.0 and 1.0");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("app.rag.top-k must be positive");
        }
    }
}
