package dev.joaopdias.auditex.core.wallet.dto;

import java.time.Instant;
import java.util.UUID;

public record ReturnWalletDto(
        UUID id,
        String ownerName,
        String address,
        String publicKey,
        String privateKey,
        Instant createdAt) {
}
