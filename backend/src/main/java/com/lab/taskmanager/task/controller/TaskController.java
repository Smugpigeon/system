package com.lab.taskmanager.task.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Fetch all tasks that belong to the currently authenticated user.
     *
     * @param authentication security context principal
     * @return task list
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listTasks(Authentication authentication) {
        List<TaskResponse> tasks = taskService.listTasks(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("任务列表获取成功", tasks));
    }

    /**
     * Create a new personal task for the current user.
     *
     * @param request task input from the frontend
     * @param authentication security context principal
     * @return created task
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            Authentication authentication) {
        TaskResponse response = taskService.createTask(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("任务创建成功", response));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long taskId,
            Authentication authentication) {
        TaskResponse response = taskService.getTask(authentication.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务详情获取成功", response));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request,
            Authentication authentication) {
        TaskResponse response = taskService.updateTask(authentication.getName(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务更新成功", response));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long taskId,
            Authentication authentication) {
        taskService.deleteTask(authentication.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务删除成功"));
    }

    /**
     * get filtered tasks on status or priority
     * @param status
     * @param priority
     * @param authentication
     * @return
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getFilteredTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            Authentication authentication
    ){
        List<TaskResponse> filteredTasks = taskService.getFilteredTasks(authentication.getName(), status, priority);
        return ResponseEntity.ok(ApiResponse.success("筛选任务获取成功", filteredTasks));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<TaskResponse>>> page(
            @RequestBody PageRequest pageRequest,
            Authentication authentication
    ) {
        PageResult<TaskResponse> pageResult = taskService.page(pageRequest, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("分页结果获取成功", pageResult));
    }
}
