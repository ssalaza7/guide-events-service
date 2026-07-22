package com.tcc.guideevents.domain.model;

import com.tcc.guideevents.domain.exception.InvalidGuideEventException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuideEventTest {

    private final Instant now = Instant.parse("2026-07-21T10:00:00Z");

    @Test
    void createsValidEventAndDefaultsMetadataToEmptyMap() {
        GuideEvent event = GuideEvent.of("evt-1", "GUI123", GuideEventStatus.EN_TRANSITO, now, "Bogota", "En camino", null, now);

        assertThat(event.eventId()).isEqualTo("evt-1");
        assertThat(event.metadata()).isEmpty();
    }

    @Test
    void generatesEventIdWhenBlank() {
        GuideEvent event = GuideEvent.of("  ", "GUI123", GuideEventStatus.CREADA, now, null, null, null, now);

        assertThat(event.eventId()).isNotBlank();
    }

    @Test
    void copiesMetadataDefensively() {
        Map<String, String> metadata = new java.util.HashMap<>(Map.of("carrier", "TCC"));
        GuideEvent event = GuideEvent.of("evt-2", "GUI123", GuideEventStatus.CREADA, now, null, null, metadata, now);

        metadata.put("extra", "value");

        assertThat(event.metadata()).containsExactly(Map.entry("carrier", "TCC"));
    }

    @Test
    void rejectsBlankGuideId() {
        assertThatThrownBy(() -> GuideEvent.of("evt-3", "  ", GuideEventStatus.CREADA, now, null, null, null, now))
                .isInstanceOf(InvalidGuideEventException.class);
    }

    @Test
    void rejectsNullStatus() {
        assertThatThrownBy(() -> GuideEvent.of("evt-4", "GUI123", null, now, null, null, null, now))
                .isInstanceOf(InvalidGuideEventException.class);
    }

    @Test
    void rejectsNullOccurredAt() {
        assertThatThrownBy(() -> GuideEvent.of("evt-5", "GUI123", GuideEventStatus.CREADA, null, null, null, null, now))
                .isInstanceOf(InvalidGuideEventException.class);
    }

    @Test
    void rejectsNullReceivedAt() {
        assertThatThrownBy(() -> GuideEvent.of("evt-7", "GUI123", GuideEventStatus.CREADA, now, null, null, null, null))
                .isInstanceOf(InvalidGuideEventException.class);
    }

    @Test
    void rejectsOccurredAtTooFarInTheFuture() {
        Instant tooFarFuture = now.plus(10, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> GuideEvent.of("evt-6", "GUI123", GuideEventStatus.CREADA, tooFarFuture, null, null, null, now))
                .isInstanceOf(InvalidGuideEventException.class);
    }
}
