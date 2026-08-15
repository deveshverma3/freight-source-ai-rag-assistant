package com.freightsource.ragassistant.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@Tag(name = "1. Chat (plain)", description = "Direct Claude call with no retrieval or tools -- proves the Anthropic API key and Spring AI wiring work.")
class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    ChatController(ChatClient.Builder builder, Advisor simpleLoggerAdvisor) {
        this.chatClient = builder.defaultAdvisors(simpleLoggerAdvisor).build();
    }

    @GetMapping
    @Operation(
            summary = "Ask Claude directly",
            description = "No RAG, no tools -- just a straight call to Claude. Use this as a sanity check before touching /ask or /agent."
    )
    String chat(@Parameter(description = "Your question, in plain English", example = "Hello Claude, are you working?")
                @RequestParam String q) {
        log.info("Received /chat request");
        String response = chatClient.prompt().user(q).call().content();
        log.info("Completed /chat request");
        return response;
    }
}
