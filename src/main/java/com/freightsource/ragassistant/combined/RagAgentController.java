package com.freightsource.ragassistant.combined;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Combines the RAG pipeline (grounded answers from ingested docs) with MCP
 * tool-calling (live file access) on a single ChatClient, so one query can
 * draw on both -- the embedded corpus and whatever the embedded chunks miss.
 */
@RestController
@RequestMapping("/rag-agent")
@Tag(name = "5. RAG + Agent (combined)", description = "Both /ask and /agent on one ChatClient -- Claude can pull grounded context from pgvector AND reach for a live file via MCP in the same query.")
class RagAgentController {

    private static final Logger log = LoggerFactory.getLogger(RagAgentController.class);

    private final ChatClient chatClient;

    RagAgentController(ChatClient.Builder builder, Advisor ragAdvisor, ToolCallbackProvider mcpToolCallbackProvider,
                        Advisor simpleLoggerAdvisor) {
        this.chatClient = builder
                .defaultAdvisors(ragAdvisor, simpleLoggerAdvisor)
                .defaultToolCallbacks(mcpToolCallbackProvider)
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Ask with both grounded retrieval and live file access",
            description = """
                    Attaches the RetrievalAugmentationAdvisor (pgvector) and the MCP filesystem tool callbacks to the \
                    same ChatClient. Claude decides per-query whether to rely on retrieved chunks, call a tool to \
                    read a live file, or both."""
    )
    String ask(@Parameter(description = "Any question -- Claude picks retrieval, live file access, or both", example = "Summarize the carrier agreement and confirm the current pgvector index settings.")
               @RequestParam String q) {
        log.info("Received /rag-agent request");
        String response = chatClient.prompt().user(q).call().content();
        log.info("Completed /rag-agent request");
        return response;
    }
}
