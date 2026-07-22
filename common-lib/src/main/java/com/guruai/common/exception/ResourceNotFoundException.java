package com.guruai.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 *
 * <p>Maps to HTTP {@code 404 Not Found}.
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * throw new ResourceNotFoundException("Session", sessionId);
 * throw new ResourceNotFoundException("Document", documentId);
 * throw new ResourceNotFoundException("User", userId);
 * }</pre>
 */
public class ResourceNotFoundException extends GuruAIException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(HttpStatus.NOT_FOUND,
              String.format("%s with id '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId   = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
        this.resourceType = "Resource";
        this.resourceId   = "unknown";
    }

    public String getResourceType() { return resourceType; }
    public String getResourceId()   { return resourceId; }
}
