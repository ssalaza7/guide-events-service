package com.tcc.guideevents.domain.model;

import com.tcc.guideevents.domain.exception.InvalidGuideEventException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public record GuideEvent(
        String eventId,
        String guideId,
        GuideEventStatus status,
        Instant occurredAt,
        String location,
        String description,
        Map<String, String> metadata,
        Instant receivedAt) {

    private static final long MAX_FUTURE_SKEW_MINUTES = 5;

    public GuideEvent {
        if (guideId == null || guideId.isBlank()) {
            throw new InvalidGuideEventException("guideId es obligatorio");
        }
        if (status == null) {
            throw new InvalidGuideEventException("status es obligatorio");
        }
        if (occurredAt == null) {
            throw new InvalidGuideEventException("occurredAt es obligatorio");
        }
        if (receivedAt == null) {
            throw new InvalidGuideEventException("receivedAt es obligatorio");
        }
        if (occurredAt.isAfter(receivedAt.plus(MAX_FUTURE_SKEW_MINUTES, ChronoUnit.MINUTES))) {
            throw new InvalidGuideEventException("occurredAt no puede estar en el futuro");
        }
        eventId = (eventId == null || eventId.isBlank()) ? UUID.randomUUID().toString() : eventId.trim();
        guideId = guideId.trim();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static GuideEvent of(
            String eventId,
            String guideId,
            GuideEventStatus status,
            Instant occurredAt,
            String location,
            String description,
            Map<String, String> metadata,
            Instant receivedAt) {
        return new GuideEvent(eventId, guideId, status, occurredAt, location, description, metadata, receivedAt);
    }
}
