package dev.joaopdias.auditex.shared.exceptions;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Object details
) {
}
