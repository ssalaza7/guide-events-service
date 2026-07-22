package com.tcc.guideevents.infrastructure.controllers;

import com.tcc.guideevents.application.ProcessGuideEventUseCase;
import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.model.GuideEventStatus;
import com.tcc.guideevents.domain.exception.InvalidGuideEventException;
import com.tcc.guideevents.domain.model.ProcessResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/guide-events")
public class GuideEventController {

    private final ProcessGuideEventUseCase processGuideEventUseCase;
    private final Clock clock;

    public GuideEventController(ProcessGuideEventUseCase processGuideEventUseCase, Clock clock) {
        this.processGuideEventUseCase = processGuideEventUseCase;
        this.clock = clock;
    }

    @PostMapping
    public Mono<ResponseEntity<GuideEventResponse>> process(@Valid @RequestBody Mono<GuideEventRequest> requestMono) {
        return requestMono
                .map(this::toDomain)
                .flatMap(event -> processGuideEventUseCase.process(event)
                        .map(result -> {
                            HttpStatus status = result == ProcessResult.ACCEPTED ? HttpStatus.ACCEPTED : HttpStatus.OK;
                            return ResponseEntity.status(status).body(toResponse(event, result));
                        }));
    }

    private GuideEvent toDomain(GuideEventRequest request) {
        GuideEventStatus status;
        try {
            status = GuideEventStatus.fromValue(request.status());
        } catch (IllegalArgumentException ex) {
            throw new InvalidGuideEventException(ex.getMessage());
        }
        return GuideEvent.of(
                request.eventId(),
                request.guideId(),
                status,
                request.occurredAt(),
                request.location(),
                request.description(),
                request.metadata(),
                clock.instant());
    }

    private GuideEventResponse toResponse(GuideEvent event, ProcessResult result) {
        return new GuideEventResponse(event.eventId(), event.guideId(), event.status().name(), result.name(), event.receivedAt());
    }
}
