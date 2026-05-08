package dev.joaopdias.auditex.core.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWalletDto(@NotBlank @Size(min = 2, max = 120) String ownerName) {

}
