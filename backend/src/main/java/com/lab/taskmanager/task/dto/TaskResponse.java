package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskScope;
import com.lab.taskmanager.task.entity.TaskStatus;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        TaskScope scope,
        Long teamId,
        String teamName,
        Long ownerId,
        String ownerUsername,
        Long assigneeId,
        String assigneeUsername,
        boolean blockedByDependencies,
        int predecessorCount,
        int successorCount,
        int unfinishedPredecessorCount,
        boolean canEditDetails,
        boolean canEditStatus,
        boolean canDelete) {
}
