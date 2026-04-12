package com.lab.taskmanager.team.dto;

import com.lab.taskmanager.team.entity.TeamRole;

public record TeamMemberResponse(Long userId, String username, TeamRole role) {
}
