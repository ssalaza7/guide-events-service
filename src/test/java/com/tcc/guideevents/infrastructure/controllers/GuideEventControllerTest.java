package com.tcc.guideevents.infrastructure.controllers;

import com.tcc.guideevents.application.ProcessGuideEventUseCase;
import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideEventControllerTest {

    @Mock
    private ProcessGuideEventUseCase processGuideEventUseCase;

    private GuideEventController controller;
    private final Instant fixedNow = Instant.parse("2026-07-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        controller = new GuideEventController(processGuideEventUseCase, Clock.fixed(fixedNow, ZoneOffset.UTC));
    }

    @Test
    void returns202WhenAccepted() {
        when(processGuideEventUseCase.process(any(GuideEvent.class))).thenReturn(Mono.just(ProcessResult.ACCEPTED));
        GuideEventRequest request = new GuideEventRequest(
                "evt-1", "GUI123", "EN_TRANSITO", fixedNow, "Bogota", "En camino", null);

        StepVerifier.create(controller.process(Mono.just(request)))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().eventId()).isEqualTo("evt-1");
                })
                .verifyComplete();
    }

    @Test
    void returns200WhenDuplicate() {
        when(processGuideEventUseCase.process(any(GuideEvent.class))).thenReturn(Mono.just(ProcessResult.DUPLICATE_IGNORED));
        GuideEventRequest request = new GuideEventRequest(
                null, "GUI123", "CREADA", fixedNow, null, null, null);

        StepVerifier.create(controller.process(Mono.just(request)))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
    }
}
