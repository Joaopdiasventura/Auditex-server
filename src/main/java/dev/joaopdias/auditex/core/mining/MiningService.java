package dev.joaopdias.auditex.core.mining;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.block.BlockService;
import dev.joaopdias.auditex.core.block.dto.MinedBlockDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.transaction.TransactionService;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.services.HashService;
import jakarta.transaction.Transactional;

@Service
public class MiningService {

    private static final Integer DEFAULT_DIFFICULTY = 4;
    private static final Integer MAX_TRANSACTIONS_PER_BLOCK = 100;
    private static final Integer MAX_PENDING_TRANSACTION_AGE_SECONDS = 15;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BlockService blockService;

    @Autowired
    private HashService hashService;


    @Transactional
    public void minePendingTransactions() {
        long pendingCount = transactionService.countByStatus(TransactionStatus.PENDING);

        if (pendingCount == 0) return;

        if (pendingCount >= MAX_TRANSACTIONS_PER_BLOCK) {
            mineBlock(MAX_TRANSACTIONS_PER_BLOCK);
            return;
        }

        boolean hasOldTransactions = transactionService.existsByStatusAndCreatedAtBefore(
                TransactionStatus.PENDING,
                Instant.now().minusSeconds(MAX_PENDING_TRANSACTION_AGE_SECONDS));

        if (hasOldTransactions) mineBlock(MAX_TRANSACTIONS_PER_BLOCK);
    }

    private void mineBlock(int limit) {
        List<LedgerTransaction> pendingTransactions = transactionService
                .findByStatusOrderByCreatedAtAscForUpdateSkipLocked(TransactionStatus.PENDING, limit);

        if (pendingTransactions.isEmpty()) return;

        for (LedgerTransaction transaction : pendingTransactions)
            transaction.setStatus(TransactionStatus.PROCESSING);

        Integer index = blockService.getNextIndex();
        String previousHash = blockService.getPreviousHash();
        String merkleRoot = this.calculateMerkleRoot(pendingTransactions);
        Integer difficulty = DEFAULT_DIFFICULTY;

        MiningResult result = proofOfWork(index, previousHash, merkleRoot, difficulty);

        MinedBlockDto minedBlockDto = new MinedBlockDto(
                index,
                result.hash(),
                previousHash,
                merkleRoot,
                result.nonce(),
                difficulty);

        Block block = blockService.saveMinedBlock(minedBlockDto);

        Instant minedAt = Instant.now();

        for (int i = 0; i < pendingTransactions.size(); i++) {
            LedgerTransaction transaction = pendingTransactions.get(i);

            transaction.setStatus(TransactionStatus.MINED);
            transaction.setBlockId(block.getId());
            transaction.setMinedAt(minedAt);
            transaction.setBlockTransactionIndex(i);
        }

        transactionService.saveAll(pendingTransactions);
    }

    private MiningResult proofOfWork(Integer index, String previousHash, String merkleRoot, Integer difficulty) {
        Long nonce = 0L;
        String target = "0".repeat(difficulty);

        while (true) {
            String content = index + previousHash + merkleRoot + nonce + difficulty;
            String hash = hashService.sha256(content);

            if (hash.startsWith(target)) return new MiningResult(hash, nonce);

            nonce++;
        }
    }

    private String calculateMerkleRoot(List<LedgerTransaction> transactions) {
        List<String> hashes = transactions
                .stream()
                .map(LedgerTransaction::getHash)
                .toList();

        return this.calculateMerkleRootFromHashes(hashes);
    }

    private String calculateMerkleRootFromHashes(List<String> hashes) {
        if (hashes.isEmpty()) return hashService.sha256("");

        if (hashes.size() == 1) return hashes.get(0);

        List<String> nextLevel = new ArrayList<>();

        for (int index = 0; index < hashes.size(); index += 2) {
            String left = hashes.get(index);
            String right = index + 1 < hashes.size() ? hashes.get(index + 1) : left;

            nextLevel.add(hashService.sha256(left + right));
        }

        return this.calculateMerkleRootFromHashes(nextLevel);
    }

    private record MiningResult(String hash, Long nonce) {
    }
}
