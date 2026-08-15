package com.freightsource.ragassistant.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.freightsource.ragassistant.config.SecurityConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises ChatController through the real security filter chain (imported
 * explicitly, since @WebMvcTest doesn't pick up arbitrary @Configuration
 * classes) -- proves both the controller wiring and that HTTP Basic auth is
 * actually enforced, using the app's default local-dev credentials from
 * application.yml (admin / changeme).
 *
 * ChatController resolves builder.defaultAdvisors(...).build() eagerly in
 * its constructor, which Spring invokes during context creation -- before
 * any @Test method (and any post-construction stubbing) runs. So the mock
 * chain must be fully built and stubbed here, in the bean factory method
 * itself, not stubbed afterward from inside a test.
 */
@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, ChatControllerTest.MockChatClientConfig.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestsWithNoCredentials() throws Exception {
        mockMvc.perform(get("/chat").param("q", "hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestsWithTheWrongPassword() throws Exception {
        mockMvc.perform(get("/chat").param("q", "hello").with(httpBasic("admin", "not-the-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsClaudesAnswerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/chat").param("q", "hello").with(httpBasic("admin", "changeme")))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from Claude"));
    }

    @TestConfiguration
    static class MockChatClientConfig {

        @Bean
        Advisor simpleLoggerAdvisor() {
            return mock(Advisor.class);
        }

        @Bean
        ChatClient.Builder chatClientBuilder(Advisor simpleLoggerAdvisor) {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            ChatClient chatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

            when(builder.defaultAdvisors(simpleLoggerAdvisor)).thenReturn(builder);
            when(builder.build()).thenReturn(chatClient);
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user("hello")).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Hello from Claude");

            return builder;
        }
    }
}
