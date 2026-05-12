package dev.joaopdias.auditex.shared.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import dev.joaopdias.auditex.shared.exceptions.InvalidSignatureException;

class SignatureServiceTest {

    private final SignatureService signatureService = new SignatureService();

    @Test
    void signAndVerifyRoundTrip() throws Exception {
        KeyPair keyPair = keyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String signature = signatureService.sign("auditex-ledger", privateKey);

        assertThat(signatureService.verify("auditex-ledger", signature, publicKey)).isTrue();
        assertThat(signatureService.verify("changed-content", signature, publicKey)).isFalse();
    }

    @Test
    void verifyReturnsFalseForMalformedPublicKey() {
        boolean valid = signatureService.verify("content", "signature", "not-a-key");

        assertThat(valid).isFalse();
    }

    @Test
    void signRejectsMalformedPrivateKey() {
        assertThatThrownBy(() -> signatureService.sign("content", "not-a-key"))
                .isInstanceOf(InvalidSignatureException.class)
                .hasMessage("Não foi possível assinar conteúdo");
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
