package com.lab.taskmanager.task.dto;

import jakarta.validation.constraints.NotNull;

public record TaskAssignRequest(
    @NotNull(message = "被分配的用户ID不能为空")
    Long assigneeId
) {}
