package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.algorithm.TaskRankingService;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.SortBy;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.task.repository.TaskSpecifications;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.service.TeamAuthorizationService;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import java.util.List;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;
    private final TaskRankingService taskRankingService;
    private final TeamAuthorizationService teamAuthorizationService;

    // Get private tasks of the current user with sorting and pagination
    // Needs to be updated to return assigned tasks as well
    public PageResult<TaskResponse> listTasks(String username,
                                             TaskStatus status,
                                             TaskPriority priority,
                                             String keyword,
                                             PageRequest pageRequest) {
        User currentUser = userService.findByUsernameOrThrow(username);

        if (pageRequest.sortBy() == SortBy.RANK) {
            return listTasksWithRankSorting(currentUser, status, priority,keyword, pageRequest);
        }

        // Establish Sorting
        Sort.Direction direction = Sort.Direction.DESC;
        String sortField = pageRequest.sortBy().getValue();
        if (pageRequest.sortBy() == SortBy.DUE_AT || pageRequest.sortBy() == SortBy.STATUS) {
            direction = Sort.Direction.ASC;
        }

        // Establish Page Request
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.page() - 1, 
            pageRequest.size(),
            Sort.by(direction, sortField)
        );

        // Handles filtering, sorting, and pagination in database layer
        Specification<Task> spec = TaskSpecifications.buildSpecification(
            currentUser.getId(), status, priority, keyword);
    
        Page<Task> taskPage = taskRepository.findAll(spec, pageable);

        List<TaskResponse> records = taskPage.getContent()
            .stream()
            .map(this::toResponse)
            .toList();

        return new PageResult<>(
            (int) taskPage.getTotalElements(),
            taskPage.getTotalPages(),
            pageRequest.page(),
            pageRequest.size(),
            records);
    }

    private PageResult<TaskResponse> listTasksWithRankSorting(User currentUser,
                                                                TaskStatus status,
                                                                TaskPriority priority,
                                                                String keyword,
                                                                PageRequest pageRequest) {
        
        // Filtering
        Specification<Task> spec = TaskSpecifications.buildSpecification(
            currentUser.getId(), status, priority, keyword);
    
        List<Task> filteredTasks = taskRepository.findAll(spec);
        
        // Sorting
        List<Task> sortedTasks = taskRankingService.sortTasks(filteredTasks);
        
        // Paging
        int totalRecords = sortedTasks.size();
        int totalPages = totalRecords == 0
                ? 0
                : (int) Math.ceil((double) totalRecords / pageRequest.size());
        int start = (pageRequest.page() - 1) * pageRequest.size();
        if (start >= totalRecords) {
            return new PageResult<>(
                    totalRecords,
                    totalPages,
                    pageRequest.page(),
                    pageRequest.size(),
                    java.util.Collections.emptyList()
            );
        }
        int end = Math.min(start + pageRequest.size(), totalRecords);
        List<Task> pagedTasks = sortedTasks.subList(start, end);
        
        return new PageResult<>(
                totalRecords,
                totalPages,
                pageRequest.page(),
                pageRequest.size(),
                pagedTasks.stream().map(this::toResponse).toList()
        );
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
        if (task.getTeam() == null) {
            return TaskResponse.fromPersonalTask(task);
        }
        TeamRole assigneeRole = null;
        if (task.getAssignee() != null) {
            try {
                assigneeRole = teamAuthorizationService
                    .requireMembership(task.getTeam().getId(), task.getAssignee().getId())
                    .getRole();
            } catch (ResourceNotFoundException e) {
                // Assignee is not a member of the team, ignore
                assigneeRole = null;
            }
        }
        return TaskResponse.fromTeamTask(task, assigneeRole);
    }
}
