package com.lab.taskmanager.team.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamDetailResponse;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamMemberResponse;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "团队管理", description = "提供团队创建、成员管理与团队空间查询接口")
public class TeamController {

    private final TeamService teamService;

    /**
     * Create one team for the authenticated user and register them as Owner.
     *
     * @param request validated team creation payload
     * @param principal authenticated principal
     * @return created team summary
     */
    @PostMapping
    @Operation(summary = "创建团队", description = "当前用户创建团队，并自动成为团队拥有者")
    public ResponseEntity<ApiResponse<TeamSummaryResponse>> createTeam(
            @Valid @RequestBody TeamCreateRequest request,
            Principal principal) {
        TeamSummaryResponse response = teamService.createTeam(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("团队创建成功", response));
    }

    /**
     * List teams joined by the authenticated user.
     *
     * @param principal authenticated principal
     * @return team summaries with current user's role
     */
    @GetMapping
    @Operation(summary = "获取我的团队", description = "返回当前用户所在的全部团队及其角色")
    public ResponseEntity<ApiResponse<List<TeamSummaryResponse>>> listTeams(Principal principal) {
        List<TeamSummaryResponse> response = teamService.listTeams(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("团队列表获取成功", response));
    }

    /**
     * Load one team workspace together with member role information.
     *
     * @param teamId target team identifier
     * @param principal authenticated principal
     * @return team detail payload
     */
    @GetMapping("/{teamId}")
    @Operation(summary = "获取团队详情", description = "返回团队基础信息以及成员角色列表")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(
            @PathVariable Long teamId,
            Principal principal) {
        TeamDetailResponse response = teamService.getTeamDetail(principal.getName(), teamId);
        return ResponseEntity.ok(ApiResponse.success("团队详情获取成功", response));
    }

    /**
     * Add one user into the target team.
     *
     * @param teamId target team identifier
     * @param request validated member payload
     * @param principal authenticated principal
     * @return created member view
     */
    @PostMapping("/{teamId}/members")
    @Operation(summary = "添加团队成员", description = "仅团队拥有者可将其他用户添加到团队")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamMemberAddRequest request,
            Principal principal) {
        TeamMemberResponse response = teamService.addMember(principal.getName(), teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("团队成员添加成功", response));
    }

    /**
     * Switch one member between Member and Admin roles.
     *
     * @param teamId target team identifier
     * @param userId target user identifier
     * @param request validated role payload
     * @param principal authenticated principal
     * @return updated member role view
     */
    @PutMapping("/{teamId}/members/{userId}/role")
    @Operation(summary = "更新团队成员角色", description = "仅团队拥有者可在 Member 和 Admin 之间调整角色")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @Valid @RequestBody TeamRoleUpdateRequest request,
            Principal principal) {
        TeamMemberResponse response = teamService.updateMemberRole(principal.getName(), teamId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("团队角色更新成功", response));
    }

    /**
     * Remove member from team.
     * @param teamId target team identifier
     * @param userId target user identifier
     * @param principal authenticated principal
     * @return remove message
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(summary = "移除团队成员", description = "仅团队拥有者可移除 Member 或 Admin，Admin 或 Member可以移除自己")
    public ResponseEntity<ApiResponse<Void>> removeMember(
        @PathVariable Long teamId,
        @PathVariable Long userId,
        Principal principal) {
        teamService.removeMember(principal.getName(), teamId, userId);
        return ResponseEntity.ok(ApiResponse.success("团队成员已移除"));
    }

    /**
     * Disband a team, information of the team will be archived but the team space will be inaccessible.
     * @param teamId target team identifier
     * @param principal target user identifier
     * @return disband message
     */
    @DeleteMapping("/{teamId}")
    @Operation(summary = "解散团队", description = "仅 Owner 可解散团队，解散后普通团队空间不能继续访问, 团队相关信息将留档")
    public ResponseEntity<ApiResponse<Void>> disbandTeam(
        @PathVariable Long teamId,
        Principal principal) {
        teamService.disbandTeam(principal.getName(), teamId);
        return ResponseEntity.ok(ApiResponse.success("团队已解散"));
    }

    /**
     * Owner voluntarily leaves the team.
     * - If no other members exist, the team is automatically disbanded.
     * - Otherwise, a new owner must be specified.
     */
    @PostMapping("/{teamId}/owner-leave")
    @Operation(summary = "Owner 离开团队", description =
        """
        Owner 主动离开团队：
        • 若团队还有其他成员，必须指定新 Owner；
        • 若团队仅剩 Owner，则自动解散团队。
        """
    )
    public ResponseEntity<ApiResponse<Void>> ownerLeaveTeam(
        @PathVariable Long teamId,
        @RequestParam(required = false) Long newOwnerId,
        Principal principal) {

        teamService.ownerLeaveTeam(principal.getName(), teamId, newOwnerId);

        return ResponseEntity.ok(ApiResponse.success("Owner 已离开团队"));
    }

    /**
     * 转让团队所有权：Owner 不离开团队，与目标成员对调角色（Owner 变 Member）。
     * 离开团队是独立操作（成员走 DELETE /members/{userId} 或解散走 DELETE /{teamId}）。
     */
    @PostMapping("/{teamId}/transfer-ownership")
    @Operation(summary = "转让团队所有权", description = "仅 Owner 可操作；与目标成员对调角色，Owner 变为 Member 后仍在团队中")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable Long teamId,
            @RequestParam Long newOwnerId,
            Principal principal) {
        teamService.transferOwnership(principal.getName(), teamId, newOwnerId);
        return ResponseEntity.ok(ApiResponse.success("团队所有权已转让"));
    }
}
