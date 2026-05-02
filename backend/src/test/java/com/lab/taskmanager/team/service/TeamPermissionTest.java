package com.lab.taskmanager.team.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskAssignRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.task.service.TaskService;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamMemberResponse;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamPermissionTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User owner;
    private User admin;
    private User member;
    private User outsider;
    private Team team;
    private Task ownerTask;
    private Task memberTask;

    @BeforeEach
    void initTestData() {
        owner = saveUser("saber");
        admin = saveUser("archer");
        member = saveUser("lancer");
        outsider = saveUser("nova");

        team = new Team();
        team.setName("team1");
        team.setOwner(owner);
        team = teamRepository.save(team);

        teamMembershipRepository.save(teamMembership(team, owner, TeamRole.OWNER));
        teamMembershipRepository.save(teamMembership(team, admin, TeamRole.ADMIN));
        teamMembershipRepository.save(teamMembership(team, member, TeamRole.MEMBER));

        ownerTask = new Task();
        ownerTask.setOwner(owner);
        ownerTask.setTeam(team);
        ownerTask.setAssignee(owner);
        ownerTask.setTitle("owner-task");
        ownerTask.setDescription("owner description");
        ownerTask.setStatus(TaskStatus.TODO);
        ownerTask.setPriority(TaskPriority.MEDIUM);
        ownerTask.setDueAt(LocalDateTime.of(2026, 3, 29, 15, 10));
        ownerTask = taskRepository.save(ownerTask);

        memberTask = new Task();
        memberTask.setOwner(owner);
        memberTask.setTeam(team);
        memberTask.setAssignee(member);
        memberTask.setTitle("member-task");
        memberTask.setDescription("member description");
        memberTask.setStatus(TaskStatus.TODO);
        memberTask.setPriority(TaskPriority.MEDIUM);
        memberTask.setDueAt(LocalDateTime.of(2026, 3, 30, 15, 10));
        memberTask = taskRepository.save(memberTask);
    }

    @Test
    void ownerShouldManageTeamAndTeamTasks() {
        // 浏览团队中的所有任务
        List<TeamSummaryResponse> teams = teamService.listTeams(owner.getUsername());
        assertEquals(1, teams.size());
        assertEquals(team.getId(), teams.get(0).id());
        assertEquals(TeamRole.OWNER, teams.get(0).currentUserRole());
        assertEquals(3, teams.get(0).memberCount());

        var detail = teamService.getTeamDetail(owner.getUsername(), team.getId());
        assertEquals(team.getId(), detail.id());
        assertEquals(TeamRole.OWNER, detail.currentUserRole());
        assertEquals(3, detail.members().size());
        assertEquals("saber", detail.members().get(0).username());
        assertEquals(TeamRole.OWNER, detail.members().get(0).role());

        // 将其他用户添加到团队
        TeamMemberResponse addedMember = teamService.addMember(
                owner.getUsername(),
                team.getId(),
                new TeamMemberAddRequest(outsider.getUsername()));
        assertEquals(outsider.getId(), addedMember.userId());
        assertEquals(TeamRole.MEMBER, addedMember.role());

        // 将 Admin 设置为 Member
        TeamMemberResponse demotedAdmin = teamService.updateMemberRole(
                owner.getUsername(),
                team.getId(),
                admin.getId(),
                new TeamRoleUpdateRequest(TeamRole.MEMBER));
        assertEquals(admin.getId(), demotedAdmin.userId());
        assertEquals(TeamRole.MEMBER, demotedAdmin.role());

        // 将 Member 设置为 Admin
        TeamMemberResponse promotedAdmin = teamService.updateMemberRole(
                owner.getUsername(),
                team.getId(),
                admin.getId(),
                new TeamRoleUpdateRequest(TeamRole.ADMIN));
        assertEquals(TeamRole.ADMIN, promotedAdmin.role());

        // 创建任务
        TaskResponse createdTask = taskService.createTeamTask(
                owner.getUsername(),
                team.getId(),
                new TeamTaskCreateRequest(
                        "owner-created-team-task",
                        "created by owner",
                        TaskStatus.TODO,
                        TaskPriority.HIGH,
                        LocalDateTime.of(2026, 4, 2, 15, 10),
                        member.getId()));
        assertNotNull(createdTask.id());
        assertEquals(team.getId(), createdTask.teamId());
        assertEquals(team.getName(), createdTask.teamName());
        assertEquals(member.getId(), createdTask.assigneeId());
        assertEquals(TeamRole.MEMBER, createdTask.assigneeRole());

        // 修改任务
        TaskResponse updatedTask = taskService.updateTask(
                owner.getUsername(),
                memberTask.getId(),
                new TaskUpdateRequest(
                        "member-task-updated",
                        "owner updated",
                        TaskStatus.DONE,
                        TaskPriority.HIGH,
                        LocalDateTime.of(2026, 4, 3, 15, 10)));
        assertEquals("member-task-updated", updatedTask.title());
        assertEquals(TaskStatus.DONE, updatedTask.status());
        assertEquals(TaskPriority.HIGH, updatedTask.priority());

        // 将任务分配给团队成员
        TaskResponse reassignedTask = taskService.assignTask(
                owner.getUsername(),
                team.getId(),
                ownerTask.getId(),
                new TaskAssignRequest(member.getId()));
        assertEquals(member.getId(), reassignedTask.assigneeId());

        // 删除任务
        assertDoesNotThrow(() -> taskService.deleteTask(owner.getUsername(), ownerTask.getId()));
        taskRepository.flush();
        assertFalse(taskRepository.existsById(ownerTask.getId()));
    }

    @Test
    void adminShouldManageTeamTasksButNotMembers() {
        List<TeamSummaryResponse> teams = teamService.listTeams(admin.getUsername());
        assertEquals(1, teams.size());
        assertEquals(TeamRole.ADMIN, teams.get(0).currentUserRole());

        TaskResponse createdTask = taskService.createTeamTask(
                admin.getUsername(),
                team.getId(),
                new TeamTaskCreateRequest(
                        "admin-created-task",
                        "created by admin",
                        TaskStatus.TODO,
                        TaskPriority.HIGH,
                        LocalDateTime.of(2026, 4, 4, 15, 10),
                        member.getId()));
        assertEquals(team.getId(), createdTask.teamId());
        assertEquals(member.getId(), createdTask.assigneeId());

        TaskResponse updatedTask = taskService.updateTask(
                admin.getUsername(),
                createdTask.id(),
                new TaskUpdateRequest(
                        "admin-updated-task",
                        "admin updated",
                        TaskStatus.IN_PROGRESS,
                        TaskPriority.MEDIUM,
                        LocalDateTime.of(2026, 4, 5, 15, 10)));
        assertEquals("admin-updated-task", updatedTask.title());
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.status());

        TaskResponse reassignedTask = taskService.assignTask(
                admin.getUsername(),
                team.getId(),
                createdTask.id(),
                new TaskAssignRequest(owner.getId()));
        assertEquals(owner.getId(), reassignedTask.assigneeId());

        assertDoesNotThrow(() -> taskService.deleteTask(admin.getUsername(), createdTask.id()));

        // Admin 没有权限管理团队成员
        ForbiddenOperationException addMemberEx = assertThrows(
                ForbiddenOperationException.class,
                () -> teamService.addMember(
                        admin.getUsername(),
                        team.getId(),
                        new TeamMemberAddRequest(outsider.getUsername())));
        assertEquals("只有团队拥有者可以执行该操作", addMemberEx.getMessage());

        ForbiddenOperationException updateRoleEx = assertThrows(
                ForbiddenOperationException.class,
                () -> teamService.updateMemberRole(
                        admin.getUsername(),
                        team.getId(),
                        member.getId(),
                        new TeamRoleUpdateRequest(TeamRole.ADMIN)));
        assertEquals("只有团队拥有者可以执行该操作", updateRoleEx.getMessage());
    }

    @Test
    void memberShouldOnlyEditTheirOwnAssignedTaskStatus() {
        List<TeamSummaryResponse> teams = teamService.listTeams(member.getUsername());
        assertEquals(1, teams.size());
        assertEquals(TeamRole.MEMBER, teams.get(0).currentUserRole());

        PageResult<TaskResponse> page = taskService.getTeamTasks(
                member.getUsername(),
                team.getId(),
                null,
                null,
                null,
                PageRequest.of(10, 1, "updatedAt"));
        assertEquals(2, page.getTotalRecords());
        assertEquals(2, page.getRecords().size());

        // Member可以修改任务状态
        TaskResponse statusUpdated = taskService.updateTask(
                member.getUsername(),
                memberTask.getId(),
                new TaskUpdateRequest(
                        "member-task",
                        null,
                        TaskStatus.DONE,
                        null,
                        null));
        assertEquals(TaskStatus.DONE, statusUpdated.status());
        assertEquals(member.getId(), statusUpdated.assigneeId());

        // 不能修改未分配给自己的任务
        ForbiddenOperationException otherTaskEx = assertThrows(
                ForbiddenOperationException.class,
                () -> taskService.updateTask(
                        member.getUsername(),
                        ownerTask.getId(),
                        new TaskUpdateRequest(
                                "owner-task",
                                null,
                                TaskStatus.DONE,
                                null,
                                null)));
        assertEquals("只能修改分配给你自己的任务", otherTaskEx.getMessage());

        // Member 不能修改任务状态外的其它属性
        ForbiddenOperationException detailEditEx = assertThrows(
                ForbiddenOperationException.class,
                () -> taskService.updateTask(
                        member.getUsername(),
                        memberTask.getId(),
                        new TaskUpdateRequest(
                                "member-task-renamed",
                                "not allowed",
                                TaskStatus.DONE,
                                TaskPriority.HIGH,
                                LocalDateTime.of(2026, 4, 6, 15, 10))));
        assertEquals("成员只能修改任务状态，不能修改标题、描述等其他字段", detailEditEx.getMessage());

        // Member 没有权限创建团队任务、分配任务和删除任务
        ForbiddenOperationException createTeamTaskEx = assertThrows(
                ForbiddenOperationException.class,
                () -> taskService.createTeamTask(
                        member.getUsername(),
                        team.getId(),
                        new TeamTaskCreateRequest(
                                "member-created-task",
                                "should fail",
                                TaskStatus.TODO,
                                TaskPriority.LOW,
                                LocalDateTime.of(2026, 4, 7, 15, 10),
                                owner.getId())));
        assertEquals("只有团队管理员或拥有者可以执行该操作", createTeamTaskEx.getMessage());

        ForbiddenOperationException assignTaskEx = assertThrows(
                ForbiddenOperationException.class,
                () -> taskService.assignTask(
                        member.getUsername(),
                        team.getId(),
                        ownerTask.getId(),
                        new TaskAssignRequest(owner.getId())));
        assertEquals("只有团队管理员或拥有者可以执行该操作", assignTaskEx.getMessage());

        ForbiddenOperationException deleteTaskEx = assertThrows(
                ForbiddenOperationException.class,
                () -> taskService.deleteTask(member.getUsername(), memberTask.getId()));
        assertEquals("只有团队管理员或拥有者可以执行该操作", deleteTaskEx.getMessage());

        // Member 没有权限管理团队成员
        ForbiddenOperationException addMemberEx = assertThrows(
                ForbiddenOperationException.class,
                () -> teamService.addMember(
                        member.getUsername(),
                        team.getId(),
                        new TeamMemberAddRequest(outsider.getUsername())));
        assertEquals("只有团队拥有者可以执行该操作", addMemberEx.getMessage());

        ForbiddenOperationException updateRoleEx = assertThrows(
                ForbiddenOperationException.class,
                () -> teamService.updateMemberRole(
                        member.getUsername(),
                        team.getId(),
                        admin.getId(),
                        new TeamRoleUpdateRequest(TeamRole.MEMBER)));
        assertEquals("只有团队拥有者可以执行该操作", updateRoleEx.getMessage());
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("password");
        return userRepository.save(user);
    }

    private TeamMembership teamMembership(Team team, User user, TeamRole role) {
        TeamMembership membership = new TeamMembership();
        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(role);
        return membership;
    }
}
