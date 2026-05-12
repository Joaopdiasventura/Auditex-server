package dev.joaopdias.auditex.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void miningQueueExchangeAndBindingUseExpectedNames() {
        var queue = config.miningQueue();
        var exchange = config.miningExchange();
        var binding = config.miningBinding();

        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.MINING_QUEUE);
        assertThat(exchange.getName()).isEqualTo(RabbitMqConfig.MINING_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.MINING_ROUTING_KEY);
    }
}
