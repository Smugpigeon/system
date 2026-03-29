package com.lab.taskmanager.task.controller.algorithm;

public record TaskRankResult(Long taskId, int score, TaskUrgencyLevel urgencyLevel, boolean overdue) {
}
