package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.TaskScope;
import com.lab.taskmanager.task.entity.TaskStatus;

public record TaskDependencyItemResponse(
        Long id,
        String title,
        TaskStatus status,
        TaskScope scope,
        Long teamId,
        String assigneeUsername) {
}
