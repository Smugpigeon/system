package com.lab.taskmanager.team.dto;

import com.lab.taskmanager.team.entity.TeamRole;
import java.util.List;

public record TeamDetailResponse(
        Long id,
        String name,
        TeamRole currentUserRole,
        List<TeamMemberResponse> members) {
}
