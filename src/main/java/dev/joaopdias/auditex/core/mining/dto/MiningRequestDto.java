package dev.joaopdias.auditex.core.mining.dto;

import java.time.Instant;
import java.util.UUID;

public record MiningRequestDto(
        UUID requestId,
        Instant requestedAt,
        String reason
    ) {}
