package com.lab.taskmanager.team.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMembershipRepository teamMembershipRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamAuthorizationService teamAuthorizationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TeamService teamService;

    @Test
    void createTeamShouldPersistOwnerMembership() {
        User owner = user(1L, "owner_user");
        when(userService.findByUsernameOrThrow("owner_user")).thenReturn(owner);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(100L);
            return team;
        });
        when(teamMembershipRepository.save(any(TeamMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamSummaryResponse response = teamService.createTeam("owner_user", new TeamCreateRequest("Alpha Team"));

        ArgumentCaptor<TeamMembership> membershipCaptor = ArgumentCaptor.forClass(TeamMembership.class);
        verify(teamMembershipRepository).save(membershipCaptor.capture());
        TeamMembership savedMembership = membershipCaptor.getValue();

        assertEquals(100L, response.id());
        assertEquals("Alpha Team", response.name());
        assertEquals(TeamRole.OWNER, response.currentUserRole());
        assertEquals(owner.getId(), savedMembership.getUser().getId());
        assertEquals(TeamRole.OWNER, savedMembership.getRole());
    }

    @Test
    void addMemberShouldRejectDuplicateMembership() {
        User owner = user(1L, "owner_user");
        User targetUser = user(2L, "member_user");
        Team team = team(100L, "Alpha Team", owner);
        TeamMembership ownerMembership = membership(team, owner, TeamRole.OWNER);

        when(userService.findByUsernameOrThrow("owner_user")).thenReturn(owner);
        when(userService.findByUsernameOrThrow("member_user")).thenReturn(targetUser);
        when(teamAuthorizationService.requireOwner(100L, owner.getId())).thenReturn(ownerMembership);
        when(teamMembershipRepository.existsByTeamIdAndUserId(100L, targetUser.getId())).thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> teamService.addMember("owner_user", 100L, new TeamMemberAddRequest("member_user")));
    }

    @Test
    void updateMemberRoleShouldRejectChangingOwnerRole() {
        User owner = user(1L, "owner_user");
        Team team = team(100L, "Alpha Team", owner);
        TeamMembership ownerMembership = membership(team, owner, TeamRole.OWNER);
        TeamMembership targetMembership = membership(team, owner, TeamRole.OWNER);

        when(userService.findByUsernameOrThrow("owner_user")).thenReturn(owner);
        when(teamAuthorizationService.requireOwner(100L, owner.getId())).thenReturn(ownerMembership);
        when(teamMembershipRepository.findByTeamIdAndUserId(100L, owner.getId()))
            .thenReturn(java.util.Optional.of(targetMembership));

        assertThrows(
            ForbiddenOperationException.class,
            () -> teamService.updateMemberRole(
                "owner_user",
                100L,
                owner.getId(),
                new TeamRoleUpdateRequest(TeamRole.ADMIN)));
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        return user;
    }

    private Team team(Long id, String name, User owner) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        team.setOwner(owner);
        return team;
    }

    private TeamMembership membership(Team team, User user, TeamRole role) {
        TeamMembership membership = new TeamMembership();
        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(role);
        return membership;
    }
}
