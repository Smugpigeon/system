package com.lab.taskmanager.team.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teams_archive")
public class TeamArchive extends Team{
    private LocalDateTime archivedAt;
}
