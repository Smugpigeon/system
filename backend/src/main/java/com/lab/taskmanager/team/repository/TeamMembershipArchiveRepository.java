package com.lab.taskmanager.team.repository;

import com.lab.taskmanager.team.entity.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipArchiveRepository extends JpaRepository<TeamMembership, Long> {
}
