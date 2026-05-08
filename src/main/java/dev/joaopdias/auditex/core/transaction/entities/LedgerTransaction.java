package dev.joaopdias.auditex.core.transaction.entities;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ledger_transaction")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @Column(nullable = false, unique = true, length = 64)
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

    @Column(nullable = false, unique = true)
    public String nonce;

    @Column(nullable = false, name = "created_at")
    public Instant createdAt;

    @Column(name = "mined_at")
    public Instant minedAt;

    @Column(name = "block_id")
    public UUID blockId;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();

        if (this.status == null)
            this.status = TransactionStatus.PENDING;
    }
}
