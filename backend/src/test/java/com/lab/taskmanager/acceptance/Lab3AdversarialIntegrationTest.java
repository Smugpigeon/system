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

    @Test
    void personalDependencyEndpointShouldRejectTeamTasks() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("scopeown"), "abc12345");

        Long teamId = createTeam(owner, "Dependency Scope Team");
        Long predecessorId = createTeamTask(owner, teamId, owner.userId(), "Team predecessor for scope", "DONE");
        Long successorId = createTeamTask(owner, teamId, owner.userId(), "Team successor for scope", "TODO");

        ResponseEntity<String> addThroughPersonalEndpoint = exchange(
                HttpMethod.POST,
                "/api/tasks/" + successorId + "/dependencies",
                Map.of("predecessorTaskId", predecessorId),
                owner.token());
        assertEquals(HttpStatus.NOT_FOUND, addThroughPersonalEndpoint.getStatusCode());

        ResponseEntity<String> viewThroughPersonalEndpoint = exchange(
                HttpMethod.GET,
                "/api/tasks/" + successorId + "/dependencies",
                null,
                owner.token());
        assertEquals(HttpStatus.NOT_FOUND, viewThroughPersonalEndpoint.getStatusCode());
    }

    @Test
    void personalDependencyLifecycleShouldSupportViewBlockDoneAndGuardDeletion() throws Exception {
        AuthSession user = registerAndLogin(uniqueUsername("perdep"), "abc12345");

        Long predecessorId = createPersonalTask(user, "Personal predecessor", "TODO");
        Long successorId = createPersonalTask(user, "Personal successor", "TODO");

        ResponseEntity<String> addDependencyResponse = exchange(
                HttpMethod.POST,
                "/api/tasks/" + successorId + "/dependencies",
                Map.of("predecessorTaskId", predecessorId),
                user.token());
        assertEquals(HttpStatus.CREATED, addDependencyResponse.getStatusCode());

        ResponseEntity<String> successorDependenciesResponse = exchange(
                HttpMethod.GET,
                "/api/tasks/" + successorId + "/dependencies",
                null,
                user.token());
        assertEquals(HttpStatus.OK, successorDependenciesResponse.getStatusCode());
        assertTrue(containsId(readBody(successorDependenciesResponse).path("data").path("predecessors"), predecessorId));

        ResponseEntity<String> predecessorDependenciesResponse = exchange(
                HttpMethod.GET,
                "/api/tasks/" + predecessorId + "/dependencies",
                null,
                user.token());
        assertEquals(HttpStatus.OK, predecessorDependenciesResponse.getStatusCode());
        assertTrue(containsId(readBody(predecessorDependenciesResponse).path("data").path("successors"), successorId));

        ResponseEntity<String> blockedDoneResponse = updatePersonalTask(user, successorId, "Personal successor", "DONE");
        assertEquals(HttpStatus.BAD_REQUEST, blockedDoneResponse.getStatusCode());
        assertTrue(readBody(blockedDoneResponse).path("message").asText().contains("前置任务"));

        ResponseEntity<String> blockedDeleteResponse = exchange(
                HttpMethod.DELETE,
                "/api/tasks/" + predecessorId,
                null,
                user.token());
        assertEquals(HttpStatus.BAD_REQUEST, blockedDeleteResponse.getStatusCode());
        assertTrue(readBody(blockedDeleteResponse).path("message").asText().contains("后继任务"));

        assertEquals(HttpStatus.OK, updatePersonalTask(user, predecessorId, "Personal predecessor", "DONE").getStatusCode());
        assertEquals(HttpStatus.OK, updatePersonalTask(user, successorId, "Personal successor", "DONE").getStatusCode());

        ResponseEntity<String> removeDependencyResponse = exchange(
                HttpMethod.DELETE,
                "/api/tasks/" + successorId + "/dependencies/" + predecessorId,
                null,
                user.token());
        assertEquals(HttpStatus.OK, removeDependencyResponse.getStatusCode());

        ResponseEntity<String> deleteAfterUnlinkResponse = exchange(
                HttpMethod.DELETE,
                "/api/tasks/" + predecessorId,
                null,
                user.token());
        assertEquals(HttpStatus.OK, deleteAfterUnlinkResponse.getStatusCode());
    }

    @Test
    void memberSelfLeaveShouldRemoveAccessAndUnassignOpenTasks() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("selfown"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("selfmem"), "abc12345");

        Long teamId = createTeam(owner, "Self Leave Team");
        addMember(owner, teamId, member.username());
        createTeamTask(owner, teamId, member.userId(), "Open task before self leave", "IN_PROGRESS");

        ResponseEntity<String> leaveResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/members/" + member.userId(),
                null,
                member.token());
        assertEquals(HttpStatus.OK, leaveResponse.getStatusCode());

        ResponseEntity<String> memberTeamDetailResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId,
                null,
                member.token());
        assertEquals(HttpStatus.NOT_FOUND, memberTeamDetailResponse.getStatusCode());

        ResponseEntity<String> memberTeamTaskResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId + "/tasks",
                null,
                member.token());
        assertEquals(HttpStatus.NOT_FOUND, memberTeamTaskResponse.getStatusCode());

        ResponseEntity<String> ownerTeamTaskResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId + "/tasks",
                null,
                owner.token());
        assertEquals(HttpStatus.OK, ownerTeamTaskResponse.getStatusCode());
        JsonNode task = findRecordByTitle(
                readBody(ownerTeamTaskResponse).path("data").path("records"),
                "Open task before self leave");
        assertTrue(task.path("assigneeId").isNull());
        assertEquals("TODO", task.path("status").asText());
    }

    @Test
    void disbandTeamShouldCloseNormalTeamSpaceAndTaskAccess() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("disown"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("dismem"), "abc12345");

        Long teamId = createTeam(owner, "Disband Team");
        addMember(owner, teamId, member.username());
        createTeamTask(owner, teamId, member.userId(), "Task before disband", "TODO");

        ResponseEntity<String> disbandResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId,
                null,
                owner.token());
        assertEquals(HttpStatus.OK, disbandResponse.getStatusCode());

        ResponseEntity<String> ownerTeamDetailResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId,
                null,
                owner.token());
        assertEquals(HttpStatus.NOT_FOUND, ownerTeamDetailResponse.getStatusCode());

        ResponseEntity<String> memberTeamTaskResponse = exchange(
                HttpMethod.GET,
                "/api/teams/" + teamId + "/tasks",
                null,
                member.token());
        assertEquals(HttpStatus.NOT_FOUND, memberTeamTaskResponse.getStatusCode());
    }

    @Test
    void disbandTeamShouldHandleUnassignedTasksAfterMemberLeaves() throws Exception {
        AuthSession owner = registerAndLogin(uniqueUsername("unassown"), "abc12345");
        AuthSession member = registerAndLogin(uniqueUsername("unassmem"), "abc12345");

        Long teamId = createTeam(owner, "Unassigned Disband Team");
        addMember(owner, teamId, member.username());
        createTeamTask(owner, teamId, member.userId(), "Task that will be unassigned", "IN_PROGRESS");

        ResponseEntity<String> leaveResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId + "/members/" + member.userId(),
                null,
                member.token());
        assertEquals(HttpStatus.OK, leaveResponse.getStatusCode());

        ResponseEntity<String> disbandResponse = exchange(
                HttpMethod.DELETE,
                "/api/teams/" + teamId,
                null,
                owner.token());
        assertEquals(HttpStatus.OK, disbandResponse.getStatusCode());
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

    private Long createPersonalTask(AuthSession user, String title, String status) throws Exception {
        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/api/tasks",
                Map.of(
                        "title", title,
                        "description", "created by adversarial test",
                        "status", status,
                        "priority", "MEDIUM"),
                user.token());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return readBody(response).path("data").path("id").asLong();
    }

    private ResponseEntity<String> updatePersonalTask(
            AuthSession user,
            Long taskId,
            String title,
            String status) {
        return exchange(
                HttpMethod.PUT,
                "/api/tasks/" + taskId,
                Map.of(
                        "title", title,
                        "description", "updated by adversarial test",
                        "status", status,
                        "priority", "MEDIUM"),
                user.token());
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

    private boolean containsId(JsonNode records, Long id) {
        for (JsonNode record : records) {
            if (record.path("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findRecordByTitle(JsonNode records, String title) {
        for (JsonNode record : records) {
            if (title.equals(record.path("title").asText())) {
                return record;
            }
        }
        throw new IllegalStateException("Record not found: " + title);
    }

    private record AuthSession(Long userId, String username, String token) {
    }
}
