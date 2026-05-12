package dev.joaopdias.auditex.core.block.dto;

import java.util.UUID;

public record ValidateResponseDto(
        boolean valid,
        int blocksChecked,
        int transactionsChecked,
        UUID brokenAtBlock,
        UUID brokenAtTransaction,
        String reason
    ) {}
