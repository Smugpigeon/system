package com.lab.taskmanager.task.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task_dependencies_archive",
    uniqueConstraints = @UniqueConstraint(columnNames = {"predecessor_task_id", "successor_task_id"}))
public class TaskDependencyArchive extends TaskDependency{
    private LocalDateTime archivedAt;
}
