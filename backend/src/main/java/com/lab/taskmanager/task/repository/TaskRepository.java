package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.Task;
import java.util.List;
import java.util.Optional;

import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    List<Task> findByOwnerIdAndStatus(Long id, TaskStatus status);

    List<Task> findByOwnerIdAndPriority(Long id, TaskPriority priority);


    List<Task> findByOwnerIdAndStatusAndPriority(Long id, TaskStatus status, TaskPriority priority);
}
