package com.lab.taskmanager.team.entity;

import com.lab.taskmanager.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teams_archive")
public class TeamArchive{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_team_id", nullable = false)
    private Long originalTeamId;

    @Column(nullable = false, length = 80)
    private String name;

    @JoinColumn(name = "owner_id", nullable = false)
    private Long ownerId;

    private LocalDateTime archivedAt;
}
