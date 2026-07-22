package com.guruai.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all GuruAI domain-specific errors.
 *
 * <p>Every service's {@code @ControllerAdvice} should catch this exception
 * and map it to an {@link com.guruai.common.dto.ApiResponse} with the
 * embedded {@link HttpStatus}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * throw new GuruAIException(HttpStatus.BAD_REQUEST, "No documents uploaded yet");
 * throw new GuruAIException(HttpStatus.CONFLICT,    "Username already taken");
 * }</pre>
 */
public class GuruAIException extends RuntimeException {

    private final HttpStatus status;

    public GuruAIException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public GuruAIException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /** The HTTP status code to return to the client. */
    public HttpStatus getStatus() {
        return status;
    }

    /** The HTTP status integer value. */
    public int getStatusCode() {
        return status.value();
    }
}
