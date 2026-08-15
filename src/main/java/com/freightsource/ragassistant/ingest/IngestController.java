package com.freightsource.ragassistant.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ingest")
@Tag(name = "2. Ingest", description = "Chunks and embeds your own documents into pgvector via a local Ollama model, so /ask can retrieve them later.")
class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final DocumentIngestor ingestor;

    IngestController(DocumentIngestor ingestor) {
        this.ingestor = ingestor;
    }

    @PostMapping
    @Operation(
            summary = "Embed a file or folder into the vector store",
            description = """
                    Reads a single file or recursively walks a folder that already exists on this machine. Prose \
                    formats (.md, .txt, .pdf, .docx) are split into chunks via TokenTextSplitter; tabular formats \
                    (.csv, .xlsx) are embedded one row per chunk instead, so a rate card or PO log row stays intact \
                    rather than being split mid-row. Everything is written to the pgvector vector_store table. \
                    Prefer POST /ingest/upload if the file lives on the caller's machine rather than the server's."""
    )
    String ingest(@Parameter(description = "Absolute path to a file or folder on this machine", example = "/Users/you/Documents/my-notes")
                  @RequestParam String path) throws IOException {
        int chunks = ingestor.ingest(Path.of(path));
        return "Ingested " + chunks + " chunks from " + path;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Embed an uploaded file into the vector store",
            description = """
                    Same ingestion pipeline as POST /ingest, but takes the file as a direct upload instead of a \
                    server-side path -- use this from Swagger UI's file picker, or any client that doesn't have \
                    filesystem access to the machine running this app. Single files only, up to 20MB \
                    (.md, .txt, .pdf, .docx, .csv, .xlsx)."""
    )
    String ingestUpload(@Parameter(description = "The file to embed") @RequestParam("file") MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Uploaded file has no filename");
        }
        Path tempFile = Files.createTempFile("ingest-upload-", extensionOf(originalFilename));
        try {
            file.transferTo(tempFile);
            int chunks = ingestor.ingest(tempFile);
            log.info("Ingested uploaded file {}: {} chunk(s)", originalFilename, chunks);
            return "Ingested " + chunks + " chunks from " + originalFilename;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
