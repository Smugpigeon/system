package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.task.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyTaskInfo {
    private Long id;
    private String title;
    private TaskStatus status;
    private String ownerUsername;
    private String assigneeUsername;
}