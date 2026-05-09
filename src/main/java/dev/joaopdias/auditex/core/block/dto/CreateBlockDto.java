package dev.joaopdias.auditex.core.block.dto;

public record CreateBlockDto(
        Integer index,
        String hash,
        String previousHash,
        String merkleRoot,
        Long nonce,
        Integer difficulty
    ) {}
