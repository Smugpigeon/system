package com.lab.taskmanager.task.dto;

import jakarta.validation.constraints.Min;

public class PageRequest {
    @Min(value = 1)
    private Integer page = 1;
    private Integer size = 5;
}
