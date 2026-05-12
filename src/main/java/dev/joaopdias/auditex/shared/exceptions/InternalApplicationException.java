package dev.joaopdias.auditex.shared.exceptions;

public class InternalApplicationException extends ApplicationException {
    public InternalApplicationException(String message) {
        super(message);
    }

    public InternalApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
