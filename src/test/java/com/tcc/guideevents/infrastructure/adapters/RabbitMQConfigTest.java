package com.tcc.guideevents.infrastructure.adapters;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void buildsConnectionFactoryWithGivenProperties() {
        ConnectionFactory connectionFactory = config.rabbitConnectionFactory("myhost", 5673, "user", "pass");

        assertThat(connectionFactory.getHost()).isEqualTo("myhost");
        assertThat(connectionFactory.getPort()).isEqualTo(5673);
    }

    @Test
    void createsSenderBoundToConnectionFactory() {
        ConnectionFactory connectionFactory = config.rabbitConnectionFactory("localhost", 5672, "guest", "guest");

        try (Sender sender = config.sender(connectionFactory)) {
            assertThat(sender).isNotNull();
        }
    }

    @Test
    void declaresTopologyUsingSenderChain() throws Exception {
        Sender sender = mock(Sender.class);
        when(sender.declareExchange(any())).thenReturn(Mono.<AMQP.Exchange.DeclareOk>empty());
        when(sender.declareQueue(any())).thenReturn(Mono.<AMQP.Queue.DeclareOk>empty());
        when(sender.bind(any())).thenReturn(Mono.<AMQP.Queue.BindOk>empty());

        ApplicationRunner runner = config.declareGuideEventsTopology(sender);
        runner.run(mock(ApplicationArguments.class));

        verify(sender, times(1)).declareExchange(any());
        verify(sender, times(1)).declareQueue(any());
        verify(sender, times(1)).bind(any());
    }
}
