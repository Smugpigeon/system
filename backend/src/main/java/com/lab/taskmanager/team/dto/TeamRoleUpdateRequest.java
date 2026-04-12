package com.lab.taskmanager.team.dto;

import com.lab.taskmanager.team.entity.TeamRole;
import jakarta.validation.constraints.NotNull;

public record TeamRoleUpdateRequest(
        @NotNull(message = "团队角色不能为空")
        TeamRole role) {
}
