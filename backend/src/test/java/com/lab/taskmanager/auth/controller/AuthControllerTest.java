package com.lab.taskmanager.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.taskmanager.auth.dto.AuthResponse;
import com.lab.taskmanager.auth.security.CustomUserDetailsService;
import com.lab.taskmanager.auth.security.JwtService;
import com.lab.taskmanager.auth.service.AuthService;
import com.lab.taskmanager.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerShouldReturnCreated() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse(1L, "alice_01", "asdfsdfadsf"));

        String requestBody = """
                {
                  "username": "alice_01",
                  "password": "abc123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("alice_01"));
    }

    @Test
    void loginShouldReturnOk() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(2L, "bob_02", "dsfasdfasd"));

        String requestBody = objectMapper.writeValueAsString(
                new LoginRequestBody("bob_02", "abc123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.username").value("bob_02"));
    }

    @Test
    void registerShouldReturnBadRequestWhenRequestInvalid() throws Exception {
        String requestBody = """
                {
                  "username": "a",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private record LoginRequestBody(String username, String password) {
    }
}
