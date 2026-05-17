package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    Optional<TaskDependency> findByPredecessorTaskIdAndSuccessorTaskId(Long predecessorId, Long successorId);

    List<TaskDependency> findAllBySuccessorTaskId(Long successorTaskId);

    List<TaskDependency> findAllByPredecessorTaskId(Long predecessorTaskId);

    boolean existsBySuccessorTaskIdAndPredecessorTaskId(Long successorId, Long predecessorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskDependency d WHERE d.predecessorTaskId = :taskId OR d.successorTaskId = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);

    boolean existsBySuccessorTaskId(Long successorTaskId);
}