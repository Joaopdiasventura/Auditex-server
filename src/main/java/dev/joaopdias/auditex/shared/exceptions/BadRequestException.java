package dev.joaopdias.auditex.shared.exceptions;

public class BadRequestException extends ApplicationException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message, Object details) {
        super(message, details);
    }
}
