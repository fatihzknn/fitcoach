package com.fitcoach.common;

/** Thrown when the caller is authenticated but not allowed to perform this action
 *  (e.g. a role mismatch). Maps to HTTP 403 — distinct from NotFoundException,
 *  which is for ownership checks that shouldn't confirm a resource's existence. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
