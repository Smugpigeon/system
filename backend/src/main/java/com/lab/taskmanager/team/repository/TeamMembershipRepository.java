package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.TeamMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    Optional<TeamMembership> findByTeamIdAndUserUsername(Long teamId, String username);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    List<TeamMembership> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"team", "team.owner", "user"})
    List<TeamMembership> findAllByTeamId(Long teamId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    long countByTeamId(Long teamId);
}
