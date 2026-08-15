package com.freightsource.ragassistant.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import com.freightsource.ragassistant.ingest.reader.DocumentFormatReader;

/**
 * Orchestrates ingestion: finds the DocumentFormatReader that supports a given
 * file, delegates parsing/chunking to it, and writes the result to the vector
 * store. Adding a new file format means adding a new DocumentFormatReader
 * bean -- this class never changes.
 */
@Component
public class DocumentIngestor {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestor.class);

    private final VectorStore vectorStore;
    private final List<DocumentFormatReader> formatReaders;

    DocumentIngestor(VectorStore vectorStore, List<DocumentFormatReader> formatReaders) {
        this.vectorStore = vectorStore;
        this.formatReaders = formatReaders;
    }

    /**
     * Ingests a single file or every supported file under a folder (recursively).
     * Returns the number of chunks written to the vector store.
     */
    public int ingest(Path path) throws IOException {
        log.info("Starting ingestion of {}", path);
        int total;
        if (Files.isRegularFile(path)) {
            total = ingestFile(path);
        } else {
            total = 0;
            try (var files = Files.walk(path)) {
                var candidates = files.filter(Files::isRegularFile)
                        .filter(p -> findReader(p).isPresent())
                        .toList();
                log.info("Found {} supported file(s) under {}", candidates.size(), path);
                for (Path p : candidates) {
                    total += ingestFile(p);
                }
            }
        }
        log.info("Finished ingestion of {}: {} chunk(s) written", path, total);
        return total;
    }

    private int ingestFile(Path file) throws IOException {
        DocumentFormatReader reader = findReader(file)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + file));
        var documents = reader.read(file);
        vectorStore.add(documents);
        log.info("Ingested {}: {} chunk(s)", file, documents.size());
        return documents.size();
    }

    private Optional<DocumentFormatReader> findReader(Path file) {
        return formatReaders.stream().filter(r -> r.supports(file)).findFirst();
    }
}
