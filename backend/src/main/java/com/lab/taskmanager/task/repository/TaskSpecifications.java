package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {
    
    public static Specification<Task> ownerIdEquals(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
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
    
    // Compose specifications
    public static Specification<Task> buildSpecification(Long ownerId,
                                                          TaskStatus status,
                                                          TaskPriority priority,
                                                          String keyword) {
        Specification<Task> spec = Specification.unrestricted();
        
        spec = spec.and(ownerIdEquals(ownerId));
        
        Specification<Task> statusSpec = statusEquals(status);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }
        
        Specification<Task> prioritySpec = priorityEquals(priority);
        if (prioritySpec != null) {
            spec = spec.and(prioritySpec);
        }
        
        Specification<Task> keywordSpec = keywordContains(keyword);
        if (keywordSpec != null) {
            spec = spec.and(keywordSpec);
        }
        
        return spec;
    }
}
