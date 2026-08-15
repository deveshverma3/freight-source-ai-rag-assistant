package com.freightsource.ragassistant.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import com.freightsource.ragassistant.ingest.reader.DocumentFormatReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestorTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentFormatReader mdReader;

    @Mock
    private DocumentFormatReader csvReader;

    @TempDir
    Path tempDir;

    @Test
    void ingestsASingleFileWithTheReaderThatSupportsIt() throws IOException {
        Path file = tempDir.resolve("notes.md");
        Files.writeString(file, "content");
        List<Document> parsed = List.of(new Document("chunk 1"), new Document("chunk 2"));

        lenient().when(mdReader.supports(file)).thenReturn(true);
        when(mdReader.read(file)).thenReturn(parsed);
        lenient().when(csvReader.supports(file)).thenReturn(false);

        var ingestor = new DocumentIngestor(vectorStore, List.of(csvReader, mdReader));
        int chunks = ingestor.ingest(file);

        assertThat(chunks).isEqualTo(2);
        verify(vectorStore).add(parsed);
    }

    @Test
    void throwsForAnUnsupportedSingleFile() throws IOException {
        Path file = tempDir.resolve("notes.xyz");
        Files.writeString(file, "content");

        lenient().when(mdReader.supports(file)).thenReturn(false);
        lenient().when(csvReader.supports(file)).thenReturn(false);

        var ingestor = new DocumentIngestor(vectorStore, List.of(mdReader, csvReader));

        assertThatThrownBy(() -> ingestor.ingest(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void walksAFolderRoutingEachFileToTheReaderThatSupportsItAndSkippingTheRest() throws IOException {
        Path mdFile = tempDir.resolve("policy.md");
        Path csvFile = tempDir.resolve("rates.csv");
        Path unsupportedFile = tempDir.resolve("archive.zip");
        Files.writeString(mdFile, "policy text");
        Files.writeString(csvFile, "csv text");
        Files.writeString(unsupportedFile, "binary junk");

        // findReader() short-circuits on the first match, so with mdReader
        // listed first, csvReader.supports(mdFile) is never actually invoked.
        when(mdReader.supports(mdFile)).thenReturn(true);
        when(mdReader.supports(csvFile)).thenReturn(false);
        when(mdReader.supports(unsupportedFile)).thenReturn(false);
        when(csvReader.supports(csvFile)).thenReturn(true);
        when(csvReader.supports(unsupportedFile)).thenReturn(false);

        List<Document> mdDocs = List.of(new Document("policy chunk"));
        List<Document> csvDocs = List.of(new Document("row 1"), new Document("row 2"));
        when(mdReader.read(mdFile)).thenReturn(mdDocs);
        when(csvReader.read(csvFile)).thenReturn(csvDocs);

        var ingestor = new DocumentIngestor(vectorStore, List.of(mdReader, csvReader));
        int chunks = ingestor.ingest(tempDir);

        assertThat(chunks).isEqualTo(3);
        verify(vectorStore).add(mdDocs);
        verify(vectorStore).add(csvDocs);
        verify(mdReader, never()).read(csvFile);
        verify(csvReader, never()).read(mdFile);
    }

    @Test
    void emptyFolderIngestsNothing() throws IOException {
        var ingestor = new DocumentIngestor(vectorStore, List.of(mdReader, csvReader));

        int chunks = ingestor.ingest(tempDir);

        assertThat(chunks).isZero();
    }
}
