package dev.joaopdias.auditex.core.block.dto;

import dev.joaopdias.auditex.core.transaction.dto.TransactionResponseDto;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;

public record BlockTransactionsResponseDto(
        BlockResponseDto block,
        PageResponseDto<TransactionResponseDto> transactions
) {
}
