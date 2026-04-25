package com.lab.taskmanager.task.algorithm;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskRankingServiceTest {

    private final TaskRankingService taskRankingService = new TaskRankingService();

    @Test
    void overdueHighPriorityTaskShouldRankFirst() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 14, 30);

        Task overdueTask = buildTask(1L, TaskPriority.HIGH, TaskStatus.TODO, now.minusHours(2));
        Task normalTask = buildTask(2L, TaskPriority.MEDIUM, TaskStatus.TODO, now.plusDays(5));

        List<Task> rankedTasks = taskRankingService.sortTasks(List.of(normalTask, overdueTask), now);

        assertEquals(1L, rankedTasks.get(0).getId());
        assertTrue(taskRankingService.rank(overdueTask, now).overdue());
        assertEquals(TaskUrgencyLevel.CRITICAL, taskRankingService.rank(overdueTask, now).urgencyLevel());
    }

    @Test
    void inProgressTaskDueSoonShouldBeatLowPriorityTaskWithoutDeadline() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 14, 30);

        Task focusedTask = buildTask(3L, TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, now.plusHours(10));
        Task relaxedTask = buildTask(4L, TaskPriority.LOW, TaskStatus.TODO, null);

        List<Task> rankedTasks = taskRankingService.sortTasks(List.of(relaxedTask, focusedTask), now);

        assertEquals(3L, rankedTasks.get(0).getId());
        assertEquals(TaskUrgencyLevel.HIGH, taskRankingService.rank(focusedTask, now).urgencyLevel());
    }

    @Test
    void doneTaskShouldStayAtLowUrgency() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 14, 30);
        Task finishedTask = buildTask(5L, TaskPriority.HIGH, TaskStatus.DONE, now.plusHours(6));

        TaskRankResult result = taskRankingService.rank(finishedTask, now);

        assertEquals(TaskUrgencyLevel.LOW, result.urgencyLevel());
    }

    private Task buildTask(Long id, TaskPriority priority, TaskStatus status, LocalDateTime dueAt) {
        Task task = new Task();
        task.setId(id);
        task.setPriority(priority);
        task.setStatus(status);
        task.setDueAt(dueAt);
        task.setTitle("task-" + id);
        task.setDescription("");
        return task;
    }
}
