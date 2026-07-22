package com.guruai.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard envelope for every HTTP response across all GuruAI services.
 *
 * <p>All REST controllers must wrap their return values in this record.
 * {@code null} fields are omitted from JSON (e.g. {@code error} is absent on success).
 *
 * <h3>Success example</h3>
 * <pre>{@code
 * {
 *   "success": true,
 *   "status":  200,
 *   "data":    { ... },
 *   "timestamp": "2026-06-19T11:30:00Z"
 * }
 * }</pre>
 *
 * <h3>Error example</h3>
 * <pre>{@code
 * {
 *   "success":  false,
 *   "status":   404,
 *   "error":    "Session not found",
 *   "timestamp": "2026-06-19T11:30:00Z"
 * }
 * }</pre>
 *
 * @param <T> the type of the {@code data} payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int     status,
        T       data,
        String  error,
        String  message,
        Instant timestamp
) {

    // ── Static factory methods ────────────────────────────────────────────

    /** 200 OK with a data payload. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, data, null, null, Instant.now());
    }

    /** 200 OK with a data payload and a descriptive message. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, 200, data, null, message, Instant.now());
    }

    /** 201 Created with a data payload. */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, data, null, "Created successfully", Instant.now());
    }

    /** 204 No Content (e.g. successful DELETE). */
    public static <Void> ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, 204, null, null, "Deleted successfully", Instant.now());
    }

    /** Generic error constructor using {@link HttpStatus}. */
    public static <T> ApiResponse<T> error(HttpStatus status, String errorMessage) {
        return new ApiResponse<>(false, status.value(), null, errorMessage, null, Instant.now());
    }

    /** 400 Bad Request. */
    public static <T> ApiResponse<T> badRequest(String errorMessage) {
        return error(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /** 401 Unauthorized. */
    public static <T> ApiResponse<T> unauthorized(String errorMessage) {
        return error(HttpStatus.UNAUTHORIZED, errorMessage);
    }

    /** 403 Forbidden. */
    public static <T> ApiResponse<T> forbidden(String errorMessage) {
        return error(HttpStatus.FORBIDDEN, errorMessage);
    }

    /** 404 Not Found. */
    public static <T> ApiResponse<T> notFound(String errorMessage) {
        return error(HttpStatus.NOT_FOUND, errorMessage);
    }

    /** 500 Internal Server Error. */
    public static <T> ApiResponse<T> internalError(String errorMessage) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
