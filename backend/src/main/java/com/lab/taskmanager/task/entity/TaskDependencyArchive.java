package com.lab.taskmanager.task.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_dependencies_archive")
public class TaskDependencyArchive{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_dependency_id", nullable = false)
    private Long taskDependencyId;

    @Column(name = "predecessor_task_id", nullable = false)
    private Long predecessorTaskId;

    @Column(name = "successor_task_id", nullable = false)
    private Long successorTaskId;

    private LocalDateTime archivedAt;
}
