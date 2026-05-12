package dev.joaopdias.auditex.core.block;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.joaopdias.auditex.core.block.dto.BlockResponseDto;
import dev.joaopdias.auditex.core.block.dto.BlockTransactionsResponseDto;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.transaction.dto.TransactionResponseDto;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;
import dev.joaopdias.auditex.shared.exceptions.GlobalExceptionHandler;
import dev.joaopdias.auditex.shared.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest {

    @Mock
    private BlockService blockService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BlockController controller = new BlockController();
        ReflectionTestUtils.setField(controller, "blockService", blockService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validateReturnsValidationResult() throws Exception {
        when(blockService.validate()).thenReturn(new ValidateResponseDto(true, 2, 4, null, null, null));

        mockMvc.perform(get("/block/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.blocksChecked").value(2))
                .andExpect(jsonPath("$.transactionsChecked").value(4));
    }

    @Test
    void latestReturnsNotFoundWhenLedgerHasNoBlocks() throws Exception {
        when(blockService.findLastBlock()).thenThrow(new ResourceNotFoundException("Nenhum bloco encontrado"));

        mockMvc.perform(get("/block/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nenhum bloco encontrado"));
    }

    @Test
    void pageTransactionsReturnsBlockAndPagedTransactions() throws Exception {
        UUID blockId = UUID.randomUUID();
        BlockTransactionsResponseDto response = new BlockTransactionsResponseDto(
                blockResponse(blockId),
                new PageResponseDto<TransactionResponseDto>(
                        java.util.List.of(),
                        0,
                        50,
                        0,
                        0,
                        true,
                        true));

        when(blockService.pageTransactions(eq(blockId), eq(0), eq(50))).thenReturn(response);

        mockMvc.perform(get("/block/{id}/transaction", blockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.block.id").value(blockId.toString()))
                .andExpect(jsonPath("$.transactions.size").value(50));
    }

    private BlockResponseDto blockResponse(UUID id) {
        return new BlockResponseDto(
                id,
                0,
                "a".repeat(64),
                "0".repeat(64),
                "b".repeat(64),
                1L,
                4,
                null,
                null,
                0);
    }
}
