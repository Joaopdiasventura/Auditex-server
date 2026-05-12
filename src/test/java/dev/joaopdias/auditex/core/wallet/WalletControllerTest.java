package dev.joaopdias.auditex.core.wallet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import dev.joaopdias.auditex.core.wallet.dto.ReturnWalletDto;
import dev.joaopdias.auditex.shared.exceptions.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WalletController controller = new WalletController();
        ReflectionTestUtils.setField(controller, "walletService", walletService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedWallet() throws Exception {
        when(walletService.create(any())).thenReturn(new ReturnWalletDto(
                UUID.randomUUID(),
                "Finance Ops",
                "AX-" + "a".repeat(32),
                "public-key",
                "private-key",
                null));

        mockMvc.perform(post("/wallet")
                        .contentType("application/json")
                        .content("{\"ownerName\":\"Finance Ops\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Finance Ops"))
                .andExpect(jsonPath("$.address").value("AX-" + "a".repeat(32)));
    }
}
