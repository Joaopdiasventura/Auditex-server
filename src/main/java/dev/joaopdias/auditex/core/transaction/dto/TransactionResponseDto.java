package dev.joaopdias.auditex.core.transaction.dto;

import java.time.Instant;
import java.util.UUID;

import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;

public record TransactionResponseDto(
        UUID id,
        String hash,
        String type,
        Object payload,
        String publicKey,
        String signature,
        TransactionStatus status,
        String nonce,
        Instant createdAt,
        Instant minedAt,
        UUID blockId) {
}
