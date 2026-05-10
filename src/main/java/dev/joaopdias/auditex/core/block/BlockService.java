package dev.joaopdias.auditex.core.block;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.block.dto.BlockResponseDto;
import dev.joaopdias.auditex.core.block.dto.CreateBlockDto;
import dev.joaopdias.auditex.core.block.dto.MinedBlockDto;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.transaction.TransactionService;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.shared.services.HashService;
import jakarta.transaction.Transactional;

@Service
public class BlockService {

    private static final String GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private HashService hashService;

    @Transactional
    public BlockResponseDto create(CreateBlockDto createBlockDto) {
        if (blockRepository.existsByHash(createBlockDto.hash()))    
            throw new IllegalStateException("Bloco já existente");

        if (blockRepository.existsByIndex(createBlockDto.index()))
            throw new IllegalStateException("Índice já existente");

        Block block = new Block();

        block.setIndex(createBlockDto.index());
        block.setHash(createBlockDto.hash());
        block.setPreviousHash(createBlockDto.previousHash());
        block.setMerkleRoot(createBlockDto.merkleRoot());
        block.setNonce(createBlockDto.nonce());
        block.setDifficulty(createBlockDto.difficulty());
        block.setMinedAt(Instant.now());

        Block saved = blockRepository.save(block);

        return toResponse(saved);
    }

    public ValidateResponseDto validate() {
        int blockPageSize = 100;
        int transactionPageSize = 100;
        int blocksChecked = 0;
        int transactionsChecked = 0;
        int lastIndex = -1;

        String expectedPreviousHash = GENESIS_PREVIOUS_HASH;

        while (true) {
            List<Block> blocks = blockRepository.findByIndexGreaterThanOrderByIndexAsc(
                    lastIndex,
                    PageRequest.of(0, blockPageSize));

            if (blocks.isEmpty()) {
                return new ValidateResponseDto(
                        true,
                        blocksChecked,
                        transactionsChecked,
                        null,
                        null);
            }

            for (Block block : blocks) {
                if (!block.getIndex().equals(lastIndex + 1))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            "INVALID_BLOCK_INDEX");


                if (!block.getPreviousHash().equals(expectedPreviousHash))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            "INVALID_PREVIOUS_HASH");


                MerkleValidationResult merkleValidationResult = calculateMerkleRootByBlockId(
                        block.getId(),
                        transactionPageSize);

                transactionsChecked += merkleValidationResult.transactionsChecked();

                if (!block.getMerkleRoot().equals(merkleValidationResult.merkleRoot()))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            "INVALID_MERKLE_ROOT");


                String recalculatedHash = calculateBlockHash(block);

                if (!block.getHash().equals(recalculatedHash))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            "INVALID_BLOCK_HASH");


                String target = "0".repeat(block.getDifficulty());

                if (!block.getHash().startsWith(target)) 
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            "INVALID_PROOF_OF_WORK");

                blocksChecked++;
                expectedPreviousHash = block.getHash();
                lastIndex = block.getIndex();
            }
        }
    }

    @Transactional
    public Block saveMinedBlock(MinedBlockDto minedBlockDto) {
        
        if (blockRepository.existsByHash(minedBlockDto.hash()))
            throw new IllegalStateException("Bloco já existente");

        if (blockRepository.existsByIndex(minedBlockDto.index()))
            throw new IllegalStateException("Índice já existente");

        Block block = new Block();

        block.setIndex(minedBlockDto.index());
        block.setHash(minedBlockDto.hash());
        block.setPreviousHash(minedBlockDto.previousHash());
        block.setMerkleRoot(minedBlockDto.merkleRoot());
        block.setNonce(minedBlockDto.nonce());
        block.setDifficulty(minedBlockDto.difficulty());
        block.setMinedAt(Instant.now());

        return blockRepository.save(block);
    }

    public BlockResponseDto findByHash(String hash) {
        Block block = blockRepository.findByHash(hash).orElseThrow(() -> new IllegalStateException("Bloco não encontrado"));
        return this.toResponse(block);
    }

    public Block findLastBlockEntity() {
        return blockRepository.findTopByOrderByIndexDesc().orElse(null);
    }

    public BlockResponseDto findLastBlock() {
        Block block = this.findLastBlockEntity();

        if (block == null) throw new IllegalStateException("Nenhum bloco encontrado");
        return this.toResponse(block);
    }

    public Integer getNextIndex() {
        Block lastBlock = this.findLastBlockEntity();

        if (lastBlock == null) return 0;
        return lastBlock.getIndex() + 1;
    }

    public String getPreviousHash() {
        Block lastBlock = this.findLastBlockEntity();

        if (lastBlock == null) return GENESIS_PREVIOUS_HASH;
        return lastBlock.getHash();
    }

    private String calculateBlockHash(Block block) {
        String content = block.getIndex()
                + block.getPreviousHash()
                + block.getMerkleRoot()
                + block.getNonce()
                + block.getDifficulty();

        return hashService.sha256(content);
    }

    private MerkleValidationResult calculateMerkleRootByBlockId(UUID blockId, int pageSize) {
        List<String> transactionHashes = new ArrayList<>();
        int page = 0;

        while (true) {
            List<LedgerTransaction> transactions = transactionService.findByBlockIdOrderByBlockTransactionIndexAsc(
                blockId,
                PageRequest.of(page, pageSize));

            if (transactions.isEmpty()) break;

            List<String> hashes = transactions
                    .stream()
                    .map(LedgerTransaction::getHash)
                    .toList();

            transactionHashes.addAll(hashes);
            page++;
        }

        String merkleRoot = calculateMerkleRootFromHashes(transactionHashes);

        return new MerkleValidationResult(
                merkleRoot,
                transactionHashes.size());
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

        return calculateMerkleRootFromHashes(nextLevel);
    }

    private record MerkleValidationResult(
            String merkleRoot,
            int transactionsChecked) {
    }

    private BlockResponseDto toResponse(Block block) {
        return new BlockResponseDto(
                block.getId(),
                block.getIndex(),
                block.getHash(),
                block.getPreviousHash(),
                block.getMerkleRoot(),
                block.getNonce(),
                block.getDifficulty(),
                block.getCreatedAt(),
                block.getMinedAt());
    }
}
