package com.lab.taskmanager.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TeamMemberAddRequest(
        @NotBlank(message = "被添加成员的用户名不能为空")
        @Pattern(
                regexp = "^[A-Za-z0-9_]{4,20}$",
                message = "用户名只允许包含字母、数字、下划线，长度为 4 到 20 位")
        String username) {
}
