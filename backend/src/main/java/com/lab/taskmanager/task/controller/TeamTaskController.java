package com.lab.taskmanager.task.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.DependencyCreateRequest;
import com.lab.taskmanager.task.dto.DependencyResponse;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.dto.TeamTaskStatusUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskUpdateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.service.TaskDependencyService;
import com.lab.taskmanager.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams/{teamId}/tasks")
@RequiredArgsConstructor
@Tag(name = "团队任务管理", description = "提供团队空间中的任务查询、创建、分配与权限内更新接口")
public class TeamTaskController {

    private final TaskService taskService;
    private final TaskDependencyService taskDependencyService;

    @GetMapping
    @Operation(summary = "获取团队任务列表", description = "团队成员可浏览团队全部任务")
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> listTeamTasks(
            @PathVariable Long teamId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "updatedAt")
            @Parameter(
                    description = "排序方式",
                    schema = @Schema(allowableValues = {
                        "rank", "dueAt", "createdAt", "priority", "status", "updatedAt"
                    }))
            String sortBy,
            Principal principal) {
        PageResult<TaskResponse> response = taskService.listTeamTasks(
                principal.getName(),
                teamId,
                status,
                priority,
                keyword,
                PageRequest.of(size, page, sortBy));
        return ResponseEntity.ok(ApiResponse.success("团队任务列表获取成功", response));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取团队任务详情", description = "团队成员可查看团队任务详情")
    public ResponseEntity<ApiResponse<TaskResponse>> getTeamTask(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            Principal principal) {
        TaskResponse response = taskService.getTeamTask(principal.getName(), teamId, taskId);
        return ResponseEntity.ok(ApiResponse.success("团队任务详情获取成功", response));
    }

    @PostMapping
    @Operation(summary = "创建团队任务", description = "仅团队管理员或拥有者可创建团队任务并分配成员")
    public ResponseEntity<ApiResponse<TaskResponse>> createTeamTask(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamTaskCreateRequest request,
            Principal principal) {
        TaskResponse response = taskService.createTeamTask(principal.getName(), teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("团队任务创建成功", response));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "更新团队任务", description = "仅团队管理员或拥有者可完整更新团队任务")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTeamTask(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody TeamTaskUpdateRequest request,
            Principal principal) {
        TaskResponse response = taskService.updateTeamTask(principal.getName(), teamId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success("团队任务更新成功", response));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "更新团队任务状态", description = "团队成员只能修改分配给自己的任务状态；管理员和拥有者也可更新状态")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTeamTaskStatus(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody TeamTaskStatusUpdateRequest request,
            Principal principal) {
        TaskResponse response = taskService.updateTeamTaskStatus(principal.getName(), teamId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success("团队任务状态更新成功", response));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除团队任务", description = "仅团队管理员或拥有者可删除团队任务")
    public ResponseEntity<ApiResponse<Void>> deleteTeamTask(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            Principal principal) {
        taskService.deleteTeamTask(principal.getName(), teamId, taskId);
        return ResponseEntity.ok(ApiResponse.success("团队任务删除成功"));
    }

    // ================= Team Task Dependencise =================

    @GetMapping("/{taskId}/dependencies")
    @Operation(summary = "获取团队任务的依赖关系", description = "团队成员可查看团队任务的依赖关系")
    public ResponseEntity<ApiResponse<DependencyResponse>> getTeamTaskDependencies(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            Principal principal) {
        // teamId 用于权限校验，但实际校验在 Service 层通过 task.team.id 完成
        DependencyResponse response = taskDependencyService.getDependencies(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("依赖关系获取成功", response));
    }

    @PostMapping("/{taskId}/dependencies")
    @Operation(summary = "为团队任务添加前置依赖", description = "仅团队管理员或拥有者可操作")
    public ResponseEntity<ApiResponse<Void>> addTeamTaskDependency(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody DependencyCreateRequest request,
            Principal principal) {
        taskDependencyService.addDependency(principal.getName(), taskId, request.getPredecessorTaskId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("依赖关系添加成功"));
    }

    @DeleteMapping("/{taskId}/dependencies/{predecessorId}")
    @Operation(summary = "删除团队任务的前置依赖", description = "仅团队管理员或拥有者可操作")
    public ResponseEntity<ApiResponse<Void>> removeTeamTaskDependency(
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @PathVariable Long predecessorId,
            Principal principal) {
        taskDependencyService.removeDependency(principal.getName(), taskId, predecessorId);
        return ResponseEntity.ok(ApiResponse.success("依赖关系删除成功"));
    }
}
