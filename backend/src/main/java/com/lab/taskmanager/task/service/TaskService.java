package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public List<TaskResponse> listTasks(String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        return taskRepository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTask(String username, Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        return toResponse(findTaskOrThrow(currentUser.getId(), taskId));
    }

    public TaskResponse createTask(String username, TaskCreateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);

        Task task = new Task();
        task.setOwner(currentUser);
        applyTaskChanges(
                task,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueAt());

        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateTask(String username, Long taskId, TaskUpdateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = findTaskOrThrow(currentUser.getId(), taskId);
        applyTaskChanges(
                task,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueAt());

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(String username, Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = findTaskOrThrow(currentUser.getId(), taskId);
        taskRepository.delete(task);
    }

    public PageResult<TaskResponse> page(PageRequest pageRequest, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        int pageNumber = Math.max(0, pageRequest.page() - 1);
        int pageSize = pageRequest.size();

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(
                        pageNumber,
                        pageSize,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC,
                                "updatedAt"));

        org.springframework.data.domain.Page<Task> taskPage =
                taskRepository.findAllByOwnerId(currentUser.getId(), pageable);

        List<TaskResponse> records = taskPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResult<>(
                (int) taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                pageRequest.page(),
                pageSize,
                records);
    }

    public List<TaskResponse> getFilteredTasks(String username, TaskStatus status, TaskPriority priority) {
        User currentUser = userService.findByUsernameOrThrow(username);
        List<Task> result;

        if (status == null && priority == null) {
            result = taskRepository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.getId());
        } else if (status != null && priority == null) {
            result = taskRepository.findByOwnerIdAndStatusOrderByUpdatedAtDesc(currentUser.getId(), status);
        } else if (priority != null && status == null) {
            result = taskRepository.findByOwnerIdAndPriorityOrderByUpdatedAtDesc(currentUser.getId(), priority);
        } else {
            result = taskRepository.findByOwnerIdAndStatusAndPriorityOrderByUpdatedAtDesc(
                    currentUser.getId(),
                    status,
                    priority);
        }

        return result.stream().map(this::toResponse).toList();
    }

    private Task findTaskOrThrow(Long ownerId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在，或你无权访问该任务"));
    }

    private void applyTaskChanges(
            Task task,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            java.time.LocalDateTime dueAt) {
        task.setTitle(title.trim());
        task.setDescription(description == null ? "" : description.trim());
        task.setStatus(status == null ? TaskStatus.TODO : status);
        task.setPriority(priority == null ? TaskPriority.MEDIUM : priority);
        task.setDueAt(dueAt);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
