package com.lab.taskmanager.common.exception;

/**
 * Base exception for predictable business validation failures.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
