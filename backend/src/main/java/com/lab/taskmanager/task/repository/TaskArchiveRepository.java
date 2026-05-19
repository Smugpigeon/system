package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.TaskArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TaskArchiveRepository extends JpaRepository<TaskArchive, Long>, JpaSpecificationExecutor<TaskArchive> {
    Optional<TaskArchive> findByOriginalTaskId(Long originalTaskId);
}
