package com.freightsource.ragassistant.agent;

import org.springframework.ai.tool.ToolCallback;

record McpToolInfo(String name, String description, String inputSchema) {

    static McpToolInfo from(ToolCallback toolCallback) {
        var definition = toolCallback.getToolDefinition();
        return new McpToolInfo(definition.name(), definition.description(), definition.inputSchema());
    }
}
