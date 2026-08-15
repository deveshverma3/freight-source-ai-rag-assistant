package com.freightsource.ragassistant.ingest.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * One row = one chunk, not token-split -- splitting a row mid-way loses the
 * column structure that makes it retrievable at all (see XlsxFormatReader).
 */
@Component
class CsvFormatReader implements DocumentFormatReader {

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build();

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".csv");
    }

    @Override
    public List<Document> read(Path file) throws IOException {
        List<Document> docs = new ArrayList<>();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, FORMAT)) {
            for (CSVRecord record : parser) {
                docs.add(new Document(TabularRowFormatter.toText(record.toMap()), Map.of(
                        "source", file.toString(),
                        "row", String.valueOf(record.getRecordNumber()))));
            }
        }
        return docs;
    }
}
