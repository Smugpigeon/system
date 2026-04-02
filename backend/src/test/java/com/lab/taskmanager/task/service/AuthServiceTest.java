package com.lab.taskmanager.auth.service;

import com.lab.taskmanager.auth.dto.AuthResponse;
import com.lab.taskmanager.auth.dto.LoginRequest;
import com.lab.taskmanager.auth.dto.RegisterRequest;
import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private User createTestUser(Long id, String username, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        return user;
    }

    // ========== register ==========

    @Test
    void register_shouldCreateNewUser() {
        String username = "newuser";
        String password = "password123";
        String hashedPassword = encoder.encode(password);
        
        when(userRepository.existsByUsername(username)).thenReturn(false);
        User savedUser = createTestUser(1L, username, hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterRequest request = new RegisterRequest(username, password);
        AuthResponse result = authService.register(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo(username);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldTrimUsername() {
        String rawUsername = "  testuser  ";
        String trimmedUsername = "testuser";
        
        when(userRepository.existsByUsername(trimmedUsername)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        RegisterRequest request = new RegisterRequest(rawUsername, "password");
        authService.register(request);

        verify(userRepository).existsByUsername(trimmedUsername);
    }

    @Test
    void register_shouldThrow_whenUsernameExists() {
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        RegisterRequest request = new RegisterRequest(username, "password");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
        
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldHashPassword() {
        String password = "mySecretPassword";
        
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        RegisterRequest request = new RegisterRequest("user", password);
        authService.register(request);

        verify(userRepository).save(argThat(user -> 
            !user.getPasswordHash().equals(password) && // 确保不是明文
            encoder.matches(password, user.getPasswordHash()) // 确保能匹配
        ));
    }

    // ========== login ==========

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsValid() {
        String username = "testuser";
        String password = "correctpassword";
        String hashedPassword = encoder.encode(password);
        User user = createTestUser(1L, username, hashedPassword);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest(username, password);
        AuthResponse result = authService.login(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo(username);
    }

    @Test
    void login_shouldTrimUsername() {
        String rawUsername = "  testuser  ";
        String trimmedUsername = "testuser";
        User user = createTestUser(1L, trimmedUsername, encoder.encode("pass"));
        
        when(userRepository.findByUsername(trimmedUsername)).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest(rawUsername, "pass");
        authService.login(request);

        verify(userRepository).findByUsername(trimmedUsername);
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest(username, "password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void login_shouldThrow_whenPasswordIncorrect() {
        String username = "testuser";
        String correctPassword = "correct123";
        String wrongPassword = "wrong456";
        User user = createTestUser(1L, username, encoder.encode(correctPassword));
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest(username, wrongPassword);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void login_shouldNotExposeWhichFieldIsWrong() {
        // 测试用例：无论用户名不存在还是密码错误，提示信息相同
        String username = "testuser";
        
        // 情况1：用户不存在
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest(username, "password");
        
        BusinessException ex1 = org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class, () -> authService.login(request)
        );

        // 情况2：密码错误
        User user = createTestUser(1L, username, encoder.encode("correct"));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        request = new LoginRequest(username, "wrongpassword");
        
        BusinessException ex2 = org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class, () -> authService.login(request)
        );

        // 错误消息相同，不暴露具体是哪个字段错误
        assertThat(ex1.getMessage()).isEqualTo(ex2.getMessage());
    }
}