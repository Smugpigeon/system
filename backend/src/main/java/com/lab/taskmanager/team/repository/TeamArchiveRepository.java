package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamArchiveRepository extends JpaRepository<Team, Long> {
}
