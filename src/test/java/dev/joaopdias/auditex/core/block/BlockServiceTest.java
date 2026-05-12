package dev.joaopdias.auditex.core.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import dev.joaopdias.auditex.core.block.dto.CreateBlockDto;
import dev.joaopdias.auditex.core.block.dto.MinedBlockDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.transaction.TransactionService;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.exceptions.BadRequestException;
import dev.joaopdias.auditex.shared.exceptions.ConflictException;
import dev.joaopdias.auditex.shared.exceptions.ResourceNotFoundException;
import dev.joaopdias.auditex.shared.services.HashService;
import dev.joaopdias.auditex.shared.services.SignatureService;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    private static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);
    private static final String TYPE = "BILLING_FILE_RECEIVED";
    private static final String PAYLOAD = "{\"processingId\":\"process-1\"}";
    private static final String PUBLIC_KEY = "public-key";
    private static final String SIGNATURE = "signature";
    private static final String NONCE = "nonce-1";

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private SignatureService signatureService;

    private final HashService hashService = new HashService();
    private BlockService blockService;

    @BeforeEach
    void setUp() {
        blockService = new BlockService();
        ReflectionTestUtils.setField(blockService, "blockRepository", blockRepository);
        ReflectionTestUtils.setField(blockService, "transactionService", transactionService);
        ReflectionTestUtils.setField(blockService, "hashService", hashService);
        ReflectionTestUtils.setField(blockService, "signatureService", signatureService);
    }

    @Test
    void createStoresBlockWhenHashAndIndexAreUnique() {
        CreateBlockDto request = new CreateBlockDto(0, "a".repeat(64), GENESIS_PREVIOUS_HASH, "b".repeat(64), 1L, 4);

        when(blockRepository.existsByHash(request.hash())).thenReturn(false);
        when(blockRepository.existsByIndex(request.index())).thenReturn(false);
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> {
            Block block = invocation.getArgument(0);
            block.setId(UUID.randomUUID());
            block.setCreatedAt(Instant.parse("2026-05-11T18:00:00Z"));
            return block;
        });
        when(transactionService.countByBlockId(any(UUID.class))).thenReturn(0L);

        var response = blockService.create(request);

        assertThat(response.index()).isZero();
        assertThat(response.hash()).isEqualTo(request.hash());
        assertThat(response.transactionsCount()).isZero();
    }

    @Test
    void createRejectsDuplicatedHash() {
        CreateBlockDto request = new CreateBlockDto(0, "a".repeat(64), GENESIS_PREVIOUS_HASH, "b".repeat(64), 1L, 4);

        when(blockRepository.existsByHash(request.hash())).thenReturn(true);

        assertThatThrownBy(() -> blockService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Bloco já existente");

        verify(blockRepository, never()).save(any());
    }

    @Test
    void saveMinedBlockRejectsDuplicatedIndex() {
        MinedBlockDto request = new MinedBlockDto(1, "a".repeat(64), GENESIS_PREVIOUS_HASH, "b".repeat(64), 1L, 4);

        when(blockRepository.existsByHash(request.hash())).thenReturn(false);
        when(blockRepository.existsByIndex(request.index())).thenReturn(true);

        assertThatThrownBy(() -> blockService.saveMinedBlock(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Índice já existente");
    }

    @Test
    void findByHashRejectsInvalidHash() {
        assertThatThrownBy(() -> blockService.findByHash("abc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Hash inválido");
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();

        when(blockRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Bloco não encontrado");
    }

    @Test
    void pageBlocksRejectsInvalidPage() {
        assertThatThrownBy(() -> blockService.pageBlocks(-1, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Parâmetro page inválido");
    }

    @Test
    void pageBlocksMapsRepositoryPage() {
        Block block = validContext().block();

        when(blockRepository.pageBlocks(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(block)));
        when(transactionService.countByBlockId(block.getId())).thenReturn(3L);

        var page = blockService.pageBlocks(0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().transactionsCount()).isEqualTo(3);
    }

    @Test
    void validateReturnsValidForIntactBlockchain() {
        ValidationContext context = validContext();
        stubValidation(context, true, 1L);

        var response = blockService.validate();

        assertThat(response.valid()).isTrue();
        assertThat(response.blocksChecked()).isEqualTo(1);
        assertThat(response.transactionsChecked()).isEqualTo(1);
        assertThat(response.reason()).isNull();
    }

    @Test
    void validateDetectsInvalidBlockIndex() {
        var response = validateBlockMutation(block -> block.setIndex(1));

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_BLOCK_INDEX");
        assertThat(response.transactionsChecked()).isZero();
    }

    @Test
    void validateDetectsInvalidPreviousHash() {
        var response = validateBlockMutation(block -> block.setPreviousHash("f".repeat(64)));

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_PREVIOUS_HASH");
        assertThat(response.transactionsChecked()).isZero();
    }

    @Test
    void validateDetectsInvalidMerkleRoot() {
        var response = validateBlockMutation(block -> {
            block.setMerkleRoot("f".repeat(64));
            block.setHash(calculateBlockHash(block));
        });

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_MERKLE_ROOT");
        assertThat(response.transactionsChecked()).isEqualTo(1);
    }

    @Test
    void validateDetectsInvalidBlockHash() {
        var response = validateBlockMutation(block -> block.setHash("f".repeat(64)));

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_BLOCK_HASH");
    }

    @Test
    void validateDetectsInvalidProofOfWork() {
        ValidationContext context = validContextWithInvalidProofOfWork();
        stubValidation(context, true, 1L);

        var response = blockService.validate();

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_PROOF_OF_WORK");
    }

    @Test
    void validateDetectsInvalidTransactionHash() {
        var response = validateTransactionMutation(transaction -> transaction.setHash("f".repeat(64)), true, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_TRANSACTION_HASH");
        assertThat(response.transactionsChecked()).isEqualTo(1);
        assertThat(response.brokenAtTransaction()).isNotNull();
    }

    @Test
    void validateDetectsInvalidTransactionSignature() {
        var response = validateTransactionMutation(transaction -> {
        }, false, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_TRANSACTION_SIGNATURE");
    }

    @Test
    void validateDetectsInvalidTransactionStatus() {
        var response = validateTransactionMutation(transaction -> transaction.setStatus(TransactionStatus.PENDING), true, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_TRANSACTION_STATUS");
    }

    @Test
    void validateDetectsMissingTransactionBlockId() {
        var response = validateTransactionMutation(transaction -> transaction.setBlockId(null), true, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("MISSING_TRANSACTION_BLOCK_ID");
    }

    @Test
    void validateDetectsInvalidTransactionBlockId() {
        var response = validateTransactionMutation(transaction -> transaction.setBlockId(UUID.randomUUID()), true, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_TRANSACTION_BLOCK_ID");
    }

    @Test
    void validateDetectsMissingTransactionMinedAt() {
        var response = validateTransactionMutation(transaction -> transaction.setMinedAt(null), true, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("MISSING_TRANSACTION_MINED_AT");
    }

    @Test
    void validateDetectsMissingTransactionBlockIndex() {
        var response = validateTransactionMutation(transaction -> transaction.setBlockTransactionIndex(null), true, 1L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("MISSING_TRANSACTION_BLOCK_INDEX");
    }

    @Test
    void validateDetectsDuplicatedTransactionNonce() {
        var response = validateTransactionMutation(transaction -> {
        }, true, 2L);

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("DUPLICATED_TRANSACTION_NONCE");
    }

    private dev.joaopdias.auditex.core.block.dto.ValidateResponseDto validateBlockMutation(Consumer<Block> mutation) {
        ValidationContext context = validContext();
        mutation.accept(context.block());
        stubValidation(context, true, 1L);
        return blockService.validate();
    }

    private dev.joaopdias.auditex.core.block.dto.ValidateResponseDto validateTransactionMutation(
            Consumer<LedgerTransaction> mutation,
            boolean validSignature,
            long nonceCount) {
        ValidationContext context = validContext();
        mutation.accept(context.transaction());
        stubValidation(context, validSignature, nonceCount);
        return blockService.validate();
    }

    private void stubValidation(ValidationContext context, boolean validSignature, long nonceCount) {
        lenient().when(blockRepository.findByIndexGreaterThanOrderByIndexAsc(eq(-1), any(Pageable.class)))
                .thenReturn(List.of(context.block()));
        lenient().when(blockRepository.findByIndexGreaterThanOrderByIndexAsc(eq(context.block().getIndex()), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(transactionService.findByBlockIdOrderByBlockTransactionIndexAsc(eq(context.block().getId()), any(Pageable.class)))
                .thenReturn(List.of(context.transaction()))
                .thenReturn(List.of());
        lenient().when(signatureService.verify(any(), eq(SIGNATURE), eq(PUBLIC_KEY))).thenReturn(validSignature);
        lenient().when(transactionService.countByPublicKeyAndNonce(PUBLIC_KEY, NONCE)).thenReturn(nonceCount);
    }

    private ValidationContext validContext() {
        UUID blockId = UUID.randomUUID();
        LedgerTransaction transaction = validTransaction(blockId);
        Block block = validBlock(blockId, transaction.getHash(), 0);
        return new ValidationContext(block, transaction);
    }

    private ValidationContext validContextWithInvalidProofOfWork() {
        UUID blockId = UUID.randomUUID();
        LedgerTransaction transaction = validTransaction(blockId);
        Block block = validBlock(blockId, transaction.getHash(), 1);

        long nonce = 0L;
        while (true) {
            block.setNonce(nonce);
            block.setHash(calculateBlockHash(block));
            if (!block.getHash().startsWith("0")) return new ValidationContext(block, transaction);
            nonce++;
        }
    }

    private LedgerTransaction validTransaction(UUID blockId) {
        String transactionHash = hashService.sha256(TYPE + PAYLOAD + PUBLIC_KEY + NONCE);
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setHash(transactionHash);
        transaction.setType(TYPE);
        transaction.setPayload(PAYLOAD);
        transaction.setPublicKey(PUBLIC_KEY);
        transaction.setSignature(SIGNATURE);
        transaction.setNonce(NONCE);
        transaction.setStatus(TransactionStatus.MINED);
        transaction.setBlockId(blockId);
        transaction.setMinedAt(Instant.parse("2026-05-11T18:00:00Z"));
        transaction.setBlockTransactionIndex(0);
        return transaction;
    }

    private Block validBlock(UUID blockId, String merkleRoot, int difficulty) {
        Block block = new Block();
        block.setId(blockId);
        block.setIndex(0);
        block.setPreviousHash(GENESIS_PREVIOUS_HASH);
        block.setMerkleRoot(merkleRoot);
        block.setNonce(0L);
        block.setDifficulty(difficulty);
        block.setCreatedAt(Instant.parse("2026-05-11T18:00:01Z"));
        block.setMinedAt(Instant.parse("2026-05-11T18:00:02Z"));
        block.setHash(calculateBlockHash(block));
        return block;
    }

    private String calculateBlockHash(Block block) {
        return hashService.sha256(
                block.getIndex()
                        + block.getPreviousHash()
                        + block.getMerkleRoot()
                        + block.getNonce()
                        + block.getDifficulty());
    }

    private record ValidationContext(Block block, LedgerTransaction transaction) {
    }
}
