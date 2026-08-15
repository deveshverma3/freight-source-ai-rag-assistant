package com.freightsource.ragassistant.rag;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ask")
@Tag(name = "3. Ask (RAG)", description = "Retrieval-augmented Q&A -- retrieves relevant chunks from pgvector and asks Claude with that context injected. Declines gracefully when nothing relevant was ingested.")
class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final ChatClient chatClient;
    private final VectorStoreDocumentRetriever documentRetriever;

    RagController(ChatClient.Builder builder, Advisor ragAdvisor, Advisor simpleLoggerAdvisor, VectorStoreDocumentRetriever documentRetriever) {
        this.chatClient = builder.defaultAdvisors(ragAdvisor, simpleLoggerAdvisor).build();
        this.documentRetriever = documentRetriever;
    }

    @GetMapping
    @Operation(
            summary = "Ask a grounded question",
            description = """
                    Embeds your question, runs similarity search against pgvector (top 4 chunks, similarity >= 0.5), \
                    and injects the matches into the prompt before calling Claude. If nothing relevant was ingested \
                    (see /ingest), Claude is instructed to say so rather than answer from its own training data."""
    )
    String ask(@Parameter(description = "A question whose answer should live in the ingested documents", example = "What is the contracted rate for Carrier X on the NY-CHI lane?")
               @RequestParam String q) {
        log.info("Received /ask request");
        String response = chatClient.prompt().user(q).call().content();
        log.info("Completed /ask request");
        return response;
    }

    @GetMapping("/preview")
    @Operation(
            summary = "Preview the chunks /ask would retrieve, without calling Claude",
            description = """
                    Runs the exact same similarity search /ask uses against pgvector -- same top-K, same \
                    similarity threshold -- and returns the matching chunks directly, with their scores and \
                    metadata. Does not call Claude, so this works without a valid Anthropic API key. Useful for \
                    verifying ingestion and retrieval quality independently of the model's answer."""
    )
    List<RetrievedChunk> preview(@Parameter(description = "The question to run retrieval for", example = "What is the contracted rate for Carrier X on the NY-CHI lane?")
                                  @RequestParam String q) {
        log.info("Received /ask/preview request");
        List<RetrievedChunk> chunks = documentRetriever.retrieve(new Query(q)).stream()
                .map(RetrievedChunk::from)
                .toList();
        log.info("Completed /ask/preview request: {} chunk(s)", chunks.size());
        return chunks;
    }
}
