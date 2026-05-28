package com.lab.taskmanager.team.service;

import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.task.entity.*;
import com.lab.taskmanager.task.repository.TaskArchiveRepository;
import com.lab.taskmanager.task.repository.TaskDependencyArchiveRepository;
import com.lab.taskmanager.task.repository.TaskDependencyRepository;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.repository.TeamArchiveRepository;
import com.lab.taskmanager.team.repository.TeamMembershipArchiveRepository;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamServiceIntegrationTest {

    @Autowired
    private TeamService teamService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMembershipRepository teamMembershipRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TeamArchiveRepository teamArchiveRepository;
    @Autowired
    private TaskArchiveRepository taskArchiveRepository;
    @Autowired
    private TeamMembershipArchiveRepository teamMembershipArchiveRepository;
    @Autowired
    private TaskDependencyRepository taskDependencyRepository;
    @Autowired
    private TaskDependencyArchiveRepository taskDependencyArchiveRepository;

    @Test
    void removeMemberBehaviourTest() {
        // 插入数据
        User owner = userRepository.save(user("owner_user"));
        User admin = userRepository.save(user("admin_user"));
        User member = userRepository.save(user("member_user"));
        Team team = teamRepository.save(team("Alpha Team", owner));
        teamMembershipRepository.save(membership(team, owner, TeamRole.OWNER));
        teamMembershipRepository.save(membership(team, admin, TeamRole.ADMIN));
        teamMembershipRepository.save(membership(team, member, TeamRole.MEMBER));
        Task task = taskRepository.save(task("Test Task", "Task for testing", TaskStatus.IN_PROGRESS,
            TaskPriority.MEDIUM, LocalDateTime.now().plusDays(1), TaskScope.TEAM,
            owner, team, member));
        Task doneTask = taskRepository.save(task("Done Task", "Done Task for testing", TaskStatus.DONE,
            TaskPriority.MEDIUM, LocalDateTime.now().plusDays(1), TaskScope.TEAM,
            owner, team, member));

        // 只有Owner可以移除其他成员，其他成员只可以移除自己
        assertThrows(
            ForbiddenOperationException.class,
            () -> teamService.removeMember("admin_user", team.getId(), member.getId())
        );

        teamService.removeMember("admin_user", team.getId(), admin.getId());
        teamService.removeMember("owner_user", team.getId(), member.getId());

        assertTrue(teamMembershipRepository
            .findAllByUserId(admin.getId()).isEmpty());
        assertTrue(teamMembershipRepository
            .findAllByUserId(admin.getId()).isEmpty());

        // 被移除成员的任务清空负责人，但保留原状态（IN_PROGRESS），避免抹掉进度信息
        Task updated = taskRepository.findById(task.getId()).orElseThrow();
        assertNull(updated.getAssignee());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());

        // 在任务描述中添加原本的分配信息
        assertTrue(updated.getDescription().contains("original assignee: member_user"));

        // 已完成任务保留历史信息
        Task updatedDone = taskRepository.findById(doneTask.getId()).orElseThrow();
        assertEquals(TaskStatus.DONE, updatedDone.getStatus());
        assertEquals("member_user", updatedDone.getAssignee().getUsername());
    }

    @Test
    void disbandTeamTest() {
        User owner = userRepository.save(user("owner_user"));
        User admin = userRepository.save(user("admin_user"));
        Team team = teamRepository.save(team("Alpha Team", owner));
        TeamMembership ownerMembership = teamMembershipRepository.save(membership(team, owner, TeamRole.OWNER));
        TeamMembership adminMembership = teamMembershipRepository.save(membership(team, admin, TeamRole.ADMIN));
        Task task1 = taskRepository.save(task("Test Task1", "Task1 for testing", TaskStatus.IN_PROGRESS,
            TaskPriority.MEDIUM, LocalDateTime.now().plusDays(1), TaskScope.TEAM, owner, team, admin));
        Task task2 = taskRepository.save(task("Test Task2", "Task2 for testing", TaskStatus.DONE,
            TaskPriority.MEDIUM, LocalDateTime.now().plusDays(1), TaskScope.TEAM, owner, team, admin));
        TaskDependency dependency = taskDependencyRepository.save(taskDependency(task1, task2));

        // 只有Owner可以解散团队
        assertThrows(ForbiddenOperationException.class,
            () -> teamService.disbandTeam("admin_user", team.getId()));

        teamService.disbandTeam("owner_user", team.getId());

        // 删除后团队空间无法访问
        assertNull(teamRepository.findById(team.getId()).orElse(null));
        assertNull(taskRepository.findById(task1.getId()).orElse(null));
        assertNull(taskRepository.findById(task2.getId()).orElse(null));
        assertNull(taskDependencyRepository.findById(dependency.getId()).orElse(null));
        assertNull(teamMembershipRepository.findById(ownerMembership.getId()).orElse(null));
        assertNull(teamMembershipRepository.findById(adminMembership.getId()).orElse(null));

        // 删除后保留信息至存档库中
        assertEquals("Alpha Team", teamArchiveRepository.findByOriginalTeamId(team.getId()).orElseThrow().getName());
        assertEquals("Test Task1", taskArchiveRepository.findByOriginalTaskId(task1.getId()).orElseThrow().getTitle());
        assertEquals("Task2 for testing", taskArchiveRepository.findByOriginalTaskId(task2.getId()).orElseThrow().getDescription());
        assertEquals(1, taskDependencyArchiveRepository.findAll().size());
        assertEquals(ownerMembership.getId(),
            teamMembershipArchiveRepository.findByOriginalTeamMembershipId(ownerMembership.getId()).orElseThrow().getOriginalTeamMembershipId());
        assertEquals(adminMembership.getId(),
            teamMembershipArchiveRepository.findByOriginalTeamMembershipId(adminMembership.getId()).orElseThrow().getOriginalTeamMembershipId());
    }

    @Test
    public void ownerLeaveTest() {
        User owner = userRepository.save(user("owner_user"));
        User admin = userRepository.save(user("admin_user"));
        Team team = teamRepository.save(team("Alpha Team", owner));
        TeamMembership ownerMembership = teamMembershipRepository.save(membership(team, owner, TeamRole.OWNER));
        TeamMembership adminMembership = teamMembershipRepository.save(membership(team, admin, TeamRole.ADMIN));

        // 当团队还有其它成员时，必须指定新的Owner
        assertThrows(ForbiddenOperationException.class,
            () -> teamService.ownerLeaveTeam("owner_user", team.getId(), null)
        );

        // 指定新的Owner, 删除旧的Owne权限
        teamService.ownerLeaveTeam("owner_user", team.getId(), admin.getId());
        assertEquals(TeamRole.OWNER, teamMembershipRepository.findById(adminMembership.getId()).orElseThrow().getRole());
        assertNull(teamMembershipRepository.findById(ownerMembership.getId()).orElse(null));

        // 如果团队没有其它成员，则自动解散团队
        teamService.ownerLeaveTeam("admin_user", team.getId(), null);
        assertTrue(teamRepository.findById(team.getId()).isEmpty());
        assertTrue(teamMembershipRepository.findAllByTeamId(team.getId()).isEmpty());
    }

    private TaskDependency taskDependency(Task task1, Task task2) {
        TaskDependency dependency = new TaskDependency();
        dependency.setPredecessorTaskId(task1.getId());
        dependency.setSuccessorTaskId(task2.getId());
        return dependency;
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hashed");
        return user;
    }

    private Team team(String name, User owner) {
        Team team = new Team();
        team.setName(name);
        team.setOwner(owner);
        return team;
    }

    private Task task(String title, String description, TaskStatus status,
                      TaskPriority priority, LocalDateTime dueAt, TaskScope scope,
                      User owner, Team team, User assignee) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueAt(dueAt);
        task.setScope(scope);
        task.setOwner(owner);
        task.setTeam(team);
        task.setAssignee(assignee);
        return task;
    }

    private TeamMembership membership(Team team, User user, TeamRole role) {
        TeamMembership membership = new TeamMembership();
        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(role);
        return membership;
    }
}
