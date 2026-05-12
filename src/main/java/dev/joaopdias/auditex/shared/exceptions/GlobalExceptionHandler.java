package dev.joaopdias.auditex.shared.exceptions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return build(404, "Not Found", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException exception,
            HttpServletRequest request) {
        return build(409, "Conflict", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException exception,
            HttpServletRequest request) {
        return build(400, "Bad Request", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(InvalidSignatureException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSignature(
            InvalidSignatureException exception,
            HttpServletRequest request) {
        return build(422, "Unprocessable Entity", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ApiErrorResponse> handleInfrastructure(
            InfrastructureException exception,
            HttpServletRequest request) {
        return build(503, "Service Unavailable", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(InternalApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleInternalApplication(
            InternalApplicationException exception,
            HttpServletRequest request) {
        return build(500, "Internal Server Error", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<Map<String, String>> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();

        return build(400, "Bad Request", "Dados inválidos", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<Map<String, String>> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();

        return build(400, "Bad Request", "Parâmetros inválidos", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return build(400, "Bad Request", "JSON inválido", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return build(400, "Bad Request", "Parâmetro inválido", request, exception.getName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return build(400, "Bad Request", "Parâmetro obrigatório ausente", request, exception.getParameterName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return build(409, "Conflict", "Recurso duplicado ou violação de integridade", request, null);
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiErrorResponse> handleAmqp(
            AmqpException exception,
            HttpServletRequest request) {
        return build(503, "Service Unavailable", "Serviço de mensageria indisponível", request, null);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplication(
            ApplicationException exception,
            HttpServletRequest request) {
        return build(500, "Internal Server Error", exception.getMessage(), request, exception.getDetails());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        return build(500, "Internal Server Error", "Erro interno inesperado", request, null);
    }

    private ResponseEntity<ApiErrorResponse> build(
            int status,
            String error,
            String message,
            HttpServletRequest request,
            Object details) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                request.getRequestURI(),
                details);

        return ResponseEntity.status(status).body(response);
    }
}
