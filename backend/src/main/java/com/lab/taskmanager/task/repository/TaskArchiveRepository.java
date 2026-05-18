package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskArchiveRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}
