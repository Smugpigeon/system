package com.lab.taskmanager.common.exception;

/**
 * Thrown when the requested domain object cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
