package dev.joaopdias.auditex.core.transaction.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.exceptions.ImmutableResourceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "ledger_transactions",
        indexes = {
                @Index(name = "idx_ledger_transactions_status", columnList = "status"),
                @Index(name = "idx_ledger_transactions_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_ledger_transactions_block_id", columnList = "block_id"),
                @Index(name = "idx_ledger_transactions_block_id_block_transaction_index", columnList = "block_id, block_transaction_index"),
                @Index(name = "idx_ledger_transactions_public_key", columnList = "public_key"),
                @Index(name = "idx_ledger_transactions_public_key_created_at", columnList = "public_key, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ledger_transactions_hash", columnNames = "hash"),
                @UniqueConstraint(name = "uk_ledger_transactions_public_key_nonce", columnNames = {"public_key", "nonce"}),
                @UniqueConstraint(name = "uk_ledger_transactions_block_id_block_transaction_index", columnNames = {"block_id", "block_transaction_index"})
        }
)
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @Column(nullable = false, length = 64)
    public String hash;

    @Column(nullable = false, length = 100)
    public String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    public String payload;

    @Column(nullable = false, name = "public_key", columnDefinition = "TEXT")
    public String publicKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public TransactionStatus status;

    @Column(nullable = false)
    public String nonce;

    @Column(nullable = false, name = "created_at")
    public Instant createdAt;

    @Column(name = "mined_at")
    public Instant minedAt;

    @Column(name = "block_id")
    public UUID blockId;

    @Column(name = "block_transaction_index")
    public Integer blockTransactionIndex;

    @Transient
    private String originalHash;

    @Transient
    private String originalType;

    @Transient
    private String originalPayload;

    @Transient
    private String originalPublicKey;

    @Transient
    private String originalSignature;

    @Transient
    private String originalNonce;

    @Transient
    private TransactionStatus originalStatus;

    @Transient
    private Instant originalMinedAt;

    @Transient
    private UUID originalBlockId;

    @Transient
    private Integer originalBlockTransactionIndex;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();

        if (this.status == null) this.status = TransactionStatus.PENDING;
    }

    @PostLoad
    public void postLoad() {
        this.originalHash = this.hash;
        this.originalType = this.type;
        this.originalPayload = this.payload;
        this.originalPublicKey = this.publicKey;
        this.originalSignature = this.signature;
        this.originalNonce = this.nonce;
        this.originalStatus = this.status;
        this.originalMinedAt = this.minedAt;
        this.originalBlockId = this.blockId;
        this.originalBlockTransactionIndex = this.blockTransactionIndex;
    }

    @PreUpdate
    public void preUpdate() {
        if (this.originalStatus != TransactionStatus.MINED) return;

        if (!Objects.equals(this.originalHash, this.hash)
                || !Objects.equals(this.originalType, this.type)
                || !Objects.equals(this.originalPayload, this.payload)
                || !Objects.equals(this.originalPublicKey, this.publicKey)
                || !Objects.equals(this.originalSignature, this.signature)
                || !Objects.equals(this.originalNonce, this.nonce)
                || !Objects.equals(this.originalBlockId, this.blockId)
                || !Objects.equals(this.originalMinedAt, this.minedAt)
                || !Objects.equals(this.originalBlockTransactionIndex, this.blockTransactionIndex))
            throw new ImmutableResourceException("Transação minerada não pode ser alterada");
    }
}
