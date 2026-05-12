package dev.joaopdias.auditex.core.mining;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.joaopdias.auditex.core.block.BlockService;
import dev.joaopdias.auditex.core.block.dto.MinedBlockDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.transaction.TransactionService;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.services.HashService;

@ExtendWith(MockitoExtension.class)
class MiningServiceTest {

    private static final String PREVIOUS_HASH = "0".repeat(64);
    private static final String MINED_HASH = "0".repeat(4) + "a".repeat(60);

    @Mock
    private TransactionService transactionService;

    @Mock
    private BlockService blockService;

    @Mock
    private HashService hashService;

    private MiningService miningService;

    @BeforeEach
     void setUp() {
        miningService = new MiningService();
        ReflectionTestUtils.setField(miningService, "transactionService", transactionService);
        ReflectionTestUtils.setField(miningService, "blockService", blockService);
        ReflectionTestUtils.setField(miningService, "hashService", hashService);
    }

    @Test
    void minePendingTransactionsDoesNothingWhenThereAreNoPendingTransactions() {
        when(transactionService.countByStatus(TransactionStatus.PENDING)).thenReturn(0L);

        miningService.minePendingTransactions();

        verify(transactionService, never()).findByStatusOrderByCreatedAtAscForUpdateSkipLocked(any(), anyInt());
        verify(blockService, never()).saveMinedBlock(any());
    }

    @Test
    void minePendingTransactionsWaitsWhenSmallBatchHasNoOldTransactions() {
        when(transactionService.countByStatus(TransactionStatus.PENDING)).thenReturn(10L);
        when(transactionService.existsByStatusAndCreatedAtBefore(eq(TransactionStatus.PENDING), any()))
                .thenReturn(false);

        miningService.minePendingTransactions();

        verify(transactionService, never()).findByStatusOrderByCreatedAtAscForUpdateSkipLocked(any(), anyInt());
        verify(blockService, never()).saveMinedBlock(any());
    }

    @Test
    void minePendingTransactionsMinesSmallBatchWhenOldTransactionExists() {
        List<LedgerTransaction> transactions = transactions(2);
        Block block = block(UUID.randomUUID());

        when(transactionService.countByStatus(TransactionStatus.PENDING)).thenReturn(2L);
        when(transactionService.existsByStatusAndCreatedAtBefore(eq(TransactionStatus.PENDING), any()))
                .thenReturn(true);
        stubMining(transactions, block);

        miningService.minePendingTransactions();

        assertMined(transactions, block);
    }

    @Test
    void minePendingTransactionsMinesImmediatelyWhenThereAreAtLeastOneHundredTransactions() {
        List<LedgerTransaction> transactions = transactions(100);
        Block block = block(UUID.randomUUID());

        when(transactionService.countByStatus(TransactionStatus.PENDING)).thenReturn(100L);
        stubMining(transactions, block);

        miningService.minePendingTransactions();

        assertMined(transactions, block);
        verify(transactionService).findByStatusOrderByCreatedAtAscForUpdateSkipLocked(TransactionStatus.PENDING, 100);
    }

    private void stubMining(List<LedgerTransaction> transactions, Block block) {
        when(transactionService.findByStatusOrderByCreatedAtAscForUpdateSkipLocked(TransactionStatus.PENDING, 100))
                .thenReturn(transactions);
        when(blockService.getNextIndex()).thenReturn(7);
        when(blockService.getPreviousHash()).thenReturn(PREVIOUS_HASH);
        when(hashService.sha256(any())).thenReturn(MINED_HASH);
        when(blockService.saveMinedBlock(any(MinedBlockDto.class))).thenReturn(block);
    }

    private void assertMined(List<LedgerTransaction> transactions, Block block) {
        ArgumentCaptor<MinedBlockDto> blockCaptor = ArgumentCaptor.forClass(MinedBlockDto.class);

        verify(blockService).saveMinedBlock(blockCaptor.capture());
        verify(transactionService).saveAll(transactions);

        assertThat(blockCaptor.getValue().index()).isEqualTo(7);
        assertThat(blockCaptor.getValue().previousHash()).isEqualTo(PREVIOUS_HASH);
        assertThat(blockCaptor.getValue().hash()).isEqualTo(MINED_HASH);

        for (int index = 0; index < transactions.size(); index++) {
            LedgerTransaction transaction = transactions.get(index);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.MINED);
            assertThat(transaction.getBlockId()).isEqualTo(block.getId());
            assertThat(transaction.getMinedAt()).isNotNull();
            assertThat(transaction.getBlockTransactionIndex()).isEqualTo(index);
        }
    }

    private List<LedgerTransaction> transactions(int count) {
        List<LedgerTransaction> transactions = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            LedgerTransaction transaction = new LedgerTransaction();
            transaction.setId(UUID.randomUUID());
            transaction.setHash("hash-" + index);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setCreatedAt(Instant.parse("2026-05-11T18:00:00Z").plusSeconds(index));
            transactions.add(transaction);
        }

        return transactions;
    }

    private Block block(UUID id) {
        Block block = new Block();
        block.setId(id);
        block.setIndex(7);
        block.setHash(MINED_HASH);
        block.setPreviousHash(PREVIOUS_HASH);
        block.setMerkleRoot("merkle-root");
        block.setNonce(0L);
        block.setDifficulty(4);
        block.setCreatedAt(Instant.parse("2026-05-11T18:00:00Z"));
        block.setMinedAt(Instant.parse("2026-05-11T18:00:01Z"));
        return block;
    }
}
