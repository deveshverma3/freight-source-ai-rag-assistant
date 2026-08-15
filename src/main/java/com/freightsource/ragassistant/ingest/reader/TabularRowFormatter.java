package com.freightsource.ragassistant.ingest.reader;

import java.util.Map;

/** Shared by CsvFormatReader and XlsxFormatReader so both render a row the same way. */
final class TabularRowFormatter {

    private TabularRowFormatter() {
    }

    static String toText(Map<String, String> row) {
        StringBuilder sb = new StringBuilder();
        row.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" | ");
                }
                sb.append(key).append(": ").append(value);
            }
        });
        return sb.toString();
    }
}
