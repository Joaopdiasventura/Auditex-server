package dev.joaopdias.auditex.shared.exceptions;

public class InvalidSignatureException extends ApplicationException {
    public InvalidSignatureException(String message) {
        super(message);
    }

    public InvalidSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
