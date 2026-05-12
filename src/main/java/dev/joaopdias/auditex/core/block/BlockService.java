package dev.joaopdias.auditex.core.block;

import java.time.Instant;
import java.util.ArrayList;
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

import dev.joaopdias.auditex.core.block.dto.BlockResponseDto;
import dev.joaopdias.auditex.core.block.dto.BlockTransactionsResponseDto;
import dev.joaopdias.auditex.core.block.dto.CreateBlockDto;
import dev.joaopdias.auditex.core.block.dto.MinedBlockDto;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.core.transaction.TransactionService;
import dev.joaopdias.auditex.core.transaction.entities.LedgerTransaction;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;
import dev.joaopdias.auditex.shared.exceptions.BadRequestException;
import dev.joaopdias.auditex.shared.exceptions.ConflictException;
import dev.joaopdias.auditex.shared.exceptions.ResourceNotFoundException;
import dev.joaopdias.auditex.shared.services.HashService;
import dev.joaopdias.auditex.shared.services.SignatureService;
import jakarta.transaction.Transactional;

@Service
public class BlockService {

    private static final String GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern SHA_256_HASH_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private HashService hashService;

    @Autowired
    private SignatureService signatureService;

    private final ObjectMapper canonicalObjectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    @Transactional
    public BlockResponseDto create(CreateBlockDto createBlockDto) {
        if (blockRepository.existsByHash(createBlockDto.hash()))
            throw new ConflictException("Bloco já existente");

        if (blockRepository.existsByIndex(createBlockDto.index()))
            throw new ConflictException("Índice já existente");

        Block block = new Block();

        block.setIndex(createBlockDto.index());
        block.setHash(createBlockDto.hash());
        block.setPreviousHash(createBlockDto.previousHash());
        block.setMerkleRoot(createBlockDto.merkleRoot());
        block.setNonce(createBlockDto.nonce());
        block.setDifficulty(createBlockDto.difficulty());
        block.setMinedAt(Instant.now());

        Block saved = blockRepository.save(block);

        return toResponse(saved);
    }

    public ValidateResponseDto validate() {
        int blockPageSize = 100;
        int transactionPageSize = 100;
        int blocksChecked = 0;
        int transactionsChecked = 0;
        int lastIndex = -1;

        String expectedPreviousHash = GENESIS_PREVIOUS_HASH;

        while (true) {
            List<Block> blocks = blockRepository.findByIndexGreaterThanOrderByIndexAsc(
                    lastIndex,
                    PageRequest.of(0, blockPageSize));

            if (blocks.isEmpty()) {
                return new ValidateResponseDto(
                        true,
                        blocksChecked,
                        transactionsChecked,
                        null,
                        null,
                        null);
            }

            for (Block block : blocks) {
                if (!block.getIndex().equals(lastIndex + 1))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            null,
                            "INVALID_BLOCK_INDEX");


                if (!block.getPreviousHash().equals(expectedPreviousHash))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            null,
                            "INVALID_PREVIOUS_HASH");


                MerkleValidationResult merkleValidationResult = calculateMerkleRootByBlockId(
                        block,
                        block.getId(),
                        transactionPageSize,
                        blocksChecked,
                        transactionsChecked);

                transactionsChecked += merkleValidationResult.transactionsChecked();

                if (merkleValidationResult.invalidResponse() != null)
                    return merkleValidationResult.invalidResponse();

                if (!block.getMerkleRoot().equals(merkleValidationResult.merkleRoot()))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            null,
                            "INVALID_MERKLE_ROOT");


                String recalculatedHash = calculateBlockHash(block);

                if (!block.getHash().equals(recalculatedHash))
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            null,
                            "INVALID_BLOCK_HASH");


                String target = "0".repeat(block.getDifficulty());

                if (!block.getHash().startsWith(target)) 
                    return new ValidateResponseDto(
                            false,
                            blocksChecked,
                            transactionsChecked,
                            block.getId(),
                            null,
                            "INVALID_PROOF_OF_WORK");

                blocksChecked++;
                expectedPreviousHash = block.getHash();
                lastIndex = block.getIndex();
            }
        }
    }

    @Transactional
    public Block saveMinedBlock(MinedBlockDto minedBlockDto) {
        
        if (blockRepository.existsByHash(minedBlockDto.hash()))
            throw new ConflictException("Bloco já existente");

        if (blockRepository.existsByIndex(minedBlockDto.index()))
            throw new ConflictException("Índice já existente");

        Block block = new Block();

        block.setIndex(minedBlockDto.index());
        block.setHash(minedBlockDto.hash());
        block.setPreviousHash(minedBlockDto.previousHash());
        block.setMerkleRoot(minedBlockDto.merkleRoot());
        block.setNonce(minedBlockDto.nonce());
        block.setDifficulty(minedBlockDto.difficulty());
        block.setMinedAt(Instant.now());

        return blockRepository.save(block);
    }

    public BlockResponseDto findByHash(String hash) {
        validateHash(hash);

        Block block = blockRepository.findByHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("Bloco não encontrado"));
        return this.toResponse(block);
    }

    public BlockResponseDto findById(UUID id) {
        Block block = blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloco não encontrado"));
        return this.toResponse(block);
    }

    public BlockResponseDto findByIndex(Integer index) {
        Block block = blockRepository.findByIndex(index)
                .orElseThrow(() -> new ResourceNotFoundException("Bloco não encontrado"));
        return this.toResponse(block);
    }

    public PageResponseDto<BlockResponseDto> pageBlocks(Integer page, Integer size) {
        Page<BlockResponseDto> blocks = blockRepository
                .pageBlocks(pageRequest(page, size))
                .map(this::toResponse);

        return PageResponseDto.from(blocks);
    }

    public BlockTransactionsResponseDto pageTransactions(UUID blockId, Integer page, Integer size) {
        BlockResponseDto block = findById(blockId);

        return new BlockTransactionsResponseDto(
                block,
                transactionService.pageByBlockId(blockId, page, size));
    }

    public Block findLastBlockEntity() {
        return blockRepository.findTopByOrderByIndexDesc().orElse(null);
    }

    public BlockResponseDto findLastBlock() {
        Block block = this.findLastBlockEntity();

        if (block == null) throw new ResourceNotFoundException("Nenhum bloco encontrado");
        return this.toResponse(block);
    }

    public Integer getNextIndex() {
        Block lastBlock = this.findLastBlockEntity();

        if (lastBlock == null) return 0;
        return lastBlock.getIndex() + 1;
    }

    public String getPreviousHash() {
        Block lastBlock = this.findLastBlockEntity();

        if (lastBlock == null) return GENESIS_PREVIOUS_HASH;
        return lastBlock.getHash();
    }

    private String calculateBlockHash(Block block) {
        String content = block.getIndex()
                + block.getPreviousHash()
                + block.getMerkleRoot()
                + block.getNonce()
                + block.getDifficulty();

        return hashService.sha256(content);
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

    private MerkleValidationResult calculateMerkleRootByBlockId(
            Block block,
            UUID blockId,
            int pageSize,
            int blocksChecked,
            int previousTransactionsChecked) {
        List<String> transactionHashes = new ArrayList<>();
        int page = 0;
        int transactionsChecked = 0;

        while (true) {
            List<LedgerTransaction> transactions = transactionService.findByBlockIdOrderByBlockTransactionIndexAsc(
                blockId,
                PageRequest.of(page, pageSize));

            if (transactions.isEmpty()) break;

            for (LedgerTransaction transaction : transactions) {
                transactionsChecked++;

                ValidateResponseDto invalidResponse = validateTransaction(
                        block,
                        transaction,
                        blocksChecked,
                        previousTransactionsChecked + transactionsChecked);

                if (invalidResponse != null)
                    return new MerkleValidationResult(
                            null,
                            transactionsChecked,
                            invalidResponse);

                transactionHashes.add(transaction.getHash());
            }

            page++;
        }

        String merkleRoot = calculateMerkleRootFromHashes(transactionHashes);

        return new MerkleValidationResult(
                merkleRoot,
                transactionsChecked,
                null);
    }

    private ValidateResponseDto validateTransaction(
            Block block,
            LedgerTransaction transaction,
            int blocksChecked,
            int transactionsChecked) {

        if (transaction.getStatus() != TransactionStatus.MINED)
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "INVALID_TRANSACTION_STATUS");

        if (transaction.getBlockId() == null)
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "MISSING_TRANSACTION_BLOCK_ID");

        if (!transaction.getBlockId().equals(block.getId()))
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "INVALID_TRANSACTION_BLOCK_ID");

        if (transaction.getMinedAt() == null)
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "MISSING_TRANSACTION_MINED_AT");

        if (transaction.getBlockTransactionIndex() == null)
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "MISSING_TRANSACTION_BLOCK_INDEX");

        String rawContent;

        try {
            rawContent = transaction.getType()
                    + canonicalizePayload(transaction.getPayload())
                    + transaction.getPublicKey()
                    + transaction.getNonce();
        } catch (JsonProcessingException exception) {
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "INVALID_TRANSACTION_HASH");
        }

        String recalculatedHash = hashService.sha256(rawContent);

        if (!recalculatedHash.equals(transaction.getHash()))
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "INVALID_TRANSACTION_HASH");

        boolean validSignature = signatureService.verify(
                rawContent,
                transaction.getSignature(),
                transaction.getPublicKey());

        if (!validSignature)
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "INVALID_TRANSACTION_SIGNATURE");

        if (transactionService.countByPublicKeyAndNonce(
                transaction.getPublicKey(),
                transaction.getNonce()) > 1)
            return invalidTransactionResponse(
                    block,
                    transaction,
                    blocksChecked,
                    transactionsChecked,
                    "DUPLICATED_TRANSACTION_NONCE");

        return null;
    }

    private String canonicalizePayload(String payload) throws JsonProcessingException {
        Object normalizedPayload = canonicalObjectMapper.readValue(payload, Object.class);

        return canonicalObjectMapper.writeValueAsString(normalizedPayload);
    }

    private ValidateResponseDto invalidTransactionResponse(
            Block block,
            LedgerTransaction transaction,
            int blocksChecked,
            int transactionsChecked,
            String reason) {

        return new ValidateResponseDto(
                false,
                blocksChecked,
                transactionsChecked,
                block.getId(),
                transaction.getId(),
                reason);
    }

    private String calculateMerkleRootFromHashes(List<String> hashes) {
        if (hashes.isEmpty()) return hashService.sha256("");

        if (hashes.size() == 1) return hashes.get(0);

        List<String> nextLevel = new ArrayList<>();

        for (int index = 0; index < hashes.size(); index += 2) {
            String left = hashes.get(index);
            String right = index + 1 < hashes.size() ? hashes.get(index + 1) : left;

            nextLevel.add(hashService.sha256(left + right));
        }

        return calculateMerkleRootFromHashes(nextLevel);
    }

    private record MerkleValidationResult(
            String merkleRoot,
            int transactionsChecked,
            ValidateResponseDto invalidResponse) {
    }

    private BlockResponseDto toResponse(Block block) {
        return new BlockResponseDto(
                block.getId(),
                block.getIndex(),
                block.getHash(),
                block.getPreviousHash(),
                block.getMerkleRoot(),
                block.getNonce(),
                block.getDifficulty(),
                block.getCreatedAt(),
                block.getMinedAt(),
                transactionService.countByBlockId(block.getId()));
    }
}
