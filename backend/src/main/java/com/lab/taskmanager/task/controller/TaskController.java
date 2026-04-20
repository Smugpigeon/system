package com.lab.taskmanager.task.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskAssignRequest;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "提供任务管理接口")
public class TaskController {

    private final TaskService taskService;

    // ============== Personal Tasks ==============
    // Get personal task list
    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取当前用户的所有个人任务（个人创建且不属于任何团队的任务）与被分配任务，支持按智能排序")
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> listTasks(
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
        PageResult<TaskResponse> tasks = taskService.listTasks(principal.getName(), status, priority, keyword, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("任务列表获取成功", tasks));
    }

    // Create personal task
    @PostMapping
    @Operation(summary = "创建任务", description = "为当前用户创建一个新任务")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            Principal principal) {
        TaskResponse response = taskService.createTask(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("任务创建成功", response));
    }


    // ============== Team Tasks ==============
    // Create team task
    @PostMapping("/teams")
    @Operation(summary = "创建团队任务", description = "在指定团队中创建任务（需要 Admin 或 Owner 权限）")
    public ResponseEntity<ApiResponse<TaskResponse>> createTeamTask(
            @Valid @RequestBody TeamTaskCreateRequest request,
            Principal principal) {
        TaskResponse response = taskService.createTeamTask(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("团队任务创建成功", response));
    }

    // Get team task list
    @GetMapping("/teams/{teamId}")
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
    @PutMapping("/{taskId}/assign")
    @Operation(summary = "分配任务", description = "将团队任务分配给团队成员（需要 Admin 或 Owner 权限）")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignRequest request,
            Principal principal) {
        TaskResponse response = taskService.assignTask(principal.getName(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务分配成功", response));
    }

    // ============== General Api ==============
    // Get personal or team task detail
    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情", description = "根据任务ID获取任务的详细信息")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long taskId,
            Principal principal) {
        TaskResponse response = taskService.getTask(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务详情获取成功", response));
    }

    // Update personal or team task
    @PutMapping("/{taskId}")
    @Operation(summary = "更新任务", description = "根据任务ID更新任务的相关信息")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request,
            Principal principal) {
        TaskResponse response = taskService.updateTask(principal.getName(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务更新成功", response));
    }

    // Delete personal or team task
    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务", description = "根据任务ID删除任务")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long taskId,
            Principal principal) {
        taskService.deleteTask(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务删除成功"));
    }
}
