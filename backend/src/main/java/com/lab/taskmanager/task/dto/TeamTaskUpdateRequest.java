package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TeamTaskUpdateRequest(
        @NotBlank(message = "任务标题不能为空")
        @Size(max = 120, message = "任务标题长度不能超过120个字符")
        String title,
        @Size(max = 1000, message = "任务描述长度不能超过1000个字符")
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueAt,
        @NotNull(message = "团队任务必须分配给一名团队成员")
        Long assigneeId) {
}
