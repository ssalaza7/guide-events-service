package com.tcc.guideevents.infrastructure.controllers;

import com.tcc.guideevents.domain.exception.EventPublishingException;
import com.tcc.guideevents.domain.exception.InvalidGuideEventException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(Clock.fixed(Instant.parse("2026-07-21T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void handlesValidationErrorsAsBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "guideId", "guideId es obligatorio"));
        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0);

        ResponseEntity<?> response = handler.handleValidation(new WebExchangeBindException(methodParameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesInvalidGuideEventAsBadRequest() {
        ResponseEntity<?> response = handler.handleInvalidGuideEvent(new InvalidGuideEventException("guideId invalido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesPublishingFailureAsServiceUnavailable() {
        ResponseEntity<?> response = handler.handlePublishingFailure(new EventPublishingException("fallo", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handlesUnexpectedErrorsAsInternalServerError() {
        ResponseEntity<?> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String arg) {
    }
}
