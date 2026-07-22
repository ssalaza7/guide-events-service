package com.tcc.guideevents.infrastructure.adapters;

import com.tcc.guideevents.domain.exception.EventPublishingException;
import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.model.GuideEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.Sender;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMQAdapterTest {

    @Mock
    private Sender sender;

    private RabbitMQAdapter publisher;
    private GuideEvent event;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMQAdapter(sender, JsonMapper.builder().build());
        Instant now = Instant.parse("2026-07-21T10:00:00Z");
        event = GuideEvent.of("evt-1", "GUI123", GuideEventStatus.EN_TRANSITO, now, "Bogota", "En camino", null, now);
    }

    @Test
    void completesWhenBrokerAcksTheMessage() {
        OutboundMessage sentMessage = new OutboundMessage("ex", "rk", new byte[0]);
        when(sender.sendWithPublishConfirms(any())).thenReturn(Flux.just(new OutboundMessageResult<>(sentMessage, true)));

        StepVerifier.create(publisher.publish(event)).verifyComplete();
    }

    @Test
    void failsWithEventPublishingExceptionWhenBrokerNacks() {
        OutboundMessage sentMessage = new OutboundMessage("ex", "rk", new byte[0]);
        when(sender.sendWithPublishConfirms(any())).thenReturn(Flux.just(new OutboundMessageResult<>(sentMessage, false)));

        StepVerifier.create(publisher.publish(event)).expectError(EventPublishingException.class).verify();
    }

    @Test
    void wrapsUnexpectedErrorsAsEventPublishingException() {
        when(sender.sendWithPublishConfirms(any())).thenReturn(Flux.error(new RuntimeException("conexion perdida")));

        StepVerifier.create(publisher.publish(event))
                .expectErrorMatches(ex -> ex instanceof EventPublishingException && ex.getCause() != null)
                .verify();
    }
}
