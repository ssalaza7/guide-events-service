package com.tcc.guideevents.infrastructure.adapters;

import java.time.Instant;
import java.util.Map;

// contrato serializado que viaja por RabbitMQ, versionado para consumidores independientes
record GuideEventMessage(
        int schemaVersion,
        String eventId,
        String guideId,
        String status,
        Instant occurredAt,
        String location,
        String description,
        Map<String, String> metadata,
        Instant publishedAt) {

    static final int CURRENT_SCHEMA_VERSION = 1;
}
