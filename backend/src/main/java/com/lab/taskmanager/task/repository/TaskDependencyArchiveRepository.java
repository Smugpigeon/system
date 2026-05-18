package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.TaskDependencyArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyArchiveRepository extends JpaRepository<TaskDependencyArchive, Long> {
}
