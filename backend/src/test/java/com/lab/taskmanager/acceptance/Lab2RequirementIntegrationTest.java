package com.lab.taskmanager.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lab2RequirementIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teamCollaborationShouldRespectRolesAndIsolationRules() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("owner"), "abc12345");
        AuthSession adminCandidate = registerAndLogin(uniqueUsername("admin"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("member"), "abc12345");
        AuthSession outsider = registerAndLogin(uniqueUsername("outsider"), "abc12345");

        ResponseEntity<String> createTeamResponse = exchange(
                HttpMethod.POST,
                "/api/teams",
                Map.of("name", "Lab2 Team Alpha"),
                owner.token());
        assertEquals(HttpStatus.CREATED, createTeamResponse.getStatusCode());
        JsonNode createdTeamBody = readBody(createTeamResponse);
        Long teamId = createdTeamBody.path("data").path("id").asLong();
        assertEquals("OWNER", createdTeamBody.path("data").path("currentUserRole").asText());

        exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/members",
                Map.of("username", adminCandidate.username()),
                owner.token());
        exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/members",
                Map.of("username", member.username()),
                owner.token());

        ResponseEntity<String> teamDetailResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId,
                null,
                owner.token());
        assertEquals(HttpStatus.OK, teamDetailResponse.getStatusCode());
        JsonNode members = readBody(teamDetailResponse).path("data").path("members");
        assertEquals(3, members.size());
        Long adminUserId = findUserId(members, adminCandidate.username());
        Long memberUserId = findUserId(members, member.username());

        ResponseEntity<String> outsiderDetailResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId,
                null,
                outsider.token());
        assertEquals(HttpStatus.NOT_FOUND, outsiderDetailResponse.getStatusCode());

        ResponseEntity<String> promoteAdminResponse = exchange(
                HttpMethod.PUT,
                "/api/teams/" + teamId + "/members/" + adminUserId + "/role",
                Map.of("role", "ADMIN"),
                owner.token());
        assertEquals(HttpStatus.OK, promoteAdminResponse.getStatusCode());
        assertEquals("ADMIN", readBody(promoteAdminResponse).path("data").path("role").asText());

        ResponseEntity<String> memberCreateTaskResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of(
                        "title", "Member should not create",
                        "description", "member create must fail",
                        "status", "TODO",
                        "priority", "HIGH",
                        "assigneeId", memberUserId),
                member.token());
        assertEquals(HttpStatus.FORBIDDEN, memberCreateTaskResponse.getStatusCode());

        ResponseEntity<String> adminCreateTaskForMemberResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of(
                        "title", "Prepare team demo",
                        "description", "assigned to member",
                        "status", "TODO",
                        "priority", "HIGH",
                        "dueAt", LocalDateTime.of(2026, 5, 1, 10, 0).toString(),
                        "assigneeId", memberUserId),
                adminCandidate.token());
        assertEquals(HttpStatus.CREATED, adminCreateTaskForMemberResponse.getStatusCode());
        Long memberTaskId = readBody(adminCreateTaskForMemberResponse).path("data").path("id").asLong();

        ResponseEntity<String> adminCreateTaskForOwnerResponse = exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of(
                        "title", "Owner review",
                        "description", "assigned to owner",
                        "status", "IN_PROGRESS",
                        "priority", "MEDIUM",
                        "assigneeId", owner.userId()),
                adminCandidate.token());
        assertEquals(HttpStatus.CREATED, adminCreateTaskForOwnerResponse.getStatusCode());
        Long ownerTaskId = readBody(adminCreateTaskForOwnerResponse).path("data").path("id").asLong();

        ResponseEntity<String> memberTeamTasksResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId + "/tasks?page=1&size=10",
                null,
                member.token());
        assertEquals(HttpStatus.OK, memberTeamTasksResponse.getStatusCode());
        assertEquals(2, readBody(memberTeamTasksResponse).path("data").path("totalRecords").asInt());

        ResponseEntity<String> memberPatchOwnStatusResponse = exchange(
                HttpMethod.PATCH,
                "/api/teams/" + teamId + "/tasks/" + memberTaskId + "/status",
                Map.of("status", "DONE"),
                member.token());
        assertEquals(HttpStatus.OK, memberPatchOwnStatusResponse.getStatusCode());
        assertEquals("DONE", readBody(memberPatchOwnStatusResponse).path("data").path("status").asText());

        ResponseEntity<String> memberPatchOthersStatusResponse = exchange(
                HttpMethod.PATCH,
                "/api/teams/" + teamId + "/tasks/" + ownerTaskId + "/status",
                Map.of("status", "DONE"),
                member.token());
        assertEquals(HttpStatus.FORBIDDEN, memberPatchOthersStatusResponse.getStatusCode());

        ResponseEntity<String> memberFullUpdateResponse = exchange(
                HttpMethod.PUT,
                "/api/teams/" + teamId + "/tasks/" + memberTaskId,
                Map.of(
                        "title", "Member should not update title",
                        "description", "full update must fail",
                        "status", "DONE",
                        "priority", "LOW",
                        "assigneeId", memberUserId),
                member.token());
        assertEquals(HttpStatus.FORBIDDEN, memberFullUpdateResponse.getStatusCode());

        ResponseEntity<String> ownerCreatePersonalTaskResponse = exchange(
                HttpMethod.POST,
                "/api/tasks",
                Map.of(
                        "title", "Owner personal task",
                        "description", "dashboard should include this",
                        "status", "TODO",
                        "priority", "LOW"),
                owner.token());
        assertEquals(HttpStatus.CREATED, ownerCreatePersonalTaskResponse.getStatusCode());

        ResponseEntity<String> ownerDashboardResponse = exchange(
                HttpMethod.GET,
                "/api/tasks?page=1&size=10",
                null,
                owner.token());
        assertEquals(HttpStatus.OK, ownerDashboardResponse.getStatusCode());
        JsonNode dashboardRecords = readBody(ownerDashboardResponse).path("data").path("records");
        assertEquals(2, dashboardRecords.size());
        assertTrue(containsTitle(dashboardRecords, "Owner personal task"));
        assertTrue(containsTitle(dashboardRecords, "Owner review"));

        ResponseEntity<String> outsiderTeamTaskResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId + "/tasks?page=1&size=10",
                null,
                outsider.token());
        assertEquals(HttpStatus.NOT_FOUND, outsiderTeamTaskResponse.getStatusCode());
    }

    @Test
    void ownerShouldNotCreateDuplicateActiveTeamNames() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("dupowner"), "abc12345");
        AuthSession anotherOwner = registerAndLogin(uniqueUsername("dupanother"), "abc12345");
        String teamName = "Duplicate Review Team";

        ResponseEntity<String> firstResponse = exchange(
                HttpMethod.POST,
                "/api/teams",
                Map.of("name", teamName),
                owner.token());
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

        ResponseEntity<String> duplicateResponse = exchange(
                HttpMethod.POST,
                "/api/teams",
                Map.of("name", "  duplicate review team  "),
                owner.token());
        assertEquals(HttpStatus.BAD_REQUEST, duplicateResponse.getStatusCode());
        assertTrue(readBody(duplicateResponse).path("message").asText().contains("团队名称"));

        ResponseEntity<String> otherOwnerResponse = exchange(
                HttpMethod.POST,
                "/api/teams",
                Map.of("name", teamName),
                anotherOwner.token());
        assertEquals(HttpStatus.CREATED, otherOwnerResponse.getStatusCode());
    }

    @Test
    void dashboardShouldOnlyShowPersonalAndAssignedTeamTasks() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("dashown"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("dashmem"), "abc12345");
        AuthSession anotherMember = registerAndLogin(uniqueUsername("dashoth"), "abc12345");

        Long teamId = readBody(exchange(
                HttpMethod.POST,
                "/api/teams",
                Map.of("name", "Dashboard Isolation Team"),
                owner.token())).path("data").path("id").asLong();
        exchange(HttpMethod.POST, "/api/teams/" + teamId + "/members", Map.of("username", member.username()), owner.token());
        exchange(HttpMethod.POST, "/api/teams/" + teamId + "/members", Map.of("username", anotherMember.username()), owner.token());

        ResponseEntity<String> teamDetailResponse = exchange(HttpMethod.GET, "/api/teams/" + teamId, null, owner.token());
        JsonNode members = readBody(teamDetailResponse).path("data").path("members");
        Long memberUserId = findUserId(members, member.username());
        Long anotherMemberUserId = findUserId(members, anotherMember.username());

        exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of("title", "Visible assigned team task", "status", "TODO", "priority", "HIGH", "assigneeId", memberUserId),
                owner.token());
        exchange(
                HttpMethod.POST,
                "/api/teams/" + teamId + "/tasks",
                Map.of("title", "Hidden other member task", "status", "TODO", "priority", "HIGH", "assigneeId", anotherMemberUserId),
                owner.token());

        ResponseEntity<String> dashboardResponse = exchange(HttpMethod.GET, "/api/tasks?page=1&size=10", null, member.token());
        assertEquals(HttpStatus.OK, dashboardResponse.getStatusCode());
        JsonNode records = readBody(dashboardResponse).path("data").path("records");
        assertTrue(containsTitle(records, "Visible assigned team task"));
        assertFalse(containsTitle(records, "Hidden other member task"));
    }

    @Test
    void dashboardShouldRejectInvalidPaginationBoundaries() throws Exception {
        AuthSession user = registerAndLogin(uniqueUsername("pageuser"), "abc12345");

        ResponseEntity<String> zeroPageResponse = exchange(HttpMethod.GET, "/api/tasks?page=0&size=10", null, user.token());
        assertEquals(HttpStatus.BAD_REQUEST, zeroPageResponse.getStatusCode());

        ResponseEntity<String> oversizedPageResponse = exchange(HttpMethod.GET, "/api/tasks?page=1&size=101", null, user.token());
        assertEquals(HttpStatus.BAD_REQUEST, oversizedPageResponse.getStatusCode());
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
