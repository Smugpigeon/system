package com.lab.taskmanager.task.algorithm;

public record TaskRankResult(Long taskId, int score, TaskUrgencyLevel urgencyLevel, boolean overdue) {
}
