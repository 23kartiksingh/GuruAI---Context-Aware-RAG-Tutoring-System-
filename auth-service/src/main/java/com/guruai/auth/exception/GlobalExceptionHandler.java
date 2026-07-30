package com.guruai.auth.exception;

import com.guruai.common.dto.ApiResponse;
import com.guruai.common.exception.GuruAIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for the Auth Service.
 *
 * <p>Catches all exceptions from controllers and maps them to the standard
 * {@link ApiResponse} wrapper. This ensures every error response has the same
 * shape as every success response.
 *
 * <p>Exception hierarchy handled:
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} — Bean Validation failures (400)</li>
 *   <li>{@link HttpMessageNotReadableException} — malformed/unparsable JSON body (400)</li>
 *   <li>{@link GuruAIException} (and all subclasses) — domain errors (4xx)</li>
 *   <li>{@link Exception} — unexpected errors (500)</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle Bean Validation failures from {@code @Valid} on request bodies.
     * Collects all field errors into a single readable message.
     *
     * <p>Example response body:
     * <pre>{@code
     * {
     *   "success": false,
     *   "status":  400,
     *   "error":   "username: Username is required; password: Password must be 6–100 characters"
     * }
     * }</pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.debug("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest(errors));
    }

    /**
     * Handle malformed request bodies — invalid JSON syntax, wrong types,
     * truncated payloads, etc. Spring throws this at the HTTP-message-conversion
     * layer, before the controller method (and its {@code @Valid}) ever runs,
     * so without this handler it fell through to the generic 500 handler
     * below and returned an opaque "unexpected error" for what is really
     * just a bad request from the client.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Request body is missing or not valid JSON."));
    }

    /**
     * Handle all GuruAI domain exceptions.
     * The status code is embedded in the exception itself.
     */
    @ExceptionHandler(GuruAIException.class)
    public ResponseEntity<ApiResponse<Void>> handleGuruAIException(GuruAIException ex) {
        log.warn("Domain exception: [{}] {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus(), ex.getMessage()));
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Logs the full stack trace but returns a generic message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedError(Exception ex) {
        log.error("Unexpected error in auth-service", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.internalError(
                        "An unexpected error occurred. Please try again later."));
    }
}
