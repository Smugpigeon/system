package com.lab.taskmanager.auth.dto;

public record UserInfoResponse(
    Long userId,
    String username
) {
}
