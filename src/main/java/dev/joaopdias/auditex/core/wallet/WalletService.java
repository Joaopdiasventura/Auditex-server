package dev.joaopdias.auditex.core.wallet;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.core.wallet.dto.CreateWalletDto;
import dev.joaopdias.auditex.core.wallet.dto.GeneratedKeyPairDto;
import dev.joaopdias.auditex.core.wallet.dto.ReturnWalletDto;
import dev.joaopdias.auditex.core.wallet.entities.Wallet;
import dev.joaopdias.auditex.shared.services.HashService;
import jakarta.transaction.Transactional;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private HashService hashService;

    @Transactional
    public ReturnWalletDto create(CreateWalletDto createWalletDto) {

        GeneratedKeyPairDto keyPair = this.generateKeyPair();

        String publicKey = keyPair.publicKey();
        String privateKey = keyPair.privateKey();

        String address = this.generateAddress(publicKey);

        Wallet wallet = new Wallet();

        wallet.setOwnerName(createWalletDto.ownerName());
        wallet.setAddress(address);
        wallet.setPublicKey(publicKey);

        walletRepository.save(wallet);

        return new ReturnWalletDto(wallet.getId(),
                wallet.getOwnerName(),
                address,
                publicKey,
                privateKey,
                wallet.getCreatedAt());

    }

    private GeneratedKeyPairDto generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            KeyPair keyPair = generator.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            return new GeneratedKeyPairDto(publicKey, privateKey);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate key pair", exception);
        }
    }

    private String generateAddress(String publicKey) {
        String hash = hashService.sha256(publicKey);
        return "AX-" + hash.substring(0, 32);
    }

}
