package com.lab.taskmanager.team.dto;

import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.entity.TeamStatus;
import java.util.List;

public record TeamDetailResponse(
        Long id,
        String name,
        TeamStatus status,
        TeamRole currentUserRole,
        List<TeamMemberResponse> members) {
}
