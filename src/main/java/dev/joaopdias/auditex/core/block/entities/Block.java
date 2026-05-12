package dev.joaopdias.auditex.core.block.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import dev.joaopdias.auditex.shared.exceptions.ImmutableResourceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "blocks")
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "block_index", nullable = false, unique = true)
    private Integer index;

    @Column(nullable = false, unique = true, length = 64)
    private String hash;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "merkle_root", nullable = false, length = 64)
    private String merkleRoot;

    @Column(nullable = false)
    private Long nonce;

    @Column(nullable = false)
    private Integer difficulty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "mined_at", nullable = false)
    private Instant minedAt;

    @Transient
    private Integer originalIndex;

    @Transient
    private String originalHash;

    @Transient
    private String originalPreviousHash;

    @Transient
    private String originalMerkleRoot;

    @Transient
    private Long originalNonce;

    @Transient
    private Integer originalDifficulty;

    @Transient
    private Instant originalMinedAt;

    @PrePersist
    public void prePersist() {
            this.createdAt = Instant.now();
        
    }

    @PostLoad
    public void postLoad() {
        this.originalIndex = this.index;
        this.originalHash = this.hash;
        this.originalPreviousHash = this.previousHash;
        this.originalMerkleRoot = this.merkleRoot;
        this.originalNonce = this.nonce;
        this.originalDifficulty = this.difficulty;
        this.originalMinedAt = this.minedAt;
    }

    @PreUpdate
    public void preUpdate() {
        if (!Objects.equals(this.originalIndex, this.index)
                || !Objects.equals(this.originalHash, this.hash)
                || !Objects.equals(this.originalPreviousHash, this.previousHash)
                || !Objects.equals(this.originalMerkleRoot, this.merkleRoot)
                || !Objects.equals(this.originalNonce, this.nonce)
                || !Objects.equals(this.originalDifficulty, this.difficulty)
                || !Objects.equals(this.originalMinedAt, this.minedAt))
            throw new ImmutableResourceException("Bloco não pode ser alterado");
    }
}
