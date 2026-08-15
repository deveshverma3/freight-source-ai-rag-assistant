package com.freightsource.ragassistant.ingest.reader;

import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/** Plain text formats: .md and .txt, token-chunked like any other prose document. */
@Component
class MarkdownFormatReader implements DocumentFormatReader {

    private final TokenTextSplitter splitter;

    MarkdownFormatReader(TokenTextSplitter splitter) {
        this.splitter = splitter;
    }

    @Override
    public boolean supports(Path file) {
        String name = file.toString();
        return name.endsWith(".md") || name.endsWith(".txt");
    }

    @Override
    public List<Document> read(Path file) {
        var rawDocs = new TextReader(new FileSystemResource(file)).get();
        return splitter.apply(rawDocs);
    }
}
