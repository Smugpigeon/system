package com.lab.taskmanager.team.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamDetailResponse;
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
import com.lab.taskmanager.user.service.UserService;
import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
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
