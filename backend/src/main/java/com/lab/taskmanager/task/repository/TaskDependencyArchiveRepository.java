package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyArchiveRepository extends JpaRepository<TaskDependency, Long> {
}
