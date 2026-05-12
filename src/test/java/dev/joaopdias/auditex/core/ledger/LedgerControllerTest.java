package dev.joaopdias.auditex.core.ledger;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.ledger.dto.LedgerStatusResponseDto;
import dev.joaopdias.auditex.shared.exceptions.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class LedgerControllerTest {

    @Mock
    private LedgerService ledgerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LedgerController controller = new LedgerController();
        ReflectionTestUtils.setField(controller, "ledgerService", ledgerService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void statusReturnsLedgerStatus() throws Exception {
        when(ledgerService.status()).thenReturn(new LedgerStatusResponseDto(true, 12, 1, 842, 11, "a".repeat(64), null));

        mockMvc.perform(get("/ledger/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.blocksCount").value(12))
                .andExpect(jsonPath("$.minedTransactions").value(842));
    }

    @Test
    void validateReturnsBlockchainValidation() throws Exception {
        when(ledgerService.validate()).thenReturn(new ValidateResponseDto(false, 1, 2, null, null, "INVALID_BLOCK_HASH"));

        mockMvc.perform(get("/ledger/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("INVALID_BLOCK_HASH"));
    }
}
