package com.lab.taskmanager.common.api;

/**
 * Standard response wrapper used by the frontend to handle success and error states consistently.
 *
 * @param success whether the request succeeded
 * @param message readable message for UI display
 * @param data payload for the request
 * @param <T> payload type
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static ApiResponse<Void> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
