package com.lab.taskmanager.auth.controller;

import com.lab.taskmanager.auth.dto.AuthResponse;
import com.lab.taskmanager.auth.dto.LoginRequest;
import com.lab.taskmanager.auth.dto.RegisterRequest;
import com.lab.taskmanager.auth.dto.UserInfoResponse;
import com.lab.taskmanager.auth.service.AuthService;
import com.lab.taskmanager.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new account for the task management system.
     *
     * @param request username and password payload
     * @return created user info and token
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("注册成功", response));
    }

    /**
     * Authenticate a user and return a token for subsequent protected requests.
     *
     * @param request login payload
     * @return user info and token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * Get current authenticated user information.
     *
     * @param userDetails the authenticated user details
     * @return current user info
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserInfoResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", response));
    }
}
