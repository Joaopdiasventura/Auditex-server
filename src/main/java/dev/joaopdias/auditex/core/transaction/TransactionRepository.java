package dev.joaopdias.auditex.core.transaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    Optional<LedgerTransaction> findByHash(String hash);

    boolean existsByHash(String hash);

    boolean existsByPublicKeyAndNonce(String publicKey, String nonce);

    boolean existsByStatusAndCreatedAtBefore(TransactionStatus status, Instant createdAt);
    
    long countByStatus(TransactionStatus status);

    long countByBlockId(UUID blockId);

    List<LedgerTransaction> findByStatusOrderByCreatedAtAsc(TransactionStatus status, Pageable pageable);

    List<LedgerTransaction> findByBlockIdOrderByBlockTransactionIndexAsc(UUID blockId, Pageable pageable);
}
