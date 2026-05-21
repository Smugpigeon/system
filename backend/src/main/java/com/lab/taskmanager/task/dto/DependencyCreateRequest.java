package com.lab.taskmanager.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyCreateRequest {
    
    @NotNull(message = "前置任务ID不能为空")
    private Long predecessorTaskId;
}