package dev.joaopdias.auditex.core.ledger.dto;

import java.time.Instant;

public record LedgerStatusResponseDto(
        boolean valid,
        long blocksCount,
        long pendingTransactions,
        long minedTransactions,
        Integer latestBlockIndex,
        String latestBlockHash,
        Instant lastMinedAt
) {
}
