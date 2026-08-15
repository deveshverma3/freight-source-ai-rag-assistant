package com.freightsource.ragassistant.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagPropertiesTest {

    @Test
    void acceptsValidValues() {
        var props = new RagProperties(0.5, 4);

        assertThat(props.similarityThreshold()).isEqualTo(0.5);
        assertThat(props.topK()).isEqualTo(4);
    }

    @Test
    void acceptsBoundaryThresholds() {
        assertThat(new RagProperties(0.0, 1).similarityThreshold()).isZero();
        assertThat(new RagProperties(1.0, 1).similarityThreshold()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, -5.0, 100.0})
    void rejectsThresholdOutsideZeroToOne(double invalidThreshold) {
        assertThatThrownBy(() -> new RagProperties(invalidThreshold, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("similarity-threshold");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void rejectsNonPositiveTopK(int invalidTopK) {
        assertThatThrownBy(() -> new RagProperties(0.5, invalidTopK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("top-k");
    }
}
