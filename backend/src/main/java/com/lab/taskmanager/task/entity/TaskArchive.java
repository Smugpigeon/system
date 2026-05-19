package com.lab.taskmanager.task.entity;

import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tasks_archive")
public class TaskArchive{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_task_id", nullable = false)
    private Long originalTaskId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    private LocalDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskScope scope;

    @Column(nullable = false, length = 20)
    private Long ownerId;

    @Column(nullable = false, length = 20)
    private Long teamId;

    @Column(nullable = false, length = 20)
    private Long assigneeId;

    LocalDateTime archivedAt;
}
