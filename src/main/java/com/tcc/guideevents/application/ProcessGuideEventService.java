package com.tcc.guideevents.application;

import com.tcc.guideevents.domain.eventos.EventPublisher;
import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.eventos.GuideValidador;
import com.tcc.guideevents.domain.exception.InvalidGuideEventException;
import com.tcc.guideevents.domain.eventos.ProcessedEventStore;
import com.tcc.guideevents.domain.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ProcessGuideEventService implements ProcessGuideEventUseCase {

    private final EventPublisher eventPublisher;
    private final ProcessedEventStore processedEventStore;
    private final GuideValidador guideValidator;

    public ProcessGuideEventService(
            EventPublisher eventPublisher, ProcessedEventStore processedEventStore, GuideValidador guideValidator) {
        this.eventPublisher = eventPublisher;
        this.processedEventStore = processedEventStore;
        this.guideValidator = guideValidator;
    }

    @Override
    public Mono<ProcessResult> process(GuideEvent event) {
        return guideValidator.isValid(event)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.<ProcessResult>error(new InvalidGuideEventException(
                                "La guia " + event.guideId() + " no pudo ser validada (posible fraude o guia inexistente)"));
                    }
                    return reserveAndPublish(event);
                });
    }

    private Mono<ProcessResult> reserveAndPublish(GuideEvent event) {
        // reserve() es atomico: evita que dos peticiones con el mismo eventId publiquen doble.
        return processedEventStore.reserve(event.eventId())
                .flatMap(reserved -> {
                    if (!reserved) {
                        log.info("Evento duplicado ignorado. eventId={} guideId={}", event.eventId(), event.guideId());
                        return Mono.just(ProcessResult.DUPLICATE_IGNORED);
                    }
                    return eventPublisher.publish(event)
                            .thenReturn(ProcessResult.ACCEPTED)
                            .doOnSuccess(result -> log.info("Evento publicado. eventId={} guideId={} status={}",
                                    event.eventId(), event.guideId(), event.status()))
                            // si falla el publish, se libera la reserva para no bloquear un reintento valido
                            .onErrorResume(ex -> processedEventStore.release(event.eventId()).then(Mono.error(ex)));
                });
    }
}
