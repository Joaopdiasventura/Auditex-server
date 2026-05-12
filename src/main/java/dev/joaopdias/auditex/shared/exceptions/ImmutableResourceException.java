package dev.joaopdias.auditex.shared.exceptions;

public class ImmutableResourceException extends ConflictException {
    public ImmutableResourceException(String message) {
        super(message);
    }
}
