package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamMembershipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    Optional<TeamMembership> findByTeamIdAndUserIdAndStatus(
            Long teamId,
            Long userId,
            TeamMembershipStatus status);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    Optional<TeamMembership> findByTeamIdAndUserUsername(Long teamId, String username);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    List<TeamMembership> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    List<TeamMembership> findAllByUserIdAndStatus(Long userId, TeamMembershipStatus status);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    List<TeamMembership> findAllByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    List<TeamMembership> findAllByTeamIdAndStatus(Long teamId, TeamMembershipStatus status);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserIdAndStatus(Long teamId, Long userId, TeamMembershipStatus status);

    long countByTeamId(Long teamId);

    long countByTeamIdAndStatus(Long teamId, TeamMembershipStatus status);
}
