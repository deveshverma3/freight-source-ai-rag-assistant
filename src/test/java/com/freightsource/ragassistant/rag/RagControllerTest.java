package com.freightsource.ragassistant.rag;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.freightsource.ragassistant.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As with ChatControllerTest, RagController resolves its ChatClient eagerly
 * in the constructor, so the mock chain is pre-built in a @TestConfiguration
 * bean rather than stubbed from inside a @Test method.
 */
@WebMvcTest(RagController.class)
@Import({SecurityConfig.class, RagControllerTest.MockRagConfig.class})
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsPreviewRequestsWithNoCredentials() throws Exception {
        mockMvc.perform(get("/ask/preview").param("q", "hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void previewReturnsRetrievedChunksWithoutCallingClaude() throws Exception {
        mockMvc.perform(get("/ask/preview").param("q", "what is the rate").with(httpBasic("admin", "changeme")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("chunk-1"))
                .andExpect(jsonPath("$[0].score").value(0.87))
                .andExpect(jsonPath("$[0].text").value("Detention is billed at $65 per hour."))
                .andExpect(jsonPath("$[0].metadata.source").value("policy.md"));
    }

    @TestConfiguration
    static class MockRagConfig {

        @Bean
        Advisor ragAdvisor() {
            return mock(Advisor.class);
        }

        @Bean
        Advisor simpleLoggerAdvisor() {
            return mock(Advisor.class);
        }

        @Bean
        ChatClient.Builder chatClientBuilder(Advisor ragAdvisor, Advisor simpleLoggerAdvisor) {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.defaultAdvisors(ragAdvisor, simpleLoggerAdvisor)).thenReturn(builder);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }

        @Bean
        VectorStoreDocumentRetriever documentRetriever() {
            VectorStoreDocumentRetriever retriever = mock(VectorStoreDocumentRetriever.class);
            Document chunk = Document.builder()
                    .id("chunk-1")
                    .text("Detention is billed at $65 per hour.")
                    .metadata(Map.of("source", "policy.md"))
                    .score(0.87)
                    .build();
            when(retriever.retrieve(any(Query.class))).thenReturn(List.of(chunk));
            return retriever;
        }
    }
}
