package dev.joaopdias.auditex.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String MINING_QUEUE = "auditex.mining.queue";
    public static final String MINING_EXCHANGE = "auditex.mining.exchange";
    public static final String MINING_ROUTING_KEY = "auditex.mining.request";

    @Bean
    public Queue miningQueue() {
        return new Queue(MINING_QUEUE, true);
    }

    @Bean
    public DirectExchange miningExchange() {
        return new DirectExchange(MINING_EXCHANGE, true, false);
    }

    @Bean
    public Binding miningBinding() {
        return BindingBuilder
                .bind(miningQueue())
                .to(miningExchange())
                .with(MINING_ROUTING_KEY);
    }
}