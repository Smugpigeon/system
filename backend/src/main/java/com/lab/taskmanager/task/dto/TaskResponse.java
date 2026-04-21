package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.team.entity.TeamRole;
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

        Long teamId,
        String teamName,

        Long ownerId,
        String ownerName,

        Long assigneeId,
        String assigneeName,
        TeamRole assigneeRole
) {
        public static TaskResponse fromPersonalTask(Task task) {
                return new TaskResponse(
                        task.getId(), task.getTitle(), task.getDescription(),
                        task.getStatus(), task.getPriority(), task.getDueAt(),
                        task.getCreatedAt(), task.getUpdatedAt(),
                        null, null, 
                        task.getOwner().getId(), task.getOwner().getUsername(),
                        null, null, null);
        }

        public static TaskResponse fromTeamTask(Task task, TeamRole assigneeRole) {
                return new TaskResponse(
                        task.getId(), task.getTitle(), task.getDescription(),
                        task.getStatus(), task.getPriority(), task.getDueAt(),
                        task.getCreatedAt(), task.getUpdatedAt(),
                        task.getTeam().getId(), task.getTeam().getName(), 
                        task.getOwner().getId(), task.getOwner().getUsername(),
                        task.getAssignee() != null ? task.getAssignee().getId() : null,
                        task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                        assigneeRole);
        }
}
