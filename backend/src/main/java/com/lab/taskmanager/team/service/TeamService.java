package com.lab.taskmanager.team.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
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
import com.lab.taskmanager.user.service.UserService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final UserService userService;

    /**
     * Create a new team and register the creator as team owner.
     *
     * @param username authenticated username
     * @param request team creation payload
     * @return summary of the created team
     */
    @Transactional
    public TeamSummaryResponse createTeam(String username, TeamCreateRequest request) {
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
     * List all teams joined by the current user.
     *
     * @param username authenticated username
     * @return sorted team summaries visible to the current user
     */
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams(String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        return teamMembershipRepository.findAllByUserId(currentUser.getId())
                .stream()
                .sorted(Comparator.comparing(membership -> membership.getTeam().getName().toLowerCase()))
                .map(this::toSummary)
                .toList();
    }

    /**
     * Load one team together with current user's role and the full member list.
     *
     * @param username authenticated username
     * @param teamId target team id
     * @return team detail payload
     */
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(String username, Long teamId) {
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
     * Add one new member into the current team. Only owner can do this.
     *
     * @param username authenticated username
     * @param teamId target team id
     * @param request member add payload
     * @return created member payload
     */
    @Transactional
    public TeamMemberResponse addMember(String username, Long teamId, TeamMemberAddRequest request) {
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
     * Switch one team member between Member and Admin. Owner role is immutable.
     *
     * @param username authenticated username
     * @param teamId target team id
     * @param targetUserId target user id
     * @param request desired role payload
     * @return updated member payload
     */
    @Transactional
    public TeamMemberResponse updateMemberRole(
            String username,
            Long teamId,
            Long targetUserId,
            TeamRoleUpdateRequest request) {
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

    private TeamSummaryResponse toSummary(TeamMembership membership) {
        Team team = membership.getTeam();
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                membership.getRole(),
                Math.toIntExact(teamMembershipRepository.countByTeamId(team.getId())),
                0);
    }

    private TeamMemberResponse toMemberResponse(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getUsername(),
                membership.getRole());
    }
}
