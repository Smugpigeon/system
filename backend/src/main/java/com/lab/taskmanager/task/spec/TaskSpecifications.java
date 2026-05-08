package com.lab.taskmanager.task.spec;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskScope;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.team.entity.TeamStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> dashboardVisibleTo(Long userId) {
        return (root, query, builder) -> builder.or(
                builder.and(
                        builder.equal(root.get("scope"), TaskScope.PERSONAL),
                        builder.equal(root.get("owner").get("id"), userId)),
                builder.and(
                        builder.equal(root.get("scope"), TaskScope.TEAM),
                        builder.equal(root.get("assignee").get("id"), userId),
                        builder.equal(root.join("team", JoinType.LEFT).get("status"), TeamStatus.ACTIVE))
        );
    }

    public static Specification<Task> teamTasks(Long teamId) {
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("scope"), TaskScope.TEAM),
                builder.equal(root.join("team", JoinType.LEFT).get("id"), teamId),
                builder.equal(root.join("team", JoinType.LEFT).get("status"), TeamStatus.ACTIVE)
        );
    }

    public static Specification<Task> personalTaskOwnedBy(Long userId) {
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("scope"), TaskScope.PERSONAL),
                builder.equal(root.get("owner").get("id"), userId)
        );
    }

    public static Specification<Task> withId(Long taskId) {
        return (root, query, builder) -> builder.equal(root.get("id"), taskId);
    }

    public static Specification<Task> withStatus(TaskStatus status) {
        return (root, query, builder) -> status == null
                ? builder.conjunction()
                : builder.equal(root.get("status"), status);
    }

    public static Specification<Task> withPriority(TaskPriority priority) {
        return (root, query, builder) -> priority == null
                ? builder.conjunction()
                : builder.equal(root.get("priority"), priority);
    }
}
