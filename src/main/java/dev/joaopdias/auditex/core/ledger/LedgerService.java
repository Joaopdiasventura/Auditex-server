package dev.joaopdias.auditex.core.ledger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.block.BlockRepository;
import dev.joaopdias.auditex.core.block.BlockService;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.ledger.dto.LedgerStatusResponseDto;
import dev.joaopdias.auditex.core.transaction.TransactionRepository;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;

@Service
public class LedgerService {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BlockService blockService;

    public LedgerStatusResponseDto status() {
        ValidateResponseDto validation = blockService.validate();
        Block latestBlock = blockRepository.findTopByOrderByIndexDesc().orElse(null);

        return new LedgerStatusResponseDto(
                validation.valid(),
                blockRepository.count(),
                transactionRepository.countByStatus(TransactionStatus.PENDING),
                transactionRepository.countByStatus(TransactionStatus.MINED),
                latestBlock == null ? null : latestBlock.getIndex(),
                latestBlock == null ? null : latestBlock.getHash(),
                latestBlock == null ? null : latestBlock.getMinedAt());
    }

    public ValidateResponseDto validate() {
        return blockService.validate();
    }
}
