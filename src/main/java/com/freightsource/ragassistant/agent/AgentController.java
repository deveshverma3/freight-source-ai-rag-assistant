package com.freightsource.ragassistant.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
@Tag(name = "4. Agent (MCP)", description = "Tool-calling via MCP -- Claude can call a filesystem MCP server to read a real file on disk, live, even if it was never embedded into pgvector, and a read-only Postgres MCP server to run direct SQL against the database.")
class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpToolCallbackProvider;
    private final ObjectMapper objectMapper;

    AgentController(ChatClient.Builder builder, ToolCallbackProvider mcpToolCallbackProvider, Advisor simpleLoggerAdvisor, ObjectMapper objectMapper) {
        this.chatClient = builder
                .defaultToolCallbacks(mcpToolCallbackProvider)
                .defaultAdvisors(simpleLoggerAdvisor)
                .build();
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(
            summary = "Ask with live tool access via MCP (filesystem + Postgres)",
            description = """
                    Wires every connected MCP server's tools onto the ChatClient -- currently a filesystem server \
                    and a read-only Postgres server -- so Claude can decide, on its own, to read a file directly \
                    from disk or run a SQL query directly against the database, rather than relying only on \
                    ingested/embedded content. Check the application logs to see the actual tool invocation."""
    )
    String ask(@Parameter(description = "A question that likely requires reading a real file or querying the database directly, not just embedded chunks", example = "What's in application.yml right now?")
               @RequestParam String q) {
        log.info("Received /agent request");
        String response = chatClient.prompt().user(q).call().content();
        log.info("Completed /agent request");
        return response;
    }

    @GetMapping("/tools")
    @Operation(
            summary = "List the MCP tools available to Claude, without calling Claude",
            description = """
                    Lists every tool every connected MCP server exposes -- filesystem and Postgres both -- \
                    name, description, and JSON input schema, as discovered at application startup. Does not \
                    call Claude, so this works without a valid Anthropic API key. Use POST /agent/tools/{name} \
                    to invoke one directly."""
    )
    List<McpToolInfo> tools() {
        return Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .map(McpToolInfo::from)
                .toList();
    }

    @PostMapping("/tools/{name}")
    @Operation(
            summary = "Invoke an MCP tool directly, without calling Claude",
            description = """
                    Calls the named MCP tool -- from either connected server -- with the given JSON arguments \
                    (matching its inputSchema from GET /agent/tools) and returns its raw result. Bypasses \
                    Claude's reasoning entirely -- proves the MCP servers and their underlying operations \
                    actually work, independently of any Anthropic API key. The same tools (including \
                    filesystem tools that write or move files) are already reachable indirectly via GET /agent \
                    if Claude chooses to call them; this just calls them directly. Filesystem tool paths are \
                    resolved relative to MCP_FILESYSTEM_ROOT (the app's working directory by default) -- e.g. \
                    src/main/resources/application.yml, not just application.yml. Postgres tools take SQL/schema \
                    arguments instead and only run in read-only mode (--access-mode=restricted)."""
    )
    String invokeTool(@Parameter(description = "Tool name, from GET /agent/tools", example = "read_text_file")
                       @PathVariable String name,
                       @Parameter(description = "JSON object of arguments matching the tool's inputSchema", example = "{\"path\": \"src/main/resources/application.yml\"}")
                       @RequestBody Map<String, Object> arguments) throws JsonProcessingException {
        ToolCallback tool = Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .filter(callback -> callback.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MCP tool: " + name));
        String argumentsJson = objectMapper.writeValueAsString(arguments);
        log.info("Invoking MCP tool directly: {}", name);
        try {
            return tool.call(argumentsJson);
        } catch (RuntimeException ex) {
            // The MCP tool most commonly fails here because of caller input --
            // a bad path, a file outside the allowed directory, malformed
            // arguments -- so surface the real reason as a 400 instead of a
            // generic 500 (see GlobalExceptionHandler).
            throw new IllegalArgumentException(
                    "MCP tool '" + name + "' failed for the given arguments: " + ex.getMessage(), ex);
        }
    }
}
