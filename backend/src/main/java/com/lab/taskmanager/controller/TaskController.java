package com.lab.taskmanager.task.controller;

import com.lab.taskmanager.common.api.ApiResponse;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listTasks(Authentication authentication) {
        String username = authentication.getName();
        List<TaskResponse> tasks = taskService.listTasks(username);
        return ResponseEntity.ok(ApiResponse.success("获取任务列表成功", tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        TaskResponse task = taskService.getTask(username, id);
        return ResponseEntity.ok(ApiResponse.success("获取任务详情成功", task));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        TaskResponse task = taskService.createTask(username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建任务成功", task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        TaskResponse task = taskService.updateTask(username, id, request);
        return ResponseEntity.ok(ApiResponse.success("更新任务成功", task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        taskService.deleteTask(username, id);
        return ResponseEntity.ok(ApiResponse.success("删除任务成功", null));
    }
}