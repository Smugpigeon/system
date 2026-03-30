package com.lab.taskmanager.auth.service;

import com.lab.taskmanager.auth.dto.AuthResponse;
import com.lab.taskmanager.auth.dto.LoginRequest;
import com.lab.taskmanager.auth.dto.RegisterRequest;
import com.lab.taskmanager.auth.security.JwtService;
import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_FAILED_MESSAGE = "用户名或密码错误";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        validateUsernameNotExists(normalizedUsername);

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new BusinessException(LOGIN_FAILED_MESSAGE));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(LOGIN_FAILED_MESSAGE);
        }

        return buildAuthResponse(user);
    }

    private void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }
}
