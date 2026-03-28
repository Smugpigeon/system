package com.lab.taskmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "^[A-Za-z0-9_]{4,20}$",
                message = "用户名必须为 4 到 20 位字母、数字或下划线")
        String username,
        @NotBlank(message = "密码不能为空")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
                message = "密码至少 6 位，且必须同时包含字母和数字")
        String password) {
}
