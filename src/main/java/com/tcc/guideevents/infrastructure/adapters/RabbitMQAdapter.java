package com.tcc.guideevents.infrastructure.adapters;

import com.tcc.guideevents.domain.eventos.EventPublisher;
import com.tcc.guideevents.domain.exception.EventPublishingException;
import com.tcc.guideevents.domain.model.GuideEvent;
import com.rabbitmq.client.AMQP;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;
import tools.jackson.databind.ObjectMapper;

@Component
public class RabbitMQAdapter implements EventPublisher {

    private final Sender sender;
    private final ObjectMapper objectMapper;

    public RabbitMQAdapter(Sender sender, ObjectMapper objectMapper) {
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publish(GuideEvent event) {
        return Mono.fromCallable(() -> toOutboundMessage(event))
                .flatMap(outboundMessage -> sender.sendWithPublishConfirms(Flux.just(outboundMessage))
                        .next()
                        .flatMap(result -> result.isAck()
                                ? Mono.<Void>empty()
                                : Mono.error(new EventPublishingException(
                                        "El broker rechazo (nack) el evento eventId=" + event.eventId(), null))))
                .onErrorMap(ex -> !(ex instanceof EventPublishingException), ex ->
                        new EventPublishingException("No fue posible publicar el evento eventId=" + event.eventId(), ex));
    }

    private OutboundMessage toOutboundMessage(GuideEvent event) {
        GuideEventMessage message = new GuideEventMessage(
                GuideEventMessage.CURRENT_SCHEMA_VERSION,
                event.eventId(),
                event.guideId(),
                event.status().name(),
                event.occurredAt(),
                event.location(),
                event.description(),
                event.metadata(),
                event.receivedAt());
        byte[] body = objectMapper.writeValueAsBytes(message);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .messageId(event.eventId())
                .deliveryMode(2)
                .build();
        return new OutboundMessage(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, properties, body);
    }
}
