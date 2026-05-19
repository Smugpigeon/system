package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.TeamMembershipArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMembershipArchiveRepository extends JpaRepository<TeamMembershipArchive, Long> {
    Optional<TeamMembershipArchive> findByOriginalTeamMembershipId(Long originalTeamMembershipId);
}
