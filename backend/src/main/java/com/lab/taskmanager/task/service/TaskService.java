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

    public PageResult<TaskResponse> page(PageRequest pageRequest, String username) {
        User currentUser = userService.findByUsernameOrThrow(username);
        int pageNumber = Math.max(0, pageRequest.page() - 1);
        int pageSize = pageRequest.size();
        SortBy sortBy = pageRequest.sortBy();

        List<Task> allTasks = taskRepository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.getId());
        List<Task> sortedTasks = applySorting(allTasks, sortBy);
        
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, sortedTasks.size());
        List<Task> pagedTasks = start < sortedTasks.size() ? 
                                    sortedTasks.subList(start, end) : 
                                    java.util.Collections.emptyList();
        
        List<TaskResponse> records = pagedTasks.stream()
                .map(this::toResponse)
                .toList();
        
        return new PageResult<>(
                sortedTasks.size(),
                (int) Math.ceil((double) sortedTasks.size() / pageSize),
                pageRequest.page(),
                pageSize,
                records);
    }

    public List<TaskResponse> getFilteredTasks(String username, TaskStatus status, TaskPriority priority, SortBy sortBy) {
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

        List<Task> sortedResult = applySorting(result, sortBy);

        return sortedResult.stream().map(this::toResponse).toList();
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

    /**
     * Sorts the given task list based on the specified sorting criteria.
     * 
     * @param tasks the list of tasks to be sorted
     * @param sortBy the sorting criteria, supports:
     *               - rank: intelligent sorting based on priority, status, and due date
     *               - dueAt: ascending by due date (most urgent first, null values last)
     *               - createdAt: descending by creation date (newest first)
     *               - priority: descending by priority (HIGH > MEDIUM > LOW)
     *               - status: ascending by status
     *               - updatedAt: descending by update time (default, newest first)
     * @return the sorted list of tasks
     */
    private List<Task> applySorting(List<Task> tasks, SortBy sortBy) {

        if (tasks == null || tasks.isEmpty()) {
            return tasks;
        }
    
        switch (sortBy) {
            case RANK:
                return taskRankingService.sortTasks(tasks);
            
            case DUE_AT:
                return tasks.stream()
                        .sorted(Comparator.comparing(Task::getDueAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();

            case CREATED_AT:
                return tasks.stream()
                        .sorted(Comparator.comparing(Task::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .toList();

            case PRIORITY:
                return tasks.stream()
                        .sorted(Comparator.comparing(Task::getPriority,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .toList();

            case STATUS:
                return tasks.stream()
                        .sorted(Comparator.comparing(Task::getStatus,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();

            default: // updatedAt
                return tasks.stream()
                        .sorted(Comparator.comparing(Task::getUpdatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .toList();
        }
    }
}
