package com.lab.taskmanager.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank(message = "团队名称不能为空")
        @Size(max = 80, message = "团队名称长度不能超过80个字符")
        String name) {
}
