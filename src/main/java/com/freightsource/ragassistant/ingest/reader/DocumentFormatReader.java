package com.freightsource.ragassistant.ingest.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.document.Document;

/**
 * One implementation per supported file format. DocumentIngestor depends only
 * on this abstraction, so adding a new format means adding a new bean here,
 * not editing the ingestion orchestrator.
 */
public interface DocumentFormatReader {

    boolean supports(Path file);

    List<Document> read(Path file) throws IOException;
}
