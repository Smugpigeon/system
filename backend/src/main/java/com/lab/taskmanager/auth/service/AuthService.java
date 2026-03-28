package com.lab.taskmanager.auth.service;

import com.lab.taskmanager.auth.dto.AuthResponse;
import com.lab.taskmanager.auth.dto.LoginRequest;
import com.lab.taskmanager.auth.dto.RegisterRequest;
import com.lab.taskmanager.auth.dto.UserInfoResponse;
import com.lab.taskmanager.auth.security.JwtService;
import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Register a new user account and immediately issue a JWT for frontend persistence.
     *
     * @param request validated register payload
     * @return authentication result with token
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);
        return new AuthResponse(savedUser.getId(), savedUser.getUsername(), jwtService.generateToken(savedUser));
    }

    /**
     * Validate login credentials and issue a fresh JWT.
     *
     * @param request login payload from the client
     * @return authentication result with token
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        return new AuthResponse(user.getId(), user.getUsername(), jwtService.generateToken(user));
    }

    /**
     * Get current user information by username.
     * 
     * @param username the username of the authenticated user
     * @return current user info
     */
    public UserInfoResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        return new UserInfoResponse(user.getId(), user.getUsername());
    }
}
