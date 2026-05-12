package dev.joaopdias.auditex.shared.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleConflictReturnsConflictErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/transaction");

        var response = handler.handleConflict(new ConflictException("Transação já existente"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).isEqualTo("Transação já existente");
        assertThat(response.getBody().path()).isEqualTo("/transaction");
    }

    @Test
    void handleInvalidSignatureReturnsUnprocessableEntity() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/transaction");

        var response = handler.handleInvalidSignature(new InvalidSignatureException("Assinatura inválida"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Unprocessable Entity");
    }

    @Test
    void handleUnexpectedDoesNotExposeOriginalExceptionMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ledger/status");

        var response = handler.handleUnexpected(new Exception("database password leaked"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Erro interno inesperado");
    }
}
