package com.lab.taskmanager.task.repository;

import com.lab.taskmanager.task.entity.TaskDependency;
import com.lab.taskmanager.task.entity.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    boolean existsByPredecessorIdAndSuccessorId(Long predecessorId, Long successorId);

    Optional<TaskDependency> findByPredecessorIdAndSuccessorId(Long predecessorId, Long successorId);

    @EntityGraph(attributePaths = {
        "predecessor", "predecessor.team", "predecessor.owner", "predecessor.assignee",
        "successor", "successor.team", "successor.owner", "successor.assignee"
    })
    List<TaskDependency> findAllBySuccessorId(Long successorId);

    @EntityGraph(attributePaths = {
        "predecessor", "predecessor.team", "predecessor.owner", "predecessor.assignee",
        "successor", "successor.team", "successor.owner", "successor.assignee"
    })
    List<TaskDependency> findAllByPredecessorId(Long predecessorId);

    long countByPredecessorId(Long predecessorId);

    long countBySuccessorId(Long successorId);

    @Query("""
            select count(dependency)
            from TaskDependency dependency
            where dependency.successor.id = :successorId
              and dependency.predecessor.status <> :doneStatus
            """)
    long countUnfinishedPredecessors(
            @Param("successorId") Long successorId,
            @Param("doneStatus") TaskStatus doneStatus);

    @Query("""
            select count(dependency)
            from TaskDependency dependency
            where dependency.predecessor.id = :predecessorId
              and dependency.successor.status = :doneStatus
            """)
    long countDoneSuccessors(
            @Param("predecessorId") Long predecessorId,
            @Param("doneStatus") TaskStatus doneStatus);

    void deleteAllBySuccessorId(Long successorId);
}
