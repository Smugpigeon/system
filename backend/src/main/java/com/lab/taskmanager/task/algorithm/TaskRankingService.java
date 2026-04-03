package com.lab.taskmanager.task.algorithm;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskRankingService {

    public List<Task> sortTasks(List<Task> tasks) {
        return sortTasks(tasks, LocalDateTime.now());
    }

    List<Task> sortTasks(List<Task> tasks, LocalDateTime referenceTime) {
        return tasks.stream()
                .sorted(buildComparator(referenceTime))
                .toList();
    }

    public TaskRankResult rank(Task task) {
        return rank(task, LocalDateTime.now());
    }

    TaskRankResult rank(Task task, LocalDateTime referenceTime) {
        int score = scoreByPriority(task.getPriority()) + scoreByStatus(task.getStatus());
        boolean overdue = false;

        if (task.getDueAt() != null) {
            overdue = task.getDueAt().isBefore(referenceTime);
            score += scoreByDeadline(task.getDueAt(), referenceTime);
        }

        return new TaskRankResult(task.getId(), score, resolveUrgencyLevel(score, overdue), overdue);
    }

    private Comparator<Task> buildComparator(LocalDateTime referenceTime) {
        return Comparator
                .comparingInt((Task task) -> rank(task, referenceTime).score())
                .reversed()
                .thenComparing(Task::getDueAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(Task::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int scoreByPriority(TaskPriority priority) {
        if (priority == null) {
            return 40;
        }

        return switch (priority) {
            case LOW -> 20;
            case MEDIUM -> 40;
            case HIGH -> 70;
        };
    }

    private int scoreByStatus(TaskStatus status) {
        if (status == null) {
            return 0;
        }

        return switch (status) {
            case TODO -> 20;
            case IN_PROGRESS -> 35;
            case DONE -> -120;
        };
    }

    private int scoreByDeadline(LocalDateTime dueAt, LocalDateTime referenceTime) {
        Duration duration = Duration.between(referenceTime, dueAt);
        long hours = duration.toHours();

        if (hours < 0) {
            return 120;
        }
        if (hours <= 24) {
            return 90;
        }
        if (hours <= 72) {
            return 60;
        }
        if (hours <= 168) {
            return 30;
        }
        return 0;
    }

    private TaskUrgencyLevel resolveUrgencyLevel(int score, boolean overdue) {
        if (overdue || score >= 170) {
            return TaskUrgencyLevel.CRITICAL;
        }
        if (score >= 90) {
            return TaskUrgencyLevel.HIGH;
        }
        if (score >= 50) {
            return TaskUrgencyLevel.MEDIUM;
        }
        return TaskUrgencyLevel.LOW;
    }
}
