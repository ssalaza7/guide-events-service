package com.tcc.guideevents.infrastructure.adapters;

import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.model.GuideEventStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;

class GuideValidadorAdapterTest {

    @Test
    void alwaysReturnsTrue() {
        GuideValidadorAdapter validator = new GuideValidadorAdapter();
        Instant now = Instant.parse("2026-07-21T10:00:00Z");
        GuideEvent event = GuideEvent.of("evt-1", "GUI123", GuideEventStatus.CREADA, now, null, null, null, now);

        StepVerifier.create(validator.isValid(event)).expectNext(true).verifyComplete();
    }
}
