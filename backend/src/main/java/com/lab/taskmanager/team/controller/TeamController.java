package com.lab.taskmanager.team.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskAssignRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.service.TaskService;
import com.lab.taskmanager.team.dto.TeamCreateRequest;
import com.lab.taskmanager.team.dto.TeamDetailResponse;
import com.lab.taskmanager.team.dto.TeamMemberAddRequest;
import com.lab.taskmanager.team.dto.TeamMemberResponse;
import com.lab.taskmanager.team.dto.TeamRoleUpdateRequest;
import com.lab.taskmanager.team.dto.TeamSummaryResponse;
import com.lab.taskmanager.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "团队管理", description = "提供团队创建、成员管理与团队空间查询接口")
public class TeamController {

    private final TeamService teamService;
    private final TaskService taskService;

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

    // ============== Team Tasks ==============
    // Create team task
    @PostMapping("/{teamId}/tasks")
    @Operation(summary = "创建团队任务", description = "在指定团队中创建任务（需要 Admin 或 Owner 权限）")
    public ResponseEntity<ApiResponse<TaskResponse>> createTeamTask(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamTaskCreateRequest request,
            Principal principal) {
        TaskResponse response = taskService.createTeamTask(principal.getName(), teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("团队任务创建成功", response));
    }

    // Get team task list
    @GetMapping("/{teamId}/tasks")
    @Operation(summary = "获取团队任务列表", description = "获取指定团队的所有任务（需要是团队成员）")
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> getTeamTasks(
            @PathVariable Long teamId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "updatedAt")
            @Parameter(description = "Ranking Criteria", 
                   example = "rank",
                   schema = @Schema(allowableValues = {
                       "rank", "dueAt", "createdAt", "priority", "status", "updatedAt"
                   })) String sortBy,
            Principal principal) {
        PageRequest pageRequest = PageRequest.of(size, page, sortBy);
        PageResult<TaskResponse> tasks = taskService.getTeamTasks(
                principal.getName(), teamId, status, priority, keyword, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("团队任务列表获取成功", tasks));
    }

    // Assign team task
    @PutMapping("/{teamId}/tasks/{taskId}/assign")
    @Operation(summary = "分配团队任务", description = "将团队任务分配给团队成员（需要 Admin 或 Owner 权限）")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignRequest request,
            Principal principal) {
        TaskResponse response = taskService.assignTask(principal.getName(), teamId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务分配成功", response));
    }
}
