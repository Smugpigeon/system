package com.lab.taskmanager.team.service;

import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserService userService;

    /**
     * Create a new team and register the creator as team owner.
     *
     * @param username authenticated username
     * @param request team creation payload
     * @return summary of the created team
     */
    @Transactional
    public TeamSummaryResponse createTeam(String username, TeamCreateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);

        Team team = new Team();
        team.setName(request.name().trim());
        team.setOwner(currentUser);
        Team savedTeam = teamRepository.save(team);

        TeamMembership ownerMembership = new TeamMembership();
        ownerMembership.setTeam(savedTeam);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(TeamRole.OWNER);
        teamMembershipRepository.save(ownerMembership);

        return new TeamSummaryResponse(savedTeam.getId(), savedTeam.getName(), TeamRole.OWNER, 1, 0);
    }

    /**
     * List all teams joined by the current user.
     *
     * @param username authenticated username
     * @return sorted team summaries visible to the current user
     */
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams(String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        return teamMembershipRepository.findAllByUserId(currentUser.getId())
                .stream()
                .sorted(Comparator.comparing(membership -> membership.getTeam().getName().toLowerCase()))
                .map(this::toSummary)
                .toList();
    }

    private TeamSummaryResponse toSummary(TeamMembership membership) {
        Team team = membership.getTeam();
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                membership.getRole(),
                Math.toIntExact(teamMembershipRepository.countByTeamId(team.getId())),
                0);
    }
}
