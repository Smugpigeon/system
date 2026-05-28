package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Override
    @EntityGraph(attributePaths = {"owner"})
    List<Team> findAll();

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}
