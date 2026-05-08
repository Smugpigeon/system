package com.lab.taskmanager.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lab3RequirementIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dependenciesAndTeamLifecycleRulesShouldBeEnforcedByBackend() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("owner"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("member"), "abc12345");

        Long teamId = createTeam(owner, "Lab3 Team");
        exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/members",
                Map.of("username", member.username()),
                owner.token());
        Long memberUserId = findUserId(
                readBody(exchange(HttpMethod.GET, "/api/teams/" + teamId, null, owner.token()))
                        .path("data")
                        .path("members"),
                member.username());

        Long predecessorTaskId = createTeamTask(owner, teamId, owner.userId(), "Finish backend rules", "TODO");
        Long successorTaskId = createTeamTask(owner, teamId, memberUserId, "Run dependent demo", "TODO");

        ResponseEntity<String> addDependencyResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks/" + successorTaskId + "/dependencies",
                Map.of("predecessorTaskId", predecessorTaskId),
                owner.token());
        assertEquals(HttpStatus.CREATED, addDependencyResponse.getStatusCode());

        ResponseEntity<String> blockedDoneResponse = exchange(
                HttpMethod.PATCH,
                "/api/teams/" + teamId + "/tasks/" + successorTaskId + "/status",
                Map.of("status", "DONE"),
                member.token());
        assertEquals(HttpStatus.BAD_REQUEST, blockedDoneResponse.getStatusCode());
        assertTrue(readBody(blockedDoneResponse).path("message").asText().contains("前置任务"));

        ResponseEntity<String> predecessorDoneResponse = exchange(
                HttpMethod.PATCH,
                "/api/teams/" + teamId + "/tasks/" + predecessorTaskId + "/status",
                Map.of("status", "DONE"),
                owner.token());
        assertEquals(HttpStatus.OK, predecessorDoneResponse.getStatusCode());

        ResponseEntity<String> successorDoneResponse = exchange(
                HttpMethod.PATCH,
                "/api/teams/" + teamId + "/tasks/" + successorTaskId + "/status",
                Map.of("status", "DONE"),
                member.token());
        assertEquals(HttpStatus.OK, successorDoneResponse.getStatusCode());

        ResponseEntity<String> deleteDependedTaskResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/tasks/" + predecessorTaskId,
                null,
                owner.token());
        assertEquals(HttpStatus.BAD_REQUEST, deleteDependedTaskResponse.getStatusCode());

        ResponseEntity<String> removeDependencyResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/tasks/" + successorTaskId + "/dependencies/" + predecessorTaskId,
                null,
                owner.token());
        assertEquals(HttpStatus.OK, removeDependencyResponse.getStatusCode());

        ResponseEntity<String> deletePredecessorAfterDetachResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/tasks/" + predecessorTaskId,
                null,
                owner.token());
        assertEquals(HttpStatus.OK, deletePredecessorAfterDetachResponse.getStatusCode());

        Long leavingTaskId = createTeamTask(owner, teamId, memberUserId, "Task before leave", "IN_PROGRESS");
        ResponseEntity<String> leaveResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/leave",
                null,
                member.token());
        assertEquals(HttpStatus.OK, leaveResponse.getStatusCode());

        ResponseEntity<String> memberAccessAfterLeaveResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId + "/tasks/" + leavingTaskId,
                null,
                member.token());
        assertEquals(HttpStatus.NOT_FOUND, memberAccessAfterLeaveResponse.getStatusCode());

        ResponseEntity<String> ownerDashboardResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?page=1&size=20",
                null,
                owner.token());
        assertEquals(HttpStatus.OK, ownerDashboardResponse.getStatusCode());
        assertTrue(containsTitle(readBody(ownerDashboardResponse).path("data").path("records"), "Task before leave"));

        ResponseEntity<String> dissolveResponse = exchange(HttpMethod.DELETE, "/api/teams/" + teamId, null, owner.token());
        assertEquals(HttpStatus.OK, dissolveResponse.getStatusCode());

        ResponseEntity<String> accessDissolvedTeamResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId,
                null,
                owner.token());
        assertEquals(HttpStatus.FORBIDDEN, accessDissolvedTeamResponse.getStatusCode());
    }

    private AuthSession registerAndLogin(String username, String password) throws Exception {
        ResponseEntity<String> registerResponse = post("/api/auth/register", Map.of("username", username, "password", password));
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        JsonNode registerBody = readBody(registerResponse);
        return new AuthSession(
                registerBody.path("data").path("userId").asLong(),
                username,
                registerBody.path("data").path("accessToken").asText());
    }

    private Long createTeam(AuthSession owner, String name) throws Exception {
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/api/teams", Map.of("name", name), owner.token());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return readBody(response).path("data").path("id").asLong();
    }

    private Long createTeamTask(
            AuthSession creator,
            Long teamId,
            Long assigneeId,
            String title,
            String status) throws Exception {
        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of(
                        "title", title,
                        "description", "Lab3 acceptance task",
                        "status", status,
                        "priority", "MEDIUM",
                        "assigneeId", assigneeId),
                creator.token());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return readBody(response).path("data").path("id").asLong();
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
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode readBody(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String uniqueUsername(String prefix) {
        String candidate = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return candidate.length() <= 20 ? candidate : candidate.substring(0, 20);
    }

    private Long findUserId(JsonNode members, String username) {
        for (JsonNode member : members) {
            if (username.equals(member.path("username").asText())) {
                return member.path("userId").asLong();
            }
        }
        throw new IllegalStateException("Member not found: " + username);
    }

    private boolean containsTitle(JsonNode records, String title) {
        for (JsonNode record : records) {
            if (title.equals(record.path("title").asText())) {
                return true;
            }
        }
        return false;
    }

    private record AuthSession(Long userId, String username, String token) {
    }
}
