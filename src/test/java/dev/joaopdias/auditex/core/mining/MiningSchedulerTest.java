package dev.joaopdias.auditex.core.mining;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import dev.joaopdias.auditex.config.RabbitMqConfig;
import dev.joaopdias.auditex.core.mining.dto.MiningRequestDto;

class MiningSchedulerTest {

    private RabbitTemplate rabbitTemplate;
    private MiningScheduler miningScheduler;

    @BeforeEach
    void setUp() {
        rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
        miningScheduler = new MiningScheduler();
        ReflectionTestUtils.setField(miningScheduler, "rabbitTemplate", rabbitTemplate);
    }

    @Test
    void scheduleMiningPublishesMiningRequestToConfiguredExchangeAndRoutingKey() {
        ArgumentCaptor<MiningRequestDto> captor = ArgumentCaptor.forClass(MiningRequestDto.class);

        miningScheduler.scheduleMining();

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.MINING_EXCHANGE),
                eq(RabbitMqConfig.MINING_ROUTING_KEY),
                captor.capture());
        assertThat(captor.getValue().requestId()).isNotNull();
        assertThat(captor.getValue().requestedAt()).isNotNull();
        assertThat(captor.getValue().reason()).isEqualTo("PERIODIC_MINING");
    }
}
