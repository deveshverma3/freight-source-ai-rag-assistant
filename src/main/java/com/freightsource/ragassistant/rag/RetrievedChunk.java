package com.freightsource.ragassistant.rag;

import java.util.Map;

import org.springframework.ai.document.Document;

record RetrievedChunk(String id, Double score, String text, Map<String, Object> metadata) {

    static RetrievedChunk from(Document document) {
        return new RetrievedChunk(document.getId(), document.getScore(), document.getText(), document.getMetadata());
    }
}
