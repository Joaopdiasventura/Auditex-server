package dev.joaopdias.auditex.shared.exceptions;

public class ConflictException extends ApplicationException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
