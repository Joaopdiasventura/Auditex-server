package dev.joaopdias.auditex.core.mining;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import dev.joaopdias.auditex.config.RabbitMqConfig;
import dev.joaopdias.auditex.core.mining.dto.MiningRequestDto;

@Component
public class MiningWorker {
    private final MiningService miningService;

    public MiningWorker(MiningService miningService) {
        this.miningService = miningService;
    }

    @RabbitListener(queues = RabbitMqConfig.MINING_QUEUE)
    public void consume(MiningRequestDto message) {
        miningService.minePendingTransactions();
    }
}