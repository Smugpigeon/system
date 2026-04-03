package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TaskCreateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 120, message = "标题长度不能超过 120 个字符")
        String title,
        @Size(max = 1000, message = "描述长度不能超过 1000 个字符")
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueAt) {
}
