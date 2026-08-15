package com.freightsource.ragassistant.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.freightsource.ragassistant.config.SecurityConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As with ChatControllerTest and RagControllerTest, AgentController resolves
 * its ChatClient eagerly in the constructor, so the mock chain is pre-built
 * in a @TestConfiguration bean rather than stubbed from inside a @Test method.
 */
@WebMvcTest(AgentController.class)
@Import({SecurityConfig.class, AgentControllerTest.MockAgentConfig.class})
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsToolListingWithNoCredentials() throws Exception {
        mockMvc.perform(get("/agent/tools"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsAvailableMcpTools() throws Exception {
        mockMvc.perform(get("/agent/tools").with(httpBasic("admin", "changeme")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("read_text_file"))
                .andExpect(jsonPath("$[0].description").value("Read a file from the allowed directory"))
                .andExpect(jsonPath("$[0].inputSchema").value("{\"path\":\"string\"}"));
    }

    @Test
    void invokesTheNamedToolDirectlyWithoutCallingClaude() throws Exception {
        mockMvc.perform(post("/agent/tools/read_text_file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\": \"application.yml\"}")
                        .with(httpBasic("admin", "changeme")))
                .andExpect(status().isOk())
                .andExpect(content().string("file contents here"));
    }

    @Test
    void rejectsAnUnknownToolName() throws Exception {
        mockMvc.perform(post("/agent/tools/does_not_exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(httpBasic("admin", "changeme")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsAFailedToolCallToABadRequestWithTheRealReason() throws Exception {
        mockMvc.perform(post("/agent/tools/read_text_file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\": \"missing.yml\"}")
                        .with(httpBasic("admin", "changeme")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "MCP tool 'read_text_file' failed for the given arguments: file not found"));
    }

    @TestConfiguration
    static class MockAgentConfig {

        @Bean
        Advisor simpleLoggerAdvisor() {
            return mock(Advisor.class);
        }

        @Bean
        ToolCallbackProvider mcpToolCallbackProvider() {
            ToolDefinition definition = mock(ToolDefinition.class);
            when(definition.name()).thenReturn("read_text_file");
            when(definition.description()).thenReturn("Read a file from the allowed directory");
            when(definition.inputSchema()).thenReturn("{\"path\":\"string\"}");

            ToolCallback callback = mock(ToolCallback.class);
            when(callback.getToolDefinition()).thenReturn(definition);
            // The controller re-serializes the parsed request body via Jackson
            // before calling the tool, so these match Jackson's compact output
            // (no space after the colon), not the original request text.
            when(callback.call("{\"path\":\"application.yml\"}")).thenReturn("file contents here");
            when(callback.call("{\"path\":\"missing.yml\"}")).thenThrow(new RuntimeException("file not found"));

            ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
            when(provider.getToolCallbacks()).thenReturn(new ToolCallback[] {callback});
            return provider;
        }

        @Bean
        ChatClient.Builder chatClientBuilder(ToolCallbackProvider mcpToolCallbackProvider, Advisor simpleLoggerAdvisor) {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.defaultToolCallbacks(mcpToolCallbackProvider)).thenReturn(builder);
            when(builder.defaultAdvisors(simpleLoggerAdvisor)).thenReturn(builder);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }
}
