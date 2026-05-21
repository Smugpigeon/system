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
@Table(name = "team_memberships_archive")
public class TeamMembershipArchive{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_team_membership_id", nullable = false)
    private Long originalTeamMembershipId;

    @JoinColumn(name = "team_id", nullable = false)
    private Long teamId;

    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role;

    private LocalDateTime archivedAt;
}
