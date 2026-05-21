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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lab3AdversarialIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void removedMemberShouldNotReadHistoricalDoneTeamTaskFromDashboard() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("advowner"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("advmember"), "abc12345");

        Long teamId = createTeam(owner, "Adversarial Team");
        addMember(owner, teamId, member.username());
        Long doneTaskId = createTeamTask(owner, teamId, member.userId(), "Historical Done Task", "DONE");

        ResponseEntity<String> beforeLeave = exchange(HttpMethod.GET, "/api/tasks", null, member.token());
        assertEquals(HttpStatus.OK, beforeLeave.getStatusCode());
        assertTrue(containsTitle(readBody(beforeLeave).path("data").path("records"), "Historical Done Task"));

        ResponseEntity<String> removeResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/members/" + member.userId(),
                null,
                owner.token());
        assertEquals(HttpStatus.OK, removeResponse.getStatusCode());

        ResponseEntity<String> dashboardAfterLeave = exchange(HttpMethod.GET, "/api/tasks", null, member.token());
        assertEquals(HttpStatus.OK, dashboardAfterLeave.getStatusCode());
        assertFalse(containsTitle(readBody(dashboardAfterLeave).path("data").path("records"), "Historical Done Task"));

        ResponseEntity<String> detailAfterLeave = exchange(
                HttpMethod.GET,
                "/api/tasks/" + doneTaskId,
                null,
                member.token());
        assertEquals(HttpStatus.NOT_FOUND, detailAfterLeave.getStatusCode());
    }

    @Test
    void memberPatchingUnassignedTeamTaskShouldReturnForbiddenInsteadOfServerError() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("unown"), "abc12345");
        AuthSession departed = registerAndLogin(uniqueUsername("undep"), "abc12345");
        AuthSession remaining = registerAndLogin(uniqueUsername("unrem"), "abc12345");

        Long teamId = createTeam(owner, "Unassigned Team");
        addMember(owner, teamId, departed.username());
        addMember(owner, teamId, remaining.username());
        Long taskId = createTeamTask(owner, teamId, departed.userId(), "Will Become Unassigned", "IN_PROGRESS");

        ResponseEntity<String> removeResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/members/" + departed.userId(),
                null,
                owner.token());
        assertEquals(HttpStatus.OK, removeResponse.getStatusCode());

        ResponseEntity<String> patchResponse = exchange(
                HttpMethod.PATCH,
                "/api/teams/" + teamId + "/tasks/" + taskId + "/status",
                Map.of("status", "DONE"),
                remaining.token());
        assertEquals(HttpStatus.FORBIDDEN, patchResponse.getStatusCode());
        assertTrue(readBody(patchResponse).path("message").asText().contains("团队成员只能修改"));
    }

    @Test
    void addingIncompletePredecessorToDoneTaskShouldBeRejected() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("depown"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("depmem"), "abc12345");

        Long teamId = createTeam(owner, "Dependency Team");
        addMember(owner, teamId, member.username());
        Long predecessorId = createTeamTask(owner, teamId, member.userId(), "Unfinished Predecessor", "TODO");
        Long successorId = createTeamTask(owner, teamId, member.userId(), "Already Done Successor", "DONE");

        ResponseEntity<String> addDependencyResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks/" + successorId + "/dependencies",
                Map.of("predecessorTaskId", predecessorId),
                owner.token());
        assertEquals(HttpStatus.BAD_REQUEST, addDependencyResponse.getStatusCode());
        assertTrue(readBody(addDependencyResponse).path("message").asText().contains("已完成任务"));
    }

    @Test
    void teamDependencyPathMustMatchTheTaskTeam() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("pathown"), "abc12345");

        Long firstTeamId = createTeam(owner, "First Team");
        Long secondTeamId = createTeam(owner, "Second Team");
        Long firstTeamTaskId = createTeamTask(owner, firstTeamId, owner.userId(), "First Team Task", "TODO");

        ResponseEntity<String> forgedPathResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + secondTeamId + "/tasks/" + firstTeamTaskId + "/dependencies",
                null,
                owner.token());
        assertEquals(HttpStatus.NOT_FOUND, forgedPathResponse.getStatusCode());
    }

    @Test
    void memberCannotManageDependenciesEvenWhenTheyKnowTaskIds() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("memown"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("memdep"), "abc12345");

        Long teamId = createTeam(owner, "Member Dependency Team");
        addMember(owner, teamId, member.username());
        Long predecessorId = createTeamTask(owner, teamId, member.userId(), "Predecessor", "DONE");
        Long successorId = createTeamTask(owner, teamId, member.userId(), "Successor", "TODO");

        ResponseEntity<String> memberAddResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks/" + successorId + "/dependencies",
                Map.of("predecessorTaskId", predecessorId),
                member.token());
        assertEquals(HttpStatus.FORBIDDEN, memberAddResponse.getStatusCode());

        ResponseEntity<String> ownerAddResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks/" + successorId + "/dependencies",
                Map.of("predecessorTaskId", predecessorId),
                owner.token());
        assertEquals(HttpStatus.CREATED, ownerAddResponse.getStatusCode());

        ResponseEntity<String> memberDeleteResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/tasks/" + successorId + "/dependencies/" + predecessorId,
                null,
                member.token());
        assertEquals(HttpStatus.FORBIDDEN, memberDeleteResponse.getStatusCode());
    }

    private AuthSession registerAndLogin(String username, String password) throws Exception {
        ResponseEntity<String> registerResponse = post(
                "/api/auth/register",
                Map.of("username", username, "password", password));
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        JsonNode registerBody = readBody(registerResponse);
        return new AuthSession(
                registerBody.path("data").path("userId").asLong(),
                username,
                registerBody.path("data").path("accessToken").asText());
    }

    private Long createTeam(AuthSession owner, String name) throws Exception {
        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/api/teams",
                Map.of("name", name),
                owner.token());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return readBody(response).path("data").path("id").asLong();
    }

    private void addMember(AuthSession owner, Long teamId, String username) {
        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/members",
                Map.of("username", username),
                owner.token());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private Long createTeamTask(
            AuthSession actor,
            Long teamId,
            Long assigneeId,
            String title,
            String status) throws Exception {
        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of(
                        "title", title,
                        "description", "created by adversarial test",
                        "status", status,
                        "priority", "MEDIUM",
                        "assigneeId", assigneeId),
                actor.token());
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
