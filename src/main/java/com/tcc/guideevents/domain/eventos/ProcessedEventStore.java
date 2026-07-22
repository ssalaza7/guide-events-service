package com.tcc.guideevents.domain.eventos;

import reactor.core.publisher.Mono;

public interface ProcessedEventStore {

    // reserva atomica (SET NX EX): evita la carrera de dos peticiones con el mismo eventId
    Mono<Boolean> reserve(String eventId);

    Mono<Void> release(String eventId);
}
