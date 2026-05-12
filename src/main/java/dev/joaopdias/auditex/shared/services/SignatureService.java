package dev.joaopdias.auditex.shared.services;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Service;

import dev.joaopdias.auditex.shared.exceptions.InvalidSignatureException;

@Service
public class SignatureService {

    public String sign(String content, String privateKey) {
        try {
            PrivateKey key = this.toPrivateKey(privateKey);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) {
            throw new InvalidSignatureException("Não foi possível assinar conteúdo", exception);
        }
    }

    public boolean verify(String content, String signatureValue, String publicKey) {
        try {
            PublicKey key = this.toPublicKey(publicKey);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureValue);

            return signature.verify(signatureBytes);
        } catch (Exception exception) {
            return false;
        }
    }

    private PublicKey toPublicKey(String publicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);
        } catch (Exception exception) {
            throw new InvalidSignatureException("PublicKey inválida", exception);
        }
    }

    private PrivateKey toPrivateKey(String privateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new InvalidSignatureException("PrivateKey inválida", exception);
        }
    }
}
