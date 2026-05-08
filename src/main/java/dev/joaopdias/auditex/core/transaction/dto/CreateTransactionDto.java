package dev.joaopdias.auditex.core.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTransactionDto(
        @NotBlank String type,
        @NotNull Object payload,
        @NotBlank String publicKey,
        @NotBlank String signature,
        @NotBlank String nonce) {
}
