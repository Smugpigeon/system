package com.lab.taskmanager.task.dto;

import jakarta.validation.constraints.NotNull;

public record TaskDependencyCreateRequest(
        @NotNull(message = "前置任务ID不能为空")
        Long predecessorTaskId) {
}
