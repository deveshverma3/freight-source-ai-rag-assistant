package com.freightsource.ragassistant.ingest.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFormatReaderTest {

    private final CsvFormatReader reader = new CsvFormatReader();

    @TempDir
    Path tempDir;

    @Test
    void supportsOnlyCsvFiles() {
        assertThat(reader.supports(Path.of("rates.csv"))).isTrue();
        assertThat(reader.supports(Path.of("rates.xlsx"))).isFalse();
        assertThat(reader.supports(Path.of("rates.pdf"))).isFalse();
        assertThat(reader.supports(Path.of("rates.md"))).isFalse();
    }

    @Test
    void readsOneDocumentPerRowWithHeaderAsFieldNames() throws IOException {
        Path csv = writeCsv("""
                Carrier,Lane,Rate
                Acme Freight,NY-CHI,450
                Beta Logistics,LA-SEA,610
                """);

        List<Document> docs = reader.read(csv);

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getText()).isEqualTo("Carrier: Acme Freight | Lane: NY-CHI | Rate: 450");
        assertThat(docs.get(1).getText()).isEqualTo("Carrier: Beta Logistics | Lane: LA-SEA | Rate: 610");
    }

    @Test
    void tagsEachDocumentWithSourceAndRowMetadata() throws IOException {
        Path csv = writeCsv("""
                Carrier,Rate
                Acme Freight,450
                """);

        Document doc = reader.read(csv).get(0);

        // Commons CSV's getRecordNumber() is 1-indexed over data records
        // (the header is skipped, not counted) -- the first data row is 1.
        assertThat(doc.getMetadata())
                .containsEntry("source", csv.toString())
                .containsEntry("row", "1");
    }

    @Test
    void emptyCsvProducesNoDocuments() throws IOException {
        Path csv = writeCsv("Carrier,Rate\n");

        assertThat(reader.read(csv)).isEmpty();
    }

    private Path writeCsv(String content) throws IOException {
        Path file = tempDir.resolve("rates.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
