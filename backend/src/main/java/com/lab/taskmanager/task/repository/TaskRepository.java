package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    List<Task> findByOwnerIdAndStatusOrderByUpdatedAtDesc(Long id, TaskStatus status);

    List<Task> findByOwnerIdAndPriorityOrderByUpdatedAtDesc(Long id, TaskPriority priority);

    List<Task> findByOwnerIdAndStatusAndPriorityOrderByUpdatedAtDesc(
            Long id,
            TaskStatus status,
            TaskPriority priority);

    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);
    
    Page<Task> findByOwnerIdAndPriority(Long ownerId, TaskPriority priority, Pageable pageable);
    
    Page<Task> findByOwnerIdAndStatusAndPriority(
            Long ownerId, TaskStatus status, TaskPriority priority, Pageable pageable);
}
