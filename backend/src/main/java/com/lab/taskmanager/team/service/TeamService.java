package com.lab.taskmanager.team.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.entity.*;
import com.lab.taskmanager.task.repository.TaskArchiveRepository;
import com.lab.taskmanager.task.repository.TaskDependencyArchiveRepository;
import com.lab.taskmanager.task.repository.TaskDependencyRepository;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamDetailResponse;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamMemberResponse;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.entity.*;
import com.lab.taskmanager.team.repository.TeamArchiveRepository;
import com.lab.taskmanager.team.repository.TeamMembershipArchiveRepository;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.repository.UserRepository;
import com.lab.taskmanager.user.service.UserService;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TaskRepository taskRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TeamMembershipArchiveRepository teamMembershipArchiveRepository;
    private final TeamArchiveRepository teamArchiveRepository;
    private final TaskArchiveRepository taskArchiveRepository;
    private final TaskDependencyArchiveRepository taskDependencyArchiveRepository;

    /**
     * Create a new team and automatically register the creator as OWNER.
     *
     * @param username authenticated username
     * @param request create team request
     * @return persisted team summary for the creator
     */
    @Transactional
    public TeamSummaryResponse createTeam(@NotNull String username, @NotNull TeamCreateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);

        Team team = new Team();
        team.setName(request.name().trim());
        team.setOwner(currentUser);
        Team savedTeam = teamRepository.save(team);

        TeamMembership ownerMembership = new TeamMembership();
        ownerMembership.setTeam(savedTeam);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(TeamRole.OWNER);
        teamMembershipRepository.save(ownerMembership);

        return new TeamSummaryResponse(savedTeam.getId(), savedTeam.getName(), TeamRole.OWNER, 1, 0);
    }

    /**
     * List all teams visible to the current user.
     *
     * @param username authenticated username
     * @return sorted team summaries for the current user
     */
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams(@NotNull String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        return teamMembershipRepository.findAllByUserId(currentUser.getId())
                .stream()
                .sorted(Comparator.comparing(membership -> membership.getTeam().getName().toLowerCase()))
                .map(this::toSummary)
                .toList();
    }

    /**
     * Fetch one team and its members after membership verification.
     *
     * @param username authenticated username
     * @param teamId target team identifier
     * @return full team detail with current user's role and member list
     */
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(@NotNull String username, @NotNull Long teamId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership currentMembership = teamAuthorizationService.requireMembership(teamId, currentUser.getId());
        List<TeamMemberResponse> members = teamMembershipRepository.findAllByTeamId(teamId)
                .stream()
                .sorted(Comparator
                        .comparing((TeamMembership membership) -> membership.getRole().ordinal())
                        .thenComparing(membership -> membership.getUser().getUsername().toLowerCase()))
                .map(this::toMemberResponse)
                .toList();

        Team team = currentMembership.getTeam();
        return new TeamDetailResponse(team.getId(), team.getName(), currentMembership.getRole(), members);
    }

    /**
     * Add a new member into the target team. Only Owner can call this method.
     *
     * @param username authenticated username
     * @param teamId target team identifier
     * @param request target member payload
     * @return created membership view
     */
    @Transactional
    public TeamMemberResponse addMember(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull TeamMemberAddRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership ownerMembership = teamAuthorizationService.requireOwner(teamId, currentUser.getId());
        User targetUser = userService.findByUsernameOrThrow(request.username().trim());

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BusinessException("团队拥有者已经在团队中，无需重复添加");
        }
        if (teamMembershipRepository.existsByTeamIdAndUserId(teamId, targetUser.getId())) {
            throw new BusinessException("该用户已经是团队成员");
        }

        TeamMembership membership = new TeamMembership();
        membership.setTeam(ownerMembership.getTeam());
        membership.setUser(targetUser);
        membership.setRole(TeamRole.MEMBER);
        TeamMembership savedMembership = teamMembershipRepository.save(membership);
        return toMemberResponse(savedMembership);
    }

    /**
     * Update one member's team role between Member and Admin. Owner role is immutable here.
     *
     * @param username authenticated username
     * @param teamId target team identifier
     * @param targetUserId user to update
     * @param request desired role payload
     * @return updated membership view
     */
    @Transactional
    public TeamMemberResponse updateMemberRole(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long targetUserId,
            @NotNull TeamRoleUpdateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireOwner(teamId, currentUser.getId());

        TeamMembership targetMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("团队成员不存在"));

        if (targetMembership.getRole() == TeamRole.OWNER) {
            throw new ForbiddenOperationException("不能修改团队拥有者的角色");
        }
        if (request.role() == TeamRole.OWNER) {
            throw new BusinessException("Lab2 当前版本不支持转移团队拥有者角色");
        }

        targetMembership.setRole(request.role());
        TeamMembership savedMembership = teamMembershipRepository.save(targetMembership);
        return toMemberResponse(savedMembership);
    }

    /**
     * Remove one member from the team. Only Owner can remove others, Admin or Member can remove themselves.
     *
     * @param username authenticated username
     * @param teamId target team identifier
     * @param targetUserId removed user identifier
     */
    @Transactional
    public void removeMember(@NotNull String username, @NotNull Long teamId, @NotNull Long targetUserId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        TeamMembership currentUserMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("当前用户不是团队成员"));
        TeamMembership targetUserMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("团队成员不存在"));

        if (targetUserMembership.getRole() == TeamRole.OWNER) {
            throw new ForbiddenOperationException("不能移除团队拥有者");
        } else if (currentUserMembership.getRole() == TeamRole.OWNER) {
            closeMembership(teamId, targetUserId);
            handleDepartedTask(teamId, targetUser);
        } else if (currentUser.getId() == targetUserId){
            closeMembership(teamId, targetUserId);
            handleDepartedTask(teamId, targetUser);
        } else {
            throw new ForbiddenOperationException("只有Owner可以移除其他成员，Admin和Member只能移除自己");
        }
    }

    /**
     * Disband an active team. Only Owner can disband the team, and the team space will be inaccessible after disbanding.
     * All related data will be deleted and archived for potential future audit.
     *
     * @param username authenticated username
     * @param teamId target team identifier
     */
    @Transactional
    public void disbandTeam(String username, Long teamId) {

        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership ownerMembership =
            teamAuthorizationService.requireOwner(teamId, currentUser.getId());

        Team team = ownerMembership.getTeam();

        // 1. 归档 Team
        teamArchiveRepository.save(toTeamArchive(team));

        // 2. 归档并删除 Task
        List<Task> tasks = taskRepository.findAllByTeamId(teamId);

        for (Task task : tasks) {

            // 2.1 先归档依赖
            archiveTaskDependencies(task.getId());

            // 2.2 再归档任务
            taskArchiveRepository.save(toTaskArchive(task));

            // 2.3 最后删除任务
            taskRepository.delete(task);
        }

        // 3. 归档并删除 Memberships
        List<TeamMembership> memberships =
            teamMembershipRepository.findAllByTeamId(teamId);

        for (TeamMembership membership : memberships) {
            teamMembershipArchiveRepository.save(
                toTeamMembershipArchive(membership));
            teamMembershipRepository.delete(membership);
        }

        // 4. 删除 Team
        teamRepository.delete(team);
    }

    @Transactional
    public void ownerLeaveTeam(String username, Long teamId, Long newOwnerId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireOwner(teamId, currentUser.getId());

        if(newOwnerId == null){
            if(hasNonOwnerMembers(teamId)){
                // 还有其它成员，必须指定新的Owner
                throw new ForbiddenOperationException("需要指定新的Owner");
            } else {
                // 没有其它团队成员，自动解散团队
                disbandTeam(username, teamId);
            }
        } else {
            TeamMembership newOwnerMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, newOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("新的Owner必须是团队成员"));
            if (newOwnerMembership.getRole() == TeamRole.OWNER) {
                throw new ForbiddenOperationException("不能指定自己为新的Owner");
            }

            // 删除旧Owner权限
            closeMembership(teamId, currentUser.getId());

            // 更新新Owner权限
            newOwnerMembership.setRole(TeamRole.OWNER);
            teamMembershipRepository.save(newOwnerMembership);
        }
    }

    private boolean hasNonOwnerMembers(Long teamId) {
        return teamMembershipRepository.findAllByTeamId(teamId)
            .stream()
            .anyMatch(membership -> membership.getRole() != TeamRole.OWNER);
    }

    private TeamMembershipArchive toTeamMembershipArchive(TeamMembership membership) {
        TeamMembershipArchive archive = new TeamMembershipArchive();

        archive.setOriginalTeamMembershipId(membership.getId());
        archive.setTeamId(membership.getTeam().getId());
        archive.setUserId(membership.getUser().getId());
        archive.setRole(membership.getRole());
        archive.setArchivedAt(LocalDateTime.now());

        return archive;
    }

    private TeamArchive toTeamArchive(Team team) {
        TeamArchive teamArchive = new TeamArchive();

        teamArchive.setOriginalTeamId(team.getId());
        teamArchive.setName(team.getName());
        teamArchive.setOwnerId(team.getOwner() != null ? team.getOwner().getId() : null);
        teamArchive.setArchivedAt(LocalDateTime.now());

        return teamArchive;
    }

    private TaskArchive toTaskArchive(Task task) {
        TaskArchive archive = new TaskArchive();

        archive.setOriginalTaskId(task.getId());
        archive.setTitle(task.getTitle());
        archive.setDescription(task.getDescription());
        archive.setStatus(task.getStatus());
        archive.setPriority(task.getPriority());
        archive.setDueAt(task.getDueAt());
        archive.setScope(task.getScope());

        archive.setTeamId(task.getTeam() != null ? task.getTeam().getId() : null);
        archive.setOwnerId(task.getOwner() != null ? task.getOwner().getId() : null);
        archive.setAssigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null);

        archive.setArchivedAt(LocalDateTime.now());
        return archive;
    }

    private void archiveTaskDependencies(Long taskId) {

        List<TaskDependency> deps =
            Stream.concat(
                taskDependencyRepository
                    .findAllByPredecessorTaskId(taskId)
                    .stream(),
                taskDependencyRepository
                    .findAllBySuccessorTaskId(taskId)
                    .stream()
            ).distinct().toList();

        for (TaskDependency dep : deps) {
            TaskDependencyArchive archive = new TaskDependencyArchive();
            archive.setOriginalDependencyId(dep.getId());
            archive.setPredecessorTaskId(dep.getPredecessorTaskId());
            archive.setSuccessorTaskId(dep.getSuccessorTaskId());
            archive.setArchivedAt(LocalDateTime.now());

            taskDependencyArchiveRepository.save(archive);
            taskDependencyRepository.delete(dep);
        }
    }

    private TeamSummaryResponse toSummary(TeamMembership membership) {
        Team team = membership.getTeam();
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                membership.getRole(),
                Math.toIntExact(teamMembershipRepository.countByTeamId(team.getId())),
                Math.toIntExact(taskRepository.countByTeamId(team.getId())));
    }

    private TeamMemberResponse toMemberResponse(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getUsername(),
                membership.getRole());
    }

    private void closeMembership(Long teamId, Long userId) {
        TeamMembership target = teamMembershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("团队成员不存在"));
        teamMembershipRepository.delete(target);
    }

    private void handleDepartedTask(Long teamId, User assignee) {
        List<Task> departedTask = taskRepository.findAllByTeamIdAndAssigneeId(teamId, assignee.getId());
        for(Task task : departedTask) {
            if(task.getStatus() == TaskStatus.DONE){
                continue;
            }
            task.setAssignee(null);
            task.setStatus(TaskStatus.TODO);
            String description = task.getDescription();
            description += "\n\noriginal assignee: " + assignee.getUsername();
            task.setDescription(description);
            taskRepository.save(task);
        }
    }
}
