package com.lab.taskmanager.common.exception;

/**
 * Raised when the current user is authenticated but lacks permission
 * to perform the requested operation.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
