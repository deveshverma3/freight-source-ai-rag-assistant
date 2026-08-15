package com.freightsource.ragassistant.ingest.reader;

import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
class PdfFormatReader implements DocumentFormatReader {

    private final TokenTextSplitter splitter;

    PdfFormatReader(TokenTextSplitter splitter) {
        this.splitter = splitter;
    }

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".pdf");
    }

    @Override
    public List<Document> read(Path file) {
        var rawDocs = new PagePdfDocumentReader(new FileSystemResource(file)).get();
        return splitter.apply(rawDocs);
    }
}
