package dev.joaopdias.auditex.core.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.joaopdias.auditex.core.transaction.dto.TransactionResponseDto;
import dev.joaopdias.auditex.core.transaction.enums.TransactionStatus;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;
import dev.joaopdias.auditex.shared.exceptions.BadRequestException;
import dev.joaopdias.auditex.shared.exceptions.GlobalExceptionHandler;
import dev.joaopdias.auditex.shared.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController();
        ReflectionTestUtils.setField(controller, "transactionService", transactionService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedTransaction() throws Exception {
        when(transactionService.create(any())).thenReturn(transactionResponse());

        mockMvc.perform(post("/transaction")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "BILLING_FILE_RECEIVED",
                                  "payload": {"processingId": "process-1"},
                                  "publicKey": "public-key",
                                  "signature": "signature",
                                  "nonce": "nonce-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hash").value("a".repeat(64)))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void findByHashReturnsNotFoundErrorResponse() throws Exception {
        when(transactionService.findByHash("a".repeat(64)))
                .thenThrow(new ResourceNotFoundException("Transação não encontrada"));

        mockMvc.perform(get("/transaction/hash/{hash}", "a".repeat(64)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Transação não encontrada"))
                .andExpect(jsonPath("$.path").value("/transaction/hash/" + "a".repeat(64)));
    }

    @Test
    void pageReturnsBadRequestForInvalidPagination() throws Exception {
        when(transactionService.pageTransactions(eq(-1), eq(20)))
                .thenThrow(new BadRequestException("Parâmetro page inválido"));

        mockMvc.perform(get("/transaction").param("page", "-1").param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro page inválido"));
    }

    @Test
    void pageByTypeReturnsPagedTransactions() throws Exception {
        PageResponseDto<TransactionResponseDto> page = new PageResponseDto<>(
                java.util.List.of(transactionResponse()),
                0,
                20,
                1,
                1,
                true,
                true);

        when(transactionService.pageByType("BILLING_FILE_RECEIVED", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/transaction/type/{type}", "BILLING_FILE_RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("BILLING_FILE_RECEIVED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private TransactionResponseDto transactionResponse() {
        return new TransactionResponseDto(
                UUID.randomUUID(),
                "a".repeat(64),
                "BILLING_FILE_RECEIVED",
                Map.of("processingId", "process-1"),
                "public-key",
                "signature",
                TransactionStatus.PENDING,
                "nonce-1",
                null,
                null,
                null,
                null);
    }
}
