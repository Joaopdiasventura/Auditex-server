package dev.joaopdias.auditex.core.transaction;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import dev.joaopdias.auditex.core.transaction.dto.CreateTransactionDto;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.exceptions.BadRequestException;
import dev.joaopdias.auditex.shared.exceptions.ConflictException;
import dev.joaopdias.auditex.shared.exceptions.InvalidSignatureException;
import dev.joaopdias.auditex.shared.exceptions.ResourceNotFoundException;
import dev.joaopdias.auditex.shared.services.HashService;
import dev.joaopdias.auditex.shared.services.SignatureService;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String HASH = "a".repeat(64);
    private static final String PUBLIC_KEY = "public-key";
    private static final String SIGNATURE = "signature";
    private static final String NONCE = "nonce-1";
    private static final String TYPE = "BILLING_FILE_RECEIVED";
    private static final String PAYLOAD_JSON = "{\"processingId\":\"process-1\"}";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private HashService hashService;

    @Mock
    private SignatureService signatureService;

    @Mock
    private JsonMapper objectMapper;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(transactionService, "hashService", hashService);
        ReflectionTestUtils.setField(transactionService, "signatureService", signatureService);
        ReflectionTestUtils.setField(transactionService, "objectMapper", objectMapper);
    }

    @Test
    void createStoresPendingTransactionWhenSignatureIsValid() throws Exception {
        Map<String, String> payload = Map.of("processingId", "process-1");
        CreateTransactionDto request = new CreateTransactionDto(TYPE, payload, PUBLIC_KEY, SIGNATURE, NONCE);
        String rawContent = TYPE + PAYLOAD_JSON + PUBLIC_KEY + NONCE;

        when(objectMapper.writeValueAsString(payload)).thenReturn(PAYLOAD_JSON);
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);
        when(hashService.sha256(rawContent)).thenReturn(HASH);
        when(transactionRepository.existsByHash(HASH)).thenReturn(false);
        when(transactionRepository.existsByPublicKeyAndNonce(PUBLIC_KEY, NONCE)).thenReturn(false);
        when(signatureService.verify(rawContent, SIGNATURE, PUBLIC_KEY)).thenReturn(true);
        when(transactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> {
            LedgerTransaction transaction = invocation.getArgument(0);
            transaction.id = UUID.randomUUID();
            transaction.createdAt = Instant.parse("2026-05-11T18:00:00Z");
            return transaction;
        });

        var response = transactionService.create(request);

        ArgumentCaptor<LedgerTransaction> captor = ArgumentCaptor.forClass(LedgerTransaction.class);
        verify(transactionRepository).save(captor.capture());
        LedgerTransaction saved = captor.getValue();

        assertThat(saved.hash).isEqualTo(HASH);
        assertThat(saved.type).isEqualTo(TYPE);
        assertThat(saved.payload).isEqualTo(PAYLOAD_JSON);
        assertThat(saved.publicKey).isEqualTo(PUBLIC_KEY);
        assertThat(saved.signature).isEqualTo(SIGNATURE);
        assertThat(saved.nonce).isEqualTo(NONCE);
        assertThat(saved.status).isEqualTo(TransactionStatus.PENDING);
        assertThat(response.hash()).isEqualTo(HASH);
        assertThat(response.status()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void createRejectsDuplicatedHash() throws Exception {
        Map<String, String> payload = Map.of("processingId", "process-1");
        CreateTransactionDto request = new CreateTransactionDto(TYPE, payload, PUBLIC_KEY, SIGNATURE, NONCE);
        String rawContent = TYPE + PAYLOAD_JSON + PUBLIC_KEY + NONCE;

        when(objectMapper.writeValueAsString(payload)).thenReturn(PAYLOAD_JSON);
        when(hashService.sha256(rawContent)).thenReturn(HASH);
        when(transactionRepository.existsByHash(HASH)).thenReturn(true);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Transação já existente");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicatedNonceForPublicKey() throws Exception {
        Map<String, String> payload = Map.of("processingId", "process-1");
        CreateTransactionDto request = new CreateTransactionDto(TYPE, payload, PUBLIC_KEY, SIGNATURE, NONCE);
        String rawContent = TYPE + PAYLOAD_JSON + PUBLIC_KEY + NONCE;

        when(objectMapper.writeValueAsString(payload)).thenReturn(PAYLOAD_JSON);
        when(hashService.sha256(rawContent)).thenReturn(HASH);
        when(transactionRepository.existsByHash(HASH)).thenReturn(false);
        when(transactionRepository.existsByPublicKeyAndNonce(PUBLIC_KEY, NONCE)).thenReturn(true);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Nonce já utilizado para esta publicKey");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidSignature() throws Exception {
        Map<String, String> payload = Map.of("processingId", "process-1");
        CreateTransactionDto request = new CreateTransactionDto(TYPE, payload, PUBLIC_KEY, SIGNATURE, NONCE);
        String rawContent = TYPE + PAYLOAD_JSON + PUBLIC_KEY + NONCE;

        when(objectMapper.writeValueAsString(payload)).thenReturn(PAYLOAD_JSON);
        when(hashService.sha256(rawContent)).thenReturn(HASH);
        when(transactionRepository.existsByHash(HASH)).thenReturn(false);
        when(transactionRepository.existsByPublicKeyAndNonce(PUBLIC_KEY, NONCE)).thenReturn(false);
        when(signatureService.verify(rawContent, SIGNATURE, PUBLIC_KEY)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InvalidSignatureException.class)
                .hasMessage("Assinatura inválida");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void findByHashReturnsTransaction() throws Exception {
        Map<String, String> payload = Map.of("processingId", "process-1");
        LedgerTransaction transaction = transaction(HASH);

        when(transactionRepository.findByHash(HASH)).thenReturn(Optional.of(transaction));
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);

        var response = transactionService.findByHash(HASH);

        assertThat(response.hash()).isEqualTo(HASH);
        assertThat(response.payload()).isEqualTo(payload);
    }

    @Test
    void findByHashRejectsInvalidHashFormat() {
        assertThatThrownBy(() -> transactionService.findByHash("abc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Hash inválido");
    }

    @Test
    void findByHashThrowsNotFoundWhenTransactionDoesNotExist() {
        when(transactionRepository.findByHash(HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findByHash(HASH))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transação não encontrada");
    }

    @Test
    void pageTransactionsRejectsInvalidSize() {
        assertThatThrownBy(() -> transactionService.pageTransactions(0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Parâmetro size inválido");
    }

    @Test
    void pageTransactionsMapsRepositoryPage() throws Exception {
        Map<String, String> payload = Map.of("processingId", "process-1");
        LedgerTransaction transaction = transaction(HASH);

        when(transactionRepository.pageTransactions(any())).thenReturn(new PageImpl<>(java.util.List.of(transaction)));
        when(objectMapper.readValue(PAYLOAD_JSON, Object.class)).thenReturn(payload);

        var page = transactionService.pageTransactions(0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().hash()).isEqualTo(HASH);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    private LedgerTransaction transaction(String hash) {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.id = UUID.randomUUID();
        transaction.hash = hash;
        transaction.type = TYPE;
        transaction.payload = PAYLOAD_JSON;
        transaction.publicKey = PUBLIC_KEY;
        transaction.signature = SIGNATURE;
        transaction.nonce = NONCE;
        transaction.status = TransactionStatus.MINED;
        transaction.createdAt = Instant.parse("2026-05-11T18:00:00Z");
        transaction.minedAt = Instant.parse("2026-05-11T18:00:30Z");
        transaction.blockId = UUID.randomUUID();
        transaction.blockTransactionIndex = 0;
        return transaction;
    }
}
