package com.guruai.document.exception;

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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Global exception handler for the Document Service.
 *
 * <p>Maps all exceptions to the standard {@link ApiResponse} wrapper.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.debug("Validation failed: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleSizeExceeded(MaxUploadSizeExceededException ex) {
        log.debug("Upload exceeded size limit: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Maximum is 50 MB."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("Request body is missing or not valid JSON."));
    }

    @ExceptionHandler(GuruAIException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(GuruAIException ex) {
        log.warn("Domain exception: [{}] {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error in document-service", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.internalError("An unexpected error occurred."));
    }
}
