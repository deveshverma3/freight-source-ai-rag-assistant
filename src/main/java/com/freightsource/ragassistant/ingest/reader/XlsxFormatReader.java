package com.freightsource.ragassistant.ingest.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/** One row = one chunk, across every sheet in the workbook. See CsvFormatReader for why. */
@Component
class XlsxFormatReader implements DocumentFormatReader {

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".xlsx");
    }

    @Override
    public List<Document> read(Path file) throws IOException {
        List<Document> docs = new ArrayList<>();
        var formatter = new DataFormatter();
        try (InputStream in = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(in)) {
            for (Sheet sheet : workbook) {
                Row headerRow = sheet.getRow(sheet.getFirstRowNum());
                if (headerRow == null) {
                    continue;
                }
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(formatter.formatCellValue(cell));
                }

                for (Row row : sheet) {
                    if (row.getRowNum() == headerRow.getRowNum()) {
                        continue;
                    }
                    Map<String, String> values = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = row.getCell(i);
                        values.put(headers.get(i), cell == null ? "" : formatter.formatCellValue(cell));
                    }
                    docs.add(new Document(TabularRowFormatter.toText(values), Map.of(
                            "source", file.toString(),
                            "sheet", sheet.getSheetName(),
                            "row", String.valueOf(row.getRowNum() + 1))));
                }
            }
        }
        return docs;
    }
}
