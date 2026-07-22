package com.tcc.guideevents.domain.model;

import java.util.Locale;

public enum GuideEventStatus {
    CREADA,
    RECOLECTADA,
    EN_TRANSITO,
    EN_REPARTO,
    ENTREGADA,
    NOVEDAD,
    DEVUELTA,
    CANCELADA;

    public static GuideEventStatus fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("El estado del evento no puede ser vacio");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return GuideEventStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de guia no soportado: " + raw);
        }
    }
}
