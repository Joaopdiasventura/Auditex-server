package dev.joaopdias.auditex.shared.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashServiceTest {

    private final HashService hashService = new HashService();

    @Test
    void sha256ReturnsExpectedHexDigest() {
        String hash = hashService.sha256("abc");

        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
