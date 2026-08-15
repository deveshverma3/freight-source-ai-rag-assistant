package com.freightsource.ragassistant.config;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared AI infrastructure beans. Kept out of the controllers so /ask, /agent,
 * and /rag-agent each configure their ChatClient from the same source of
 * truth instead of re-building an equivalent Advisor/ToolCallbackProvider.
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
class AiCapabilitiesConfig {

    @Bean
    VectorStoreDocumentRetriever documentRetriever(VectorStore vectorStore, RagProperties ragProperties) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(ragProperties.similarityThreshold())
                .topK(ragProperties.topK())
                .build();
    }

    @Bean
    Advisor ragAdvisor(VectorStoreDocumentRetriever documentRetriever) {
        // RetrievalAugmentationAdvisor's default ContextualQueryAugmenter has
        // allowEmptyContext=false, so when retrieval finds nothing relevant it
        // tells the model to say so instead of answering from its own training data.
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }

    @Bean
    ToolCallbackProvider mcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
        return new SyncMcpToolCallbackProvider(mcpClients);
    }

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder().build();
    }

    /** Logs every request/response through a ChatClient. Level controlled by
     * logging.level.org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor. */
    @Bean
    Advisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
}
