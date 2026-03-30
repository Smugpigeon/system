package com.lab.taskmanager.task.dto;

import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(value = 1)
        Integer size,
        Integer page
){
}
