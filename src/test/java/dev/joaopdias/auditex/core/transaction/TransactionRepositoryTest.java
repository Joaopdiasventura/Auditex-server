package dev.joaopdias.auditex.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void pageByProcessingIdFiltersUsingJsonb() {
        String processingId = UUID.randomUUID().toString();
        LedgerTransaction transaction = transaction(processingId, "file-hash-1");

        transactionRepository.saveAndFlush(transaction);

        var page = transactionRepository.pageByProcessingId(processingId, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getHash()).isEqualTo(transaction.getHash());
    }

    @Test
    void pageByFileHashFiltersUsingJsonb() {
        String fileHash = "file-" + UUID.randomUUID();
        LedgerTransaction transaction = transaction(UUID.randomUUID().toString(), fileHash);

        transactionRepository.saveAndFlush(transaction);

        var page = transactionRepository.pageByFileHash(fileHash, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getHash()).isEqualTo(transaction.getHash());
    }

    private LedgerTransaction transaction(String processingId, String fileHash) {
        String nonce = UUID.randomUUID().toString();
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        transaction.setType("BILLING_FILE_RECEIVED");
        transaction.setPayload("{\"processingId\":\"" + processingId + "\",\"fileHash\":\"" + fileHash + "\"}");
        transaction.setPublicKey("public-key-" + UUID.randomUUID());
        transaction.setSignature("signature");
        transaction.setNonce(nonce);
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }
}
