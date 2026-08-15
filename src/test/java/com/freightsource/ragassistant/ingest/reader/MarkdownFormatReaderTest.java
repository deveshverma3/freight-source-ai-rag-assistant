package com.freightsource.ragassistant.ingest.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownFormatReaderTest {

    private final MarkdownFormatReader reader = new MarkdownFormatReader(TokenTextSplitter.builder().build());

    @TempDir
    Path tempDir;

    @Test
    void supportsMarkdownAndPlainText() {
        assertThat(reader.supports(Path.of("notes.md"))).isTrue();
        assertThat(reader.supports(Path.of("notes.txt"))).isTrue();
        assertThat(reader.supports(Path.of("notes.pdf"))).isFalse();
        assertThat(reader.supports(Path.of("notes.docx"))).isFalse();
        assertThat(reader.supports(Path.of("notes.csv"))).isFalse();
    }

    @Test
    void readsAndChunksShortFileIntoOneDocument() throws IOException {
        Path file = tempDir.resolve("notes.md");
        Files.writeString(file, "# Freight Policy\n\nAll domestic shipments require signed confirmation.",
                StandardCharsets.UTF_8);

        List<Document> docs = reader.read(file);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getText()).contains("Freight Policy", "signed confirmation");
    }
}
