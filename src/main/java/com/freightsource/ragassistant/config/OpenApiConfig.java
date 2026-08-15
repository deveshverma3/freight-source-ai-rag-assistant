package com.freightsource.ragassistant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BASIC_AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Every endpoint requires HTTP Basic auth -- see the Security section in the README.")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME))
                .info(new Info()
                .title("FreightSource Knowledge Assistant")
                .version("1.0.0")
                .description("""
                        Ask questions about your own documents in natural language, \
                        answered by Claude and grounded via retrieval-augmented generation.

                        - **/chat** -- plain Claude call, no grounding. Use this first to confirm the API key and wiring work.
                        - **/ingest** -- chunk and embed a file or folder (.md / .txt / .pdf / .docx / .csv / .xlsx) into pgvector, via a local Ollama embedding model.
                        - **/ask** -- RAG: retrieves the closest embedded chunks for a question and asks Claude with that context injected. Declines when nothing relevant was ingested.
                        - **/agent** -- MCP tool-calling: Claude can read a live file from disk via a filesystem MCP server, even if it was never embedded.
                        - **/rag-agent** -- combines /ask and /agent on one ChatClient: grounded retrieval plus live file access in a single query.

                        Typical flow: call /ingest once to embed the documents, then use /ask (or /rag-agent) to query them."""));
    }
}
