package dev.joaopdias.auditex.core.mining;

import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.joaopdias.auditex.config.RabbitMqConfig;
import dev.joaopdias.auditex.core.mining.dto.MiningRequestDto;

@Component
public class MiningScheduler {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(initialDelay = 1000, fixedDelay = 5000)
    public void scheduleMining() {
        MiningRequestDto message = new MiningRequestDto(
                UUID.randomUUID(),
                Instant.now(),
                "PERIODIC_MINING");

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.MINING_EXCHANGE,
                RabbitMqConfig.MINING_ROUTING_KEY,
                message);
    }
}
