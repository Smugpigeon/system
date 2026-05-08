package com.lab.taskmanager.team.dto;

import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.entity.TeamStatus;

public record TeamSummaryResponse(
        Long id,
        String name,
        TeamStatus status,
        TeamRole currentUserRole,
        int memberCount,
        int teamTaskCount) {
}
