package com.lab.taskmanager.task.dto;

import java.util.List;

public record TaskDependencyResponse(
        Long taskId,
        List<TaskDependencyItemResponse> predecessors,
        List<TaskDependencyItemResponse> successors,
        boolean blockedByDependencies,
        List<TaskDependencyItemResponse> unfinishedPredecessors) {
}
