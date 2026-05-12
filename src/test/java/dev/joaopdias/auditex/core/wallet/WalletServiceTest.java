package dev.joaopdias.auditex.core.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.joaopdias.auditex.core.wallet.dto.CreateWalletDto;
import dev.joaopdias.auditex.core.wallet.entities.Wallet;
import dev.joaopdias.auditex.shared.services.HashService;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private HashService hashService;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService();
        ReflectionTestUtils.setField(walletService, "walletRepository", walletRepository);
        ReflectionTestUtils.setField(walletService, "hashService", hashService);
    }

    @Test
    void createGeneratesKeyPairAddressAndStoresWallet() {
        when(hashService.sha256(anyString())).thenReturn("a".repeat(64));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(UUID.randomUUID());
            wallet.setCreatedAt(Instant.parse("2026-05-11T18:00:00Z"));
            return wallet;
        });

        var response = walletService.create(new CreateWalletDto("Finance Ops"));

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());

        assertThat(captor.getValue().getOwnerName()).isEqualTo("Finance Ops");
        assertThat(captor.getValue().getAddress()).isEqualTo("AX-" + "a".repeat(32));
        assertThat(response.address()).isEqualTo("AX-" + "a".repeat(32));
        assertThat(response.publicKey()).isNotBlank();
        assertThat(response.privateKey()).isNotBlank();
    }
}
