package com.tcc.guideevents.infrastructure.controllers;

import com.tcc.guideevents.domain.exception.EventPublishingException;
import com.tcc.guideevents.domain.exception.InvalidGuideEventException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidation(WebExchangeBindException ex) {
        List<String> messages = ex.getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, messages);
    }

    @ExceptionHandler(InvalidGuideEventException.class)
    public ResponseEntity<ApiError> handleInvalidGuideEvent(InvalidGuideEventException ex) {
        return build(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
    }

    @ExceptionHandler(EventPublishingException.class)
    public ResponseEntity<ApiError> handlePublishingFailure(EventPublishingException ex) {
        log.error("Fallo publicando evento hacia el broker", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, List.of("No fue posible publicar el evento. Reintente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, List.of("Error interno inesperado"));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, List<String> messages) {
        return ResponseEntity.status(status).body(new ApiError(clock.instant(), status.value(), messages));
    }
}

record ApiError(Instant timestamp, int status, List<String> messages) {
}
