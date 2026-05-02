package com.lab.taskmanager.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.taskmanager.auth.security.CustomUserDetailsService;
import com.lab.taskmanager.auth.security.JwtService;
import com.lab.taskmanager.common.exception.GlobalExceptionHandler;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskAssignRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.service.TaskService;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamDetailResponse;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamMemberResponse;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void testCreateTeam() throws Exception {
        TeamSummaryResponse response = new TeamSummaryResponse(10L, "Dev Team", TeamRole.OWNER, 3, 5);
        when(teamService.createTeam(eq("alice"), any())).thenReturn(response);

        String requestBody = objectMapper.writeValueAsString(new TeamCreateRequest("Dev Team"));

        mockMvc.perform(post("/api/teams")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队创建成功"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Dev Team"))
                .andExpect(jsonPath("$.data.currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(3))
                .andExpect(jsonPath("$.data.teamTaskCount").value(5));
    }

    @Test
    void testListTeams() throws Exception {
        List<TeamSummaryResponse> teams = List.of(
                new TeamSummaryResponse(1L, "Alpha", TeamRole.MEMBER, 2, 1),
                new TeamSummaryResponse(2L, "Beta", TeamRole.ADMIN, 5, 3)
        );
        when(teamService.listTeams("alice")).thenReturn(teams);

        mockMvc.perform(get("/api/teams").principal(() -> "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队列表获取成功"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Alpha"))
                .andExpect(jsonPath("$.data[0].currentUserRole").value("MEMBER"))
                .andExpect(jsonPath("$.data[0].memberCount").value(2))
                .andExpect(jsonPath("$.data[0].teamTaskCount").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].currentUserRole").value("ADMIN"))
                .andExpect(jsonPath("$.data[1].memberCount").value(5))
                .andExpect(jsonPath("$.data[1].teamTaskCount").value(3));
    }

    @Test
    void testGetTeamDetail() throws Exception {
        List<TeamMemberResponse> members = List.of(
                new TeamMemberResponse(101L, "alice", TeamRole.OWNER),
                new TeamMemberResponse(102L, "saber", TeamRole.MEMBER)
        );
        TeamDetailResponse detail = new TeamDetailResponse(5L, "Gamma", TeamRole.ADMIN, members);

        when(teamService.getTeamDetail("alice", 5L)).thenReturn(detail);

        mockMvc.perform(get("/api/teams/5").principal(() -> "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队详情获取成功"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Gamma"))
                .andExpect(jsonPath("$.data.currentUserRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.members[0].username").value("alice"))
                .andExpect(jsonPath("$.data.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data.members[1].username").value("saber"))
                .andExpect(jsonPath("$.data.members[1].role").value("MEMBER"));
    }

    @Test
    void testAddMember() throws Exception {
        TeamMemberResponse response = new TeamMemberResponse(200L, "saber", TeamRole.MEMBER);
        when(teamService.addMember(eq("alice"), eq(7L), any())).thenReturn(response);

        String requestBody = objectMapper.writeValueAsString(new TeamMemberAddRequest("saber"));

        mockMvc.perform(post("/api/teams/7/members")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队成员添加成功"))
                .andExpect(jsonPath("$.data.userId").value(200))
                .andExpect(jsonPath("$.data.username").value("saber"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    @Test
    void testUpdateMemberRole() throws Exception {
        TeamMemberResponse response = new TeamMemberResponse(200L, "saber", TeamRole.ADMIN);
        when(teamService.updateMemberRole(eq("alice"), eq(7L), eq(200L), any())).thenReturn(response);

        String requestBody = objectMapper.writeValueAsString(new TeamRoleUpdateRequest(TeamRole.ADMIN));

        mockMvc.perform(put("/api/teams/7/members/200/role")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队角色更新成功"))
                .andExpect(jsonPath("$.data.userId").value(200))
                .andExpect(jsonPath("$.data.username").value("saber"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void testCreateTeamTask() throws Exception {
        TaskResponse response = buildTaskResponse(300L, "team task");
        when(taskService.createTeamTask(eq("alice"), eq(7L), any())).thenReturn(response);

        TeamTaskCreateRequest request = new TeamTaskCreateRequest(
                "team task",
                "implement feature",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                null,
                200L);

        mockMvc.perform(post("/api/teams/7/tasks")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队任务创建成功"))
                .andExpect(jsonPath("$.data.id").value(300))
                .andExpect(jsonPath("$.data.title").value("team task"))
                .andExpect(jsonPath("$.data.teamId").value(7))
                .andExpect(jsonPath("$.data.teamName").value("Design Team"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void testGetTeamTasks() throws Exception {
        PageRequest pageRequest = PageRequest.of(10, 1, "updatedAt");
        List<TaskResponse> tasks = List.of(buildTaskResponse(301L, "first task"));
        PageResult<TaskResponse> pageResult = new PageResult<>(1, 1, 1, 10, tasks);

        when(taskService.getTeamTasks(eq("alice"), eq(7L), eq(TaskStatus.TODO), eq(TaskPriority.HIGH), eq("backend"), eq(pageRequest)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/teams/7/tasks")
                        .principal(() -> "alice")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("keyword", "backend")
                        .param("sortBy", "updatedAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队任务列表获取成功"))
                .andExpect(jsonPath("$.data.totalRecords").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.currPage").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records[0].id").value(301))
                .andExpect(jsonPath("$.data.records[0].teamId").value(7))
                .andExpect(jsonPath("$.data.records[0].teamName").value("Design Team"));
    }

    @Test
    void testAssignTask() throws Exception {
        TaskResponse response = buildTaskResponse(302L, "assigned task");
        when(taskService.assignTask(eq("alice"), eq(7L), eq(400L), any())).thenReturn(response);

        String requestBody = objectMapper.writeValueAsString(new TaskAssignRequest(200L));

        mockMvc.perform(put("/api/teams/7/tasks/400/assign")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("任务分配成功"))
                .andExpect(jsonPath("$.data.id").value(302))
                .andExpect(jsonPath("$.data.title").value("assigned task"))
                .andExpect(jsonPath("$.data.teamId").value(7))
                .andExpect(jsonPath("$.data.teamName").value("Design Team"));
    }

    private TaskResponse buildTaskResponse(Long id, String title) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 15, 10);
        return new TaskResponse(
                id,
                title,
                "sample description",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                now.plusDays(1),
                now.minusDays(1),
                now,
                7L,
                "Design Team",
                100L,
                "alice",
                200L,
                "saber",
                TeamRole.MEMBER);
    }
}
