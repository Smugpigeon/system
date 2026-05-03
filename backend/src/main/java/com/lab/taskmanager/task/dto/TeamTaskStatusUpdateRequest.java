package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TeamTaskStatusUpdateRequest(
        @NotNull(message = "任务状态不能为空")
        TaskStatus status) {
}
