package com.lab.taskmanager.team.controller;

import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.GlobalExceptionHandler;
import com.lab.taskmanager.task.dto.TaskAssignRequest;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.task.service.TaskService;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.team.service.TeamService;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TeamPermissionTest {
    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TeamMembershipRepository teamMembershipRepository;

    @MockitoBean
    private TeamRepository teamRepository;

    @MockitoBean
    private TaskRepository taskRepository;

    @BeforeEach
    void initTestData() {
        // 创建三个角色
        User user1 = new User();
        user1.setId(100L);
        user1.setUsername("saber");
        user1.setPasswordHash("password");
        User user2 = new User();
        user2.setId(200L);
        user2.setUsername("archer");
        user2.setPasswordHash("password");
        User user3 = new User();
        user3.setId(300L);
        user3.setUsername("lancer");
        user3.setPasswordHash("password");
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        // 创建团队并指定owner
        Team team1 = new Team();
        team1.setId(100L);
        team1.setName("team1");
        team1.setOwner(user1);
        Team savedTeam = teamRepository.save(team1);

        TeamMembership ownerMembership = new TeamMembership();
        ownerMembership.setTeam(savedTeam);
        ownerMembership.setUser(user1);
        ownerMembership.setRole(TeamRole.OWNER);
        teamMembershipRepository.save(ownerMembership);

        // 分配角色
        TeamMembership membership1 = new TeamMembership();
        membership1.setTeam(team1);
        membership1.setUser(user2);
        membership1.setRole(TeamRole.ADMIN);
        teamMembershipRepository.save(membership1);
//        TeamMembership membership2 = new TeamMembership();
//        membership2.setTeam(team1);
//        membership2.setUser(user3);
//        membership2.setRole(TeamRole.MEMBER);
//        teamMembershipRepository.save(membership2);

        //添加任务
        Task task = new Task();
        task.setId(1000L);
        task.setOwner(user1);
        task.setTeam(team1);
        task.setAssignee(user1);
        task.setTitle("task1");
        task.setDescription("description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueAt(LocalDateTime.now().plusDays(7));
        taskRepository.save(task);
    }

    @Test
    void testOwnerPermission() throws Exception {
        // 浏览团队中的所有任务
        List<TeamSummaryResponse> listTeams = teamService.listTeams("saber");
        assert listTeams.size() == 1;

        // 操作分配给自己的任务，但只能修改任务状态
        Assertions.assertThrows(ForbiddenOperationException.class, () -> taskService.updateTask("saber", 1000L, new TaskUpdateRequest(
                "task1",
                " ",
                TaskStatus.DONE,
                TaskPriority.HIGH,
                LocalDateTime.now())));

        // 创建/删除/修改任务
        taskService.createTeamTask("saber", 100L, new TeamTaskCreateRequest(
                "task2",
                "description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(5),
                200L
        ));
        taskService.deleteTask("saber", 1000L);

        // 将任务分配给团队成员
        taskService.assignTask("saber",100L,1L, new TaskAssignRequest(200L));

        // 将其他用户添加到团队
        teamService.addMember("saber", 100L, new TeamMemberAddRequest("lancer"));

        // 将 Member 设置为 Admin
        teamService.updateMemberRole("saber", 100L, 300L, new TeamRoleUpdateRequest(TeamRole.ADMIN));

        // 将 Admin 重新设置为 Member
        teamService.updateMemberRole("saber", 100L, 300L, new TeamRoleUpdateRequest(TeamRole.MEMBER));
    }

    @Test
    void testAdminPermission() throws Exception {
        // 浏览团队中的所有任务
        // 操作分配给自己的任务，但只能修改任务状态
        // 创建/删除/修改任务
        // 将任务分配给团队成员

    }

    @Test
    void testMemberPermission() throws Exception {
        // 浏览团队中的所有任务
        // 操作分配给自己的任务，但只能修改任务状态

    }
}
