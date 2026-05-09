package dev.joaopdias.auditex.core.block.dto;

import java.time.Instant;
import java.util.UUID;

public record BlockResponseDto(
        UUID id,
        Integer index,
        String hash,
        String previousHash,
        String merkleRoot,
        Long nonce,
        Integer difficulty,
        Instant createdAt,
        Instant minedAt
    ) {}