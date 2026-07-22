package com.guruai.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request lacks valid authentication credentials
 * or the authenticated user does not have permission to access the resource.
 *
 * <p>Uses HTTP {@code 401 Unauthorized} for missing/invalid tokens
 * and HTTP {@code 403 Forbidden} for valid token but insufficient permissions.
 */
public class UnauthorizedException extends GuruAIException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    /** Use this when the user is authenticated but not permitted (403). */
    public UnauthorizedException(String message, boolean forbidden) {
        super(forbidden ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED, message);
    }

    // ── Common factory methods ────────────────────────────────────────────

    public static UnauthorizedException missingToken() {
        return new UnauthorizedException("Authentication token is missing or expired. Please log in.");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("Invalid authentication token. Please log in again.");
    }

    public static UnauthorizedException accessDenied(String resource) {
        return new UnauthorizedException(
                "You do not have permission to access: " + resource, true);
    }
}
