package dev.joaopdias.auditex.core.mining;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joaopdias.auditex.core.mining.dto.MiningRequestDto;

class MiningWorkerTest {

    @Test
    void consumeDelegatesToMiningService() {
        MiningService miningService = mock(MiningService.class);
        MiningWorker worker = new MiningWorker(miningService);

        worker.consume(new MiningRequestDto(UUID.randomUUID(), Instant.now(), "PERIODIC_MINING"));

        verify(miningService).minePendingTransactions();
    }
}
