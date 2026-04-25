package com.lab.taskmanager.auth.controller;

import com.lab.taskmanager.auth.dto.AuthResponse;
import com.lab.taskmanager.auth.dto.LoginRequest;
import com.lab.taskmanager.auth.dto.RegisterRequest;
import com.lab.taskmanager.auth.service.AuthService;
import com.lab.taskmanager.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "提供用户注册和登录接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册一个新用户并返回认证信息")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("注册成功", response));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录并返回认证信息")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }
}
