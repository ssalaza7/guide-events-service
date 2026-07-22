package com.tcc.guideevents.application;

import com.tcc.guideevents.domain.eventos.EventPublisher;
import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.model.GuideEventStatus;
import com.tcc.guideevents.domain.eventos.GuideValidador;
import com.tcc.guideevents.domain.exception.InvalidGuideEventException;
import com.tcc.guideevents.domain.eventos.ProcessedEventStore;
import com.tcc.guideevents.domain.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessGuideEventServiceTest {

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ProcessedEventStore processedEventStore;
    @Mock
    private GuideValidador guideValidator;

    private ProcessGuideEventService service;
    private GuideEvent event;

    @BeforeEach
    void setUp() {
        service = new ProcessGuideEventService(eventPublisher, processedEventStore, guideValidator);
        Instant now = Instant.parse("2026-07-21T10:00:00Z");
        event = GuideEvent.of("evt-1", "GUI123", GuideEventStatus.EN_TRANSITO, now, "Bogota", "En camino", null, now);
    }

    @Test
    void publishesAndAcceptsWhenValidAndReservationSucceeds() {
        when(guideValidator.isValid(event)).thenReturn(Mono.just(true));
        when(processedEventStore.reserve("evt-1")).thenReturn(Mono.just(true));
        when(eventPublisher.publish(event)).thenReturn(Mono.empty());

        StepVerifier.create(service.process(event)).expectNext(ProcessResult.ACCEPTED).verifyComplete();
    }

    @Test
    void returnsDuplicateIgnoredWhenReservationFails() {
        when(guideValidator.isValid(event)).thenReturn(Mono.just(true));
        when(processedEventStore.reserve("evt-1")).thenReturn(Mono.just(false));

        StepVerifier.create(service.process(event)).expectNext(ProcessResult.DUPLICATE_IGNORED).verifyComplete();

        verify(eventPublisher, never()).publish(event);
    }

    @Test
    void rejectsWhenGuideIsNotValid() {
        when(guideValidator.isValid(event)).thenReturn(Mono.just(false));

        StepVerifier.create(service.process(event)).expectError(InvalidGuideEventException.class).verify();

        verify(processedEventStore, never()).reserve(event.eventId());
    }

    @Test
    void releasesReservationAndPropagatesErrorWhenPublishFails() {
        RuntimeException failure = new RuntimeException("broker down");
        when(guideValidator.isValid(event)).thenReturn(Mono.just(true));
        when(processedEventStore.reserve("evt-1")).thenReturn(Mono.just(true));
        when(eventPublisher.publish(event)).thenReturn(Mono.error(failure));
        when(processedEventStore.release("evt-1")).thenReturn(Mono.empty());

        StepVerifier.create(service.process(event)).expectErrorMatches(ex -> ex == failure).verify();

        verify(processedEventStore, times(1)).release("evt-1");
    }
}
