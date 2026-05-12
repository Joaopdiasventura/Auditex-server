package dev.joaopdias.auditex.core.transaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByPublicKeyAndNonce(String publicKey, String nonce);

    @Query("select t from LedgerTransaction t order by t.createdAt desc")
    Page<LedgerTransaction> pageTransactions(Pageable pageable);

    @Query("select t from LedgerTransaction t where t.publicKey = :publicKey order by t.createdAt desc")
    Page<LedgerTransaction> pageByPublicKey(
            @Param("publicKey") String publicKey,
            Pageable pageable);

    @Query("select t from LedgerTransaction t where t.type = :type order by t.createdAt desc")
    Page<LedgerTransaction> pageByType(
            @Param("type") String type,
            Pageable pageable);

    @Query("select t from LedgerTransaction t where t.blockId = :blockId order by t.blockTransactionIndex asc")
    Page<LedgerTransaction> pageByBlockId(
            @Param("blockId") UUID blockId,
            Pageable pageable);

    @Query(value = """
            SELECT *
            FROM ledger_transactions
            WHERE payload ->> 'processingId' = :processingId
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM ledger_transactions
            WHERE payload ->> 'processingId' = :processingId
            """,
            nativeQuery = true)
    Page<LedgerTransaction> pageByProcessingId(
            @Param("processingId") String processingId,
            Pageable pageable);

    @Query(value = """
            SELECT *
            FROM ledger_transactions
            WHERE payload ->> 'fileHash' = :fileHash
            ORDER BY created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM ledger_transactions
            WHERE payload ->> 'fileHash' = :fileHash
            """,
            nativeQuery = true)
    Page<LedgerTransaction> pageByFileHash(
            @Param("fileHash") String fileHash,
            Pageable pageable);

    List<LedgerTransaction> findByStatusOrderByCreatedAtAsc(TransactionStatus status, Pageable pageable);

    @Query(value = """
            SELECT *
            FROM ledger_transactions
            WHERE status = :status
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<LedgerTransaction> findByStatusOrderByCreatedAtAscForUpdateSkipLocked(
            @Param("status") String status,
            @Param("limit") int limit);

    List<LedgerTransaction> findByBlockIdOrderByBlockTransactionIndexAsc(UUID blockId, Pageable pageable);
}
