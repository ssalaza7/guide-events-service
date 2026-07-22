package com.tcc.guideevents.infrastructure.adapters;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import java.time.Duration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "tcc.guide-events.exchange";
    public static final String QUEUE = "tcc.guide-events.status.queue";
    public static final String ROUTING_KEY = "guide.event.status.updated";

    @Bean
    public ConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.useNio();
        return connectionFactory;
    }

    @Bean(destroyMethod = "close")
    public Sender sender(ConnectionFactory connectionFactory) {
        return RabbitFlux.createSender(new SenderOptions().connectionFactory(connectionFactory));
    }

    // unico block() del proyecto: solo al arrancar, para fallar rapido si el broker no responde
    @Bean
    public ApplicationRunner declareGuideEventsTopology(Sender sender) {
        return args -> sender.declareExchange(ExchangeSpecification.exchange(EXCHANGE).type("topic").durable(true))
                .then(sender.declareQueue(QueueSpecification.queue(QUEUE).durable(true)))
                .then(sender.bind(BindingSpecification.binding(EXCHANGE, ROUTING_KEY, QUEUE)))
                .block(Duration.ofSeconds(10));
    }
}
