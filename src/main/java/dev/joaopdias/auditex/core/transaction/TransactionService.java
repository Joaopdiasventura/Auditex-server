package dev.joaopdias.auditex.core.transaction;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.transaction.dto.CreateTransactionDto;
import dev.joaopdias.auditex.core.transaction.dto.TransactionResponseDto;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.services.HashService;
import dev.joaopdias.auditex.shared.services.SignatureService;
import jakarta.transaction.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private HashService hashService;

    @Autowired
    private SignatureService signatureService;
    
    @Autowired
    private JsonMapper objectMapper;

    @Transactional
    public TransactionResponseDto create(CreateTransactionDto createTransactionDto) {
        try {
            String payload = objectMapper.writeValueAsString(createTransactionDto.payload());

            String rawContent = createTransactionDto.type() +
                    payload +
                    createTransactionDto.publicKey() +
                    createTransactionDto.nonce();

            String hash = hashService.sha256(rawContent);

            if (transactionRepository.existsByHash(hash))
                throw new IllegalStateException("Transação já existente");

            if (transactionRepository.existsByPublicKeyAndNonce(
                    createTransactionDto.publicKey(),
                    createTransactionDto.nonce()))
                throw new IllegalStateException("Tente realizar a transação novamente");

            boolean validSignature = signatureService.verify(
                    rawContent,
                    createTransactionDto.signature(),
                    createTransactionDto.publicKey());

            if (validSignature)
                throw new IllegalStateException("Assinatura inválida");

            LedgerTransaction transaction = new LedgerTransaction();

            transaction.setHash(hash);
            transaction.setType(createTransactionDto.type());
            transaction.setPayload(payload);
            transaction.setPublicKey(createTransactionDto.publicKey());
            transaction.setSignature(createTransactionDto.signature());
            transaction.setNonce(createTransactionDto.nonce());
            transaction.setStatus(TransactionStatus.PENDING);

            LedgerTransaction saved = transactionRepository.save(transaction);

            return this.toResponse(saved);

        } catch (IllegalStateException | JacksonException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void saveAll(List<LedgerTransaction> transactions) {
        transactionRepository.saveAll(transactions);
    }

    public List<LedgerTransaction> findByStatusOrderByCreatedAtAsc(TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByStatusOrderByCreatedAtAsc(status, pageable);
    }

    public TransactionResponseDto findByHash(String hash) {
        LedgerTransaction transaction = this.transactionRepository.findByHash(hash)
                .orElseThrow(() -> new IllegalStateException("Transaction not found"));
        return this.toResponse(transaction);
    }

    public long countByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public boolean existsByStatusAndCreatedAtBefore(TransactionStatus status, Instant createdAt) {
        return transactionRepository.existsByStatusAndCreatedAtBefore(status, createdAt);
    }

    private TransactionResponseDto toResponse(LedgerTransaction transaction) {
        try {
            Object payload = objectMapper.readValue(transaction.payload, Object.class);

            return new TransactionResponseDto(
                    transaction.id,
                    transaction.hash,
                    transaction.type,
                    payload,
                    transaction.publicKey,
                    transaction.signature,
                    transaction.status,
                    transaction.nonce,
                    transaction.createdAt,
                    transaction.minedAt,
                    transaction.blockId);
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception.getMessage());
        }

    }

}
