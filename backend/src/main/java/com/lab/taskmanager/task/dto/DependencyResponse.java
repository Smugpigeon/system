package com.lab.taskmanager.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyResponse {
    private List<DependencyTaskInfo> predecessors;
    private List<DependencyTaskInfo> successors;
}