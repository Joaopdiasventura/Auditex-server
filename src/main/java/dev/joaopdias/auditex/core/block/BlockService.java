package dev.joaopdias.auditex.core.block;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.block.dto.BlockResponseDto;
import dev.joaopdias.auditex.core.block.dto.CreateBlockDto;
import dev.joaopdias.auditex.core.block.dto.MinedBlockDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import jakarta.transaction.Transactional;

@Service
public class BlockService {

    private static final String GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Autowired
    private BlockRepository blockRepository;

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
