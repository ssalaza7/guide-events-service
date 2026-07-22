package com.tcc.guideevents.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuideEventStatusTest {

    @ParameterizedTest
    @ValueSource(strings = {"en_transito", "EN_TRANSITO", " en-transito "})
    void parsesTolerantly(String raw) {
        assertThat(GuideEventStatus.fromValue(raw)).isEqualTo(GuideEventStatus.EN_TRANSITO);
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> GuideEventStatus.fromValue("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownStatus() {
        assertThatThrownBy(() -> GuideEventStatus.fromValue("NO_EXISTE")).isInstanceOf(IllegalArgumentException.class);
    }
}
