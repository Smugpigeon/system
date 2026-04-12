package com.lab.taskmanager.team.dto;

import com.lab.taskmanager.team.entity.TeamRole;

public record TeamSummaryResponse(
        Long id,
        String name,
        TeamRole currentUserRole,
        int memberCount,
        int teamTaskCount) {
}
