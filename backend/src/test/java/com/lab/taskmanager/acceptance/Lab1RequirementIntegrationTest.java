package com.lab.taskmanager.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lab1RequirementIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerShouldValidateInputHashPasswordAndRejectDuplicateUsername() throws Exception {
        ResponseEntity<String> invalidUsernameResponse = post(
                "/api/auth/register",
                Map.of("username", "ab", "password", "abc123"));
        assertEquals(HttpStatus.BAD_REQUEST, invalidUsernameResponse.getStatusCode());
        assertTrue(readBody(invalidUsernameResponse).path("message").asText().contains("用户名"));

        ResponseEntity<String> invalidPasswordResponse = post(
                "/api/auth/register",
                Map.of("username", uniqueUsername("invalid"), "password", "abcdef"));
        assertEquals(HttpStatus.BAD_REQUEST, invalidPasswordResponse.getStatusCode());
        assertTrue(readBody(invalidPasswordResponse).path("message").asText().contains("密码"));

        String username = uniqueUsername("hashuser");
        String rawPassword = "abc12345";
        ResponseEntity<String> registerResponse = post(
                "/api/auth/register",
                Map.of("username", username, "password", rawPassword));
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        JsonNode registerBody = readBody(registerResponse);
        assertTrue(registerBody.path("success").asBoolean());
        assertTrue(registerBody.path("data").path("accessToken").asText().length() > 20);

        User savedUser = userRepository.findByUsername(username).orElseThrow();
        assertNotEquals(rawPassword, savedUser.getPasswordHash());
        assertFalse(savedUser.getPasswordHash().contains(rawPassword));

        ResponseEntity<String> duplicateResponse = post(
                "/api/auth/register",
                Map.of("username", username, "password", "abc12345"));
        assertEquals(HttpStatus.BAD_REQUEST, duplicateResponse.getStatusCode());
        assertTrue(readBody(duplicateResponse).path("message").asText().contains("用户名已存在"));
    }

    @Test
    void loginShouldReturnJwtAndProtectedApisShouldRequireAuthentication() throws Exception {
        String username = uniqueUsername("loginuser");
        String password = "pass1234";
        register(username, password);

        ResponseEntity<String> wrongPasswordResponse = post(
                "/api/auth/login",
                Map.of("username", username, "password", "wrong123"));
        assertEquals(HttpStatus.BAD_REQUEST, wrongPasswordResponse.getStatusCode());
        assertTrue(readBody(wrongPasswordResponse).path("message").asText().contains("用户名或密码错误"));

        ResponseEntity<String> anonymousTaskResponse = restTemplate.getForEntity("/api/tasks", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, anonymousTaskResponse.getStatusCode());
        JsonNode anonymousTaskBody = readBody(anonymousTaskResponse);
        assertFalse(anonymousTaskBody.path("success").asBoolean());
        assertTrue(anonymousTaskBody.path("message").asText().contains("未登录"));

        ResponseEntity<String> loginResponse = post(
                "/api/auth/login",
                Map.of("username", username, "password", password));
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        JsonNode loginBody = readBody(loginResponse);
        assertTrue(loginBody.path("success").asBoolean());
        assertEquals(username, loginBody.path("data").path("username").asText());
        assertTrue(loginBody.path("data").path("accessToken").asText().length() > 20);

        ResponseEntity<String> taskListResponse = exchange(
                HttpMethod.GET,
                "/api/tasks",
                null,
                loginBody.path("data").path("accessToken").asText());
        assertEquals(HttpStatus.OK, taskListResponse.getStatusCode());
        JsonNode taskListBody = readBody(taskListResponse);
        assertTrue(taskListBody.path("success").asBoolean());
        assertEquals(0, taskListBody.path("data").path("totalRecords").asInt());

        ResponseEntity<String> invalidTokenResponse = exchange(
                HttpMethod.GET,
                "/api/tasks",
                null,
                "invalid-token");
        assertEquals(HttpStatus.UNAUTHORIZED, invalidTokenResponse.getStatusCode());
        assertTrue(readBody(invalidTokenResponse).path("message").asText().contains("令牌"));
    }

    @Test
    void taskCrudShouldPersistDataAndKeepUsersIsolated() throws Exception {
        AuthSession alice = registerAndLogin(uniqueUsername("alice"), "abc12345");
        AuthSession bob = registerAndLogin(uniqueUsername("bob"), "abc12345");

        ResponseEntity<String> createResponse = exchange(
                HttpMethod.POST,
                "/api/tasks",
                Map.of(
                        "title", "Prepare Lab1 Demo",
                        "description", "Record the acceptance flow",
                        "status", "TODO",
                        "priority", "HIGH",
                        "dueAt", LocalDateTime.of(2026, 4, 5, 20, 0).toString()),
                alice.token());
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        JsonNode createBody = readBody(createResponse);
        Long taskId = createBody.path("data").path("id").asLong();
        assertTrue(taskId > 0);
        assertNotNull(createBody.path("data").path("createdAt").asText(null));
        assertNotNull(createBody.path("data").path("updatedAt").asText(null));

        ResponseEntity<String> aliceListResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?status=TODO&priority=HIGH&page=1&size=10",
                null,
                alice.token());
        assertEquals(HttpStatus.OK, aliceListResponse.getStatusCode());
        JsonNode aliceListBody = readBody(aliceListResponse);
        assertEquals(1, aliceListBody.path("data").path("totalRecords").asInt());
        assertEquals("Prepare Lab1 Demo", aliceListBody.path("data").path("records").get(0).path("title").asText());

        ResponseEntity<String> keywordMatchResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?keyword=acceptance&page=1&size=10",
                null,
                alice.token());
        assertEquals(HttpStatus.OK, keywordMatchResponse.getStatusCode());
        assertEquals(1, readBody(keywordMatchResponse).path("data").path("totalRecords").asInt());

        ResponseEntity<String> keywordMissResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?keyword=not_existing_keyword&page=1&size=10",
                null,
                alice.token());
        assertEquals(HttpStatus.OK, keywordMissResponse.getStatusCode());
        assertEquals(0, readBody(keywordMissResponse).path("data").path("totalRecords").asInt());

        ResponseEntity<String> bobDetailResponse = exchange(
                HttpMethod.GET,
                "/api/tasks/" + taskId,
                null,
                bob.token());
        assertEquals(HttpStatus.NOT_FOUND, bobDetailResponse.getStatusCode());

        ResponseEntity<String> updateResponse = exchange(
                HttpMethod.PUT,
                "/api/tasks/" + taskId,
                Map.of(
                        "title", "Prepare Final Lab1 Demo",
                        "description", "Record the acceptance flow and screenshots",
                        "status", "IN_PROGRESS",
                        "priority", "MEDIUM",
                        "dueAt", LocalDateTime.of(2026, 4, 6, 10, 30).toString()),
                alice.token());
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(
                "IN_PROGRESS",
                readBody(updateResponse).path("data").path("status").asText());

        ResponseEntity<String> bobDeleteResponse = exchange(
                HttpMethod.DELETE,
                "/api/tasks/" + taskId,
                null,
                bob.token());
        assertEquals(HttpStatus.NOT_FOUND, bobDeleteResponse.getStatusCode());

        ResponseEntity<String> aliceDeleteResponse = exchange(
                HttpMethod.DELETE,
                "/api/tasks/" + taskId,
                null,
                alice.token());
        assertEquals(HttpStatus.OK, aliceDeleteResponse.getStatusCode());

        ResponseEntity<String> afterDeleteResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?page=1&size=10",
                null,
                alice.token());
        assertEquals(0, readBody(afterDeleteResponse).path("data").path("totalRecords").asInt());
    }

    @Test
    void taskListShouldHandlePagingBoundariesAndOutOfRangeRankPages() throws Exception {
        AuthSession user = registerAndLogin(uniqueUsername("pager"), "abc12345");

        ResponseEntity<String> invalidPageResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?page=0&size=10",
                null,
                user.token());
        assertEquals(HttpStatus.BAD_REQUEST, invalidPageResponse.getStatusCode());
        assertTrue(readBody(invalidPageResponse).path("message").asText().contains("页码"));

        ResponseEntity<String> invalidSizeResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?page=1&size=0",
                null,
                user.token());
        assertEquals(HttpStatus.BAD_REQUEST, invalidSizeResponse.getStatusCode());
        assertTrue(readBody(invalidSizeResponse).path("message").asText().contains("每页大小"));

        ResponseEntity<String> oversizeResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?page=1&size=101",
                null,
                user.token());
        assertEquals(HttpStatus.BAD_REQUEST, oversizeResponse.getStatusCode());
        assertTrue(readBody(oversizeResponse).path("message").asText().contains("不能超过"));

        exchange(
                HttpMethod.POST,
                "/api/tasks",
                Map.of(
                        "title", "Rank page task",
                        "description", "used for rank paging edge case",
                        "status", "TODO",
                        "priority", "HIGH"),
                user.token());

        ResponseEntity<String> outOfRangeRankResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?sortBy=rank&page=2&size=10",
                null,
                user.token());
        assertEquals(HttpStatus.OK, outOfRangeRankResponse.getStatusCode());
        JsonNode outOfRangeBody = readBody(outOfRangeRankResponse);
        assertEquals(1, outOfRangeBody.path("data").path("totalRecords").asInt());
        assertEquals(1, outOfRangeBody.path("data").path("totalPages").asInt());
        assertEquals(0, outOfRangeBody.path("data").path("records").size());
    }

    private void register(String username, String password) {
        ResponseEntity<String> response = post(
                "/api/auth/register",
                Map.of("username", username, "password", password));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private AuthSession registerAndLogin(String username, String password) throws Exception {
        register(username, password);
        ResponseEntity<String> loginResponse = post(
                "/api/auth/login",
                Map.of("username", username, "password", password));
        JsonNode loginBody = readBody(loginResponse);
        return new AuthSession(
                loginBody.path("data").path("userId").asLong(),
                loginBody.path("data").path("username").asText(),
                loginBody.path("data").path("accessToken").asText());
    }

    private ResponseEntity<String> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> exchange(
            HttpMethod method,
            String path,
            Object body,
            String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<?> request = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, method, request, String.class);
    }

    private JsonNode readBody(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String uniqueUsername(String prefix) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return username.length() > 20 ? username.substring(0, 20) : username;
    }

    private record AuthSession(Long userId, String username, String token) {
    }
}
