package dev.joaopdias.auditex.shared.exceptions;

public abstract class ApplicationException extends RuntimeException {

    private final Object details;

    protected ApplicationException(String message) {
        super(message);
        this.details = null;
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.details = null;
    }

    protected ApplicationException(String message, Object details) {
        super(message);
        this.details = details;
    }

    public Object getDetails() {
        return details;
    }
}
