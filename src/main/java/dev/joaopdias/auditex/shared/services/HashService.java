package dev.joaopdias.auditex.shared.services;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import dev.joaopdias.auditex.shared.exceptions.InternalApplicationException;

@Service
public class HashService {
    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : bytes)
                result.append(String.format("%02x", b));

            return result.toString();
        } catch (Exception exception) {
            throw new InternalApplicationException("Não foi possível calcular hash", exception);
        }
    }
}
