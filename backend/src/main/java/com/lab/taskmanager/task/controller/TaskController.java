package com.lab.taskmanager.task.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
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

    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取当前用户创建的个人任务，以及分配给自己的团队任务")
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

    @PostMapping
    @Operation(summary = "创建个人任务", description = "为当前用户创建一个新的个人任务")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            Principal principal) {
        TaskResponse response = taskService.createTask(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("任务创建成功", response));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情", description = "获取当前用户可见的个人任务或分配给自己的团队任务详情")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long taskId,
            Principal principal) {
        TaskResponse response = taskService.getTask(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务详情获取成功", response));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "更新个人任务", description = "更新当前用户拥有的个人任务。团队任务请走团队空间接口")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request,
            Principal principal) {
        TaskResponse response = taskService.updateTask(principal.getName(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务更新成功", response));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除个人任务", description = "删除当前用户拥有的个人任务。团队任务请走团队空间接口")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long taskId,
            Principal principal) {
        taskService.deleteTask(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务删除成功"));
    }
}
