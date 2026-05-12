package dev.joaopdias.auditex.core.transaction;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.exceptions.ImmutableResourceException;

class LedgerTransactionTest {

    @Test
    void prePersistSetsCreatedAtAndDefaultPendingStatus() {
        LedgerTransaction transaction = new LedgerTransaction();

        transaction.prePersist();

        assertThatCode(() -> {
            if (transaction.getCreatedAt() == null || transaction.getStatus() != TransactionStatus.PENDING)
                throw new AssertionError();
        }).doesNotThrowAnyException();
    }

    @Test
    void preUpdateRejectsChangingImmutableFieldsAfterTransactionWasMined() {
        LedgerTransaction transaction = minedTransaction();
        transaction.postLoad();

        transaction.setPayload("{\"changed\":true}");

        assertThatThrownBy(transaction::preUpdate)
                .isInstanceOf(ImmutableResourceException.class)
                .hasMessage("Transação minerada não pode ser alterada");
    }

    @Test
    void preUpdateAllowsPendingTransactionChanges() {
        LedgerTransaction transaction = minedTransaction();
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.postLoad();

        transaction.setPayload("{\"changed\":true}");

        assertThatCode(transaction::preUpdate).doesNotThrowAnyException();
    }

    private LedgerTransaction minedTransaction() {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setHash("a".repeat(64));
        transaction.setType("BILLING_FILE_RECEIVED");
        transaction.setPayload("{\"processingId\":\"process-1\"}");
        transaction.setPublicKey("public-key");
        transaction.setSignature("signature");
        transaction.setNonce("nonce-1");
        transaction.setStatus(TransactionStatus.MINED);
        transaction.setCreatedAt(Instant.parse("2026-05-11T18:00:00Z"));
        transaction.setMinedAt(Instant.parse("2026-05-11T18:00:01Z"));
        transaction.setBlockId(UUID.randomUUID());
        transaction.setBlockTransactionIndex(0);
        return transaction;
    }
}
