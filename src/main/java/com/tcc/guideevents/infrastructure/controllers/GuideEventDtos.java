package com.tcc.guideevents.infrastructure.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

record GuideEventRequest(
        @Size(max = 100) String eventId,
        @NotBlank(message = "guideId es obligatorio") @Size(max = 50) String guideId,
        @NotBlank(message = "status es obligatorio") String status,
        @NotNull(message = "occurredAt es obligatorio") Instant occurredAt,
        @Size(max = 200) String location,
        @Size(max = 500) String description,
        Map<String, String> metadata) {
}

record GuideEventResponse(String eventId, String guideId, String status, String result, Instant receivedAt) {
}
