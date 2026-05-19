package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.TeamArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamArchiveRepository extends JpaRepository<TeamArchive, Long> {
    Optional<TeamArchive> findByOriginalTeamId(Long originalTeamId);
}
