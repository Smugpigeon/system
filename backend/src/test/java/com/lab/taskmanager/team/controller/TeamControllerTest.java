package com.lab.taskmanager.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.taskmanager.auth.security.CustomUserDetailsService;
import com.lab.taskmanager.auth.security.JwtService;
import com.lab.taskmanager.common.exception.GlobalExceptionHandler;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamDetailResponse;
import com.lab.taskmanager.team.dto.TeamMemberResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void testlistTeams() throws Exception {
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
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].currentUserRole").value("ADMIN"));
    }

    @Test
    void testgetTeamDetail() throws Exception {
        List<TeamMemberResponse> members = List.of(
                new TeamMemberResponse(101L, "alice", TeamRole.OWNER),
                new TeamMemberResponse(102L, "bob", TeamRole.MEMBER)
        );
        TeamDetailResponse detail = new TeamDetailResponse(5L, "Gamma", TeamRole.ADMIN, members);

        when(teamService.getTeamDetail("alice", 5L)).thenReturn(detail);

        mockMvc.perform(get("/api/teams/5").principal(() -> "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("团队详情获取成功"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Gamma"))
                .andExpect(jsonPath("$.data.members[0].username").value("alice"));
    }
}
