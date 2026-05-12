package dev.joaopdias.auditex.core.transaction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.transaction.dto.CreateTransactionDto;
import dev.joaopdias.auditex.core.transaction.dto.TransactionResponseDto;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;
import dev.joaopdias.auditex.shared.exceptions.BadRequestException;
import dev.joaopdias.auditex.shared.exceptions.ConflictException;
import dev.joaopdias.auditex.shared.exceptions.InvalidSignatureException;
import dev.joaopdias.auditex.shared.exceptions.ResourceNotFoundException;
import dev.joaopdias.auditex.shared.services.HashService;
import dev.joaopdias.auditex.shared.services.SignatureService;
import jakarta.transaction.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class TransactionService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern SHA_256_HASH_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private HashService hashService;

    @Autowired
    private SignatureService signatureService;
    
    @Autowired
    private JsonMapper objectMapper;

    private final ObjectMapper canonicalObjectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    @Transactional
    public TransactionResponseDto create(CreateTransactionDto createTransactionDto) {
        try {
            String payload = objectMapper.writeValueAsString(createTransactionDto.payload());
            String canonicalPayload = canonicalizePayload(createTransactionDto.payload());

            String rawContent = createTransactionDto.type() +
                    canonicalPayload +
                    createTransactionDto.publicKey() +
                    createTransactionDto.nonce();

            String hash = hashService.sha256(rawContent);

            if (transactionRepository.existsByHash(hash))
                throw new ConflictException("Transação já existente");

            if (transactionRepository.existsByPublicKeyAndNonce(
                    createTransactionDto.publicKey(),
                    createTransactionDto.nonce()))
                throw new ConflictException("Nonce já utilizado para esta publicKey");

            boolean validSignature = signatureService.verify(
                    rawContent,
                    createTransactionDto.signature(),
                    createTransactionDto.publicKey());

            if (!validSignature)
                throw new InvalidSignatureException("Assinatura inválida");

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

        } catch (JacksonException | JsonProcessingException exception) {
            throw new BadRequestException("Payload inválido", exception);
        }
    }

    public void saveAll(List<LedgerTransaction> transactions) {
        transactionRepository.saveAll(transactions);
    }

    public List<LedgerTransaction> findByStatusOrderByCreatedAtAsc(TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByStatusOrderByCreatedAtAsc(status, pageable);
    }

    public PageResponseDto<TransactionResponseDto> pageTransactions(Integer page, Integer size) {
        Page<TransactionResponseDto> transactions = transactionRepository
                .pageTransactions(pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(transactions);
    }

    public PageResponseDto<TransactionResponseDto> pageByPublicKey(String publicKey, Integer page, Integer size) {
        Page<TransactionResponseDto> transactions = transactionRepository
                .pageByPublicKey(publicKey, pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(transactions);
    }

    public PageResponseDto<TransactionResponseDto> pageByType(String type, Integer page, Integer size) {
        Page<TransactionResponseDto> transactions = transactionRepository
                .pageByType(type, pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(transactions);
    }

    public PageResponseDto<TransactionResponseDto> pageByProcessingId(
            String processingId,
            Integer page,
            Integer size) {
        Page<TransactionResponseDto> transactions = transactionRepository
                .pageByProcessingId(processingId, pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(transactions);
    }

    public PageResponseDto<TransactionResponseDto> pageByFileHash(String fileHash, Integer page, Integer size) {
        Page<TransactionResponseDto> transactions = transactionRepository
                .pageByFileHash(fileHash, pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(transactions);
    }

    public PageResponseDto<TransactionResponseDto> pageByBlockId(UUID blockId, Integer page, Integer size) {
        Page<TransactionResponseDto> transactions = transactionRepository
                .pageByBlockId(blockId, pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(transactions);
    }

    public List<LedgerTransaction> findByStatusOrderByCreatedAtAscForUpdateSkipLocked(
            TransactionStatus status,
            int limit) {
        return transactionRepository.findByStatusOrderByCreatedAtAscForUpdateSkipLocked(
                status.name(),
                limit);
    }

    public List<LedgerTransaction> findByBlockIdOrderByBlockTransactionIndexAsc(UUID blockId, Pageable pageable) {
        return transactionRepository.findByBlockIdOrderByBlockTransactionIndexAsc(blockId, pageable);
    }

    public TransactionResponseDto findByHash(String hash) {
        validateHash(hash);

        LedgerTransaction transaction = this.transactionRepository.findByHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        return this.toResponse(transaction);
    }

    public long countByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public long countByBlockId(UUID blockId) {
        return transactionRepository.countByBlockId(blockId);
    }

    public long countByPublicKeyAndNonce(String publicKey, String nonce) {
        return transactionRepository.countByPublicKeyAndNonce(publicKey, nonce);
    }

    public boolean existsByStatusAndCreatedAtBefore(TransactionStatus status, Instant createdAt) {
        return transactionRepository.existsByStatusAndCreatedAtBefore(status, createdAt);
    }

    private Pageable pageRequest(Integer page, Integer size) {
        if (page != null && page < 0)
            throw new BadRequestException("Parâmetro page inválido");

        if (size != null && (size < 1 || size > MAX_PAGE_SIZE))
            throw new BadRequestException("Parâmetro size inválido");

        return PageRequest.of(
                page == null ? 0 : page,
                size == null ? DEFAULT_PAGE_SIZE : size);
    }

    private void validateHash(String hash) {
        if (hash == null || !SHA_256_HASH_PATTERN.matcher(hash).matches())
            throw new BadRequestException("Hash inválido");
    }

    private String canonicalizePayload(Object payload) throws JsonProcessingException {
        Object normalizedPayload = canonicalObjectMapper.readValue(
                canonicalObjectMapper.writeValueAsString(payload),
                Object.class);

        return canonicalObjectMapper.writeValueAsString(normalizedPayload);
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
                    transaction.blockId,
                    transaction.blockTransactionIndex);
        } catch (JacksonException exception) {
            throw new BadRequestException("Payload inválido", exception);
        }

    }

}
