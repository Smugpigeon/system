package com.lab.taskmanager.team.service;

import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamMembershipStatus;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.entity.TeamStatus;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class TeamAuthorizationService {

    private final TeamMembershipRepository teamMembershipRepository;

    /**
     * Ensure the current user belongs to the target team.
     *
     * @param teamId team identifier
     * @param userId current user identifier
     * @return persisted team membership
     */
    public TeamMembership requireMembership(@NotNull Long teamId, @NotNull Long userId) {
        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserIdAndStatus(
                        teamId,
                        userId,
                        TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("团队不存在，或你不是该团队成员"));
        if (membership.getTeam().getStatus() != TeamStatus.ACTIVE) {
            throw new ForbiddenOperationException("团队已解散，不能继续访问团队空间");
        }
        return membership;
    }

    /**
     * Ensure the current user has at least Admin privilege in the target team.
     *
     * @param teamId team identifier
     * @param userId current user identifier
     * @return validated team membership
     */
    public TeamMembership requireAdminOrOwner(@NotNull Long teamId, @NotNull Long userId) {
        TeamMembership membership = requireMembership(teamId, userId);
        if (membership.getRole() == TeamRole.MEMBER) {
            throw new ForbiddenOperationException("只有团队管理员或拥有者可以执行该操作");
        }
        return membership;
    }

    /**
     * Ensure the current user is the Owner of the target team.
     *
     * @param teamId team identifier
     * @param userId current user identifier
     * @return validated owner membership
     */
    public TeamMembership requireOwner(@NotNull Long teamId, @NotNull Long userId) {
        TeamMembership membership = requireMembership(teamId, userId);
        if (membership.getRole() != TeamRole.OWNER) {
            throw new ForbiddenOperationException("只有团队拥有者可以执行该操作");
        }
        return membership;
    }
}
