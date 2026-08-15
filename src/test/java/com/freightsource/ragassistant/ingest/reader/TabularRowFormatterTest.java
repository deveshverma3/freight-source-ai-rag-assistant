package com.freightsource.ragassistant.ingest.reader;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TabularRowFormatterTest {

    @Test
    void formatsMultipleColumnsPipeSeparated() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("Carrier", "Acme Freight");
        row.put("Lane", "NY-CHI");
        row.put("Rate", "450");

        assertThat(TabularRowFormatter.toText(row))
                .isEqualTo("Carrier: Acme Freight | Lane: NY-CHI | Rate: 450");
    }

    @Test
    void skipsBlankAndNullValues() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("Carrier", "Acme Freight");
        row.put("Notes", "");
        row.put("Comment", "   ");
        row.put("Contact", null);
        row.put("Rate", "450");

        assertThat(TabularRowFormatter.toText(row))
                .isEqualTo("Carrier: Acme Freight | Rate: 450");
    }

    @Test
    void singleColumnHasNoSeparator() {
        Map<String, String> row = Map.of("Carrier", "Acme Freight");

        assertThat(TabularRowFormatter.toText(row)).isEqualTo("Carrier: Acme Freight");
    }

    @Test
    void emptyRowProducesEmptyString() {
        assertThat(TabularRowFormatter.toText(Map.of())).isEmpty();
    }

    @Test
    void allBlankValuesProduceEmptyString() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("A", "");
        row.put("B", "   ");

        assertThat(TabularRowFormatter.toText(row)).isEmpty();
    }
}
