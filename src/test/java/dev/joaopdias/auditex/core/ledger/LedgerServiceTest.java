package dev.joaopdias.auditex.core.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.joaopdias.auditex.core.block.BlockRepository;
import dev.joaopdias.auditex.core.block.BlockService;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.transaction.TransactionRepository;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BlockService blockService;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService();
        ReflectionTestUtils.setField(ledgerService, "blockRepository", blockRepository);
        ReflectionTestUtils.setField(ledgerService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(ledgerService, "blockService", blockService);
    }

    @Test
    void statusReturnsAggregatedLedgerStatus() {
        Block latestBlock = new Block();
        latestBlock.setId(UUID.randomUUID());
        latestBlock.setIndex(11);
        latestBlock.setHash("a".repeat(64));
        latestBlock.setMinedAt(Instant.parse("2026-05-11T18:00:00Z"));

        when(blockService.validate()).thenReturn(new ValidateResponseDto(true, 12, 842, null, null, null));
        when(blockRepository.findTopByOrderByIndexDesc()).thenReturn(Optional.of(latestBlock));
        when(blockRepository.count()).thenReturn(12L);
        when(transactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(3L);
        when(transactionRepository.countByStatus(TransactionStatus.MINED)).thenReturn(842L);

        var response = ledgerService.status();

        assertThat(response.valid()).isTrue();
        assertThat(response.blocksCount()).isEqualTo(12);
        assertThat(response.pendingTransactions()).isEqualTo(3);
        assertThat(response.minedTransactions()).isEqualTo(842);
        assertThat(response.latestBlockIndex()).isEqualTo(11);
        assertThat(response.latestBlockHash()).isEqualTo(latestBlock.getHash());
        assertThat(response.lastMinedAt()).isEqualTo(latestBlock.getMinedAt());
    }

    @Test
    void statusHandlesEmptyLedger() {
        when(blockService.validate()).thenReturn(new ValidateResponseDto(true, 0, 0, null, null, null));
        when(blockRepository.findTopByOrderByIndexDesc()).thenReturn(Optional.empty());
        when(blockRepository.count()).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.MINED)).thenReturn(0L);

        var response = ledgerService.status();

        assertThat(response.valid()).isTrue();
        assertThat(response.blocksCount()).isZero();
        assertThat(response.latestBlockIndex()).isNull();
        assertThat(response.latestBlockHash()).isNull();
        assertThat(response.lastMinedAt()).isNull();
    }
}
