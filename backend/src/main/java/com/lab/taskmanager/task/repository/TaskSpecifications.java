package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {
    
    // Personal tasks Condtions: ownerId = userId and teamId is null
    public static Specification<Task> personalTask(Long userId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("owner").get("id"), userId),
            cb.isNull(root.get("team"))
        );
    }

    // Assigned tasks Condtions: assigneeId = userId and teamId is not null
    public static Specification<Task> assignedTeamTask(Long userId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("assignee").get("id"), userId),
            cb.isNotNull(root.get("team"))
        );
    }

    // Team tasks Condtions: task.teamId = teamId
    public static Specification<Task> teamTask(Long teamId) {
        return (root, query, cb) -> cb.equal(root.get("team").get("id"), teamId);
    }
    
    public static Specification<Task> statusEquals(TaskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
    
    public static Specification<Task> priorityEquals(TaskPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }
    
    public static Specification<Task> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
            Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
            return cb.or(titleMatch, descMatch);
        };
    }
    
    // Compose specifications for personal tasks list(contains both personal tasks and assigned tasks)
    public static Specification<Task> buildPersonalTasks(Long ownerId,
                                                          TaskStatus status,
                                                          TaskPriority priority,
                                                          String keyword) {
        Specification<Task> spec = personalTask(ownerId).or(assignedTeamTask(ownerId));
        
        if (status != null) {
            spec = spec.and(statusEquals(status));
        }
        
        if (priority != null) {
            spec = spec.and(priorityEquals(priority));
        }
        
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(keywordContains(keyword));
        }
        
        return spec;
    }

    // Compose specifications for team tasks list(contains only task.team.id = team.id)
    public static Specification<Task> buildTeamTasks(Long teamId,
                                                          TaskStatus status,
                                                          TaskPriority priority,
                                                          String keyword) {
        Specification<Task> spec = teamTask(teamId);
        
        if (status != null) {
            spec = spec.and(statusEquals(status));
        }
        
        if (priority != null) {
            spec = spec.and(priorityEquals(priority));
        }
        
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(keywordContains(keyword));
        }
        
        return spec;
    }
}
