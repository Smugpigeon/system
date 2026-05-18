package com.lab.taskmanager.team.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "team_memberships_archive",
    uniqueConstraints = @UniqueConstraint(name = "uk_team_membership", columnNames = {"team_id", "user_id"})
)
public class TeamMembershipArchive extends TeamMembership{
    private LocalDateTime archivedAt;
}
