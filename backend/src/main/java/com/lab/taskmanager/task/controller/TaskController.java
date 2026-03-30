package com.lab.taskmanager.task.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.service.TaskService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listTasks(Principal principal) {
        List<TaskResponse> tasks = taskService.listTasks(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("任务列表获取成功", tasks));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            Principal principal) {
        TaskResponse response = taskService.createTask(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("任务创建成功", response));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long taskId,
            Principal principal) {
        TaskResponse response = taskService.getTask(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务详情获取成功", response));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request,
            Principal principal) {
        TaskResponse response = taskService.updateTask(principal.getName(), taskId, request);
        return ResponseEntity.ok(ApiResponse.success("任务更新成功", response));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long taskId,
            Principal principal) {
        taskService.deleteTask(principal.getName(), taskId);
        return ResponseEntity.ok(ApiResponse.success("任务删除成功"));
    }

    /**
     * get filtered tasks on status or priority
     * @param status
     * @param priority
     * @param principal
     * @return
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getFilteredTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            Principal principal
    ){
        List<TaskResponse> filteredTasks = taskService.getFilteredTasks(principal.getName(), status, priority);
        return ResponseEntity.ok(ApiResponse.success("筛选任务获取成功", filteredTasks));
    }
}
