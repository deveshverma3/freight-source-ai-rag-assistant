package com.freightsource.ragassistant.ingest.reader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxFormatReaderTest {

    private final XlsxFormatReader reader = new XlsxFormatReader();

    @TempDir
    Path tempDir;

    @Test
    void supportsOnlyXlsxFiles() {
        assertThat(reader.supports(Path.of("rates.xlsx"))).isTrue();
        assertThat(reader.supports(Path.of("rates.csv"))).isFalse();
        assertThat(reader.supports(Path.of("rates.pdf"))).isFalse();
    }

    @Test
    void readsOneDocumentPerRowAcrossAllSheets() throws IOException {
        Path xlsx = tempDir.resolve("rates.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeSheet(workbook, "Domestic", new String[]{"Carrier", "Rate"}, new String[][]{
                    {"Acme Freight", "450"},
            });
            writeSheet(workbook, "International", new String[]{"Carrier", "Rate"}, new String[][]{
                    {"Beta Logistics", "1200"},
                    {"Gamma Shipping", "980"},
            });
            try (OutputStream out = Files.newOutputStream(xlsx)) {
                workbook.write(out);
            }
        }

        List<Document> docs = reader.read(xlsx);

        assertThat(docs).hasSize(3);
        assertThat(docs).extracting(Document::getText).containsExactlyInAnyOrder(
                "Carrier: Acme Freight | Rate: 450",
                "Carrier: Beta Logistics | Rate: 1200",
                "Carrier: Gamma Shipping | Rate: 980");
        assertThat(docs).allSatisfy(doc -> assertThat(doc.getMetadata()).containsKeys("source", "sheet", "row"));
    }

    @Test
    void emptySheetProducesNoDocuments() throws IOException {
        Path xlsx = tempDir.resolve("empty.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Empty");
            try (OutputStream out = Files.newOutputStream(xlsx)) {
                workbook.write(out);
            }
        }

        assertThat(reader.read(xlsx)).isEmpty();
    }

    private void writeSheet(XSSFWorkbook workbook, String sheetName, String[] headers, String[][] rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows[r].length; c++) {
                row.createCell(c).setCellValue(rows[r][c]);
            }
        }
    }
}
