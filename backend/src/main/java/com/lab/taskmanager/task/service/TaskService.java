package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.algorithm.TaskRankingService;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.dto.TeamTaskStatusUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskUpdateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.SortBy;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskScope;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.task.spec.TaskSpecifications;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.service.TeamAuthorizationService;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;
    private final TaskRankingService taskRankingService;
    private final TeamAuthorizationService teamAuthorizationService;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TaskDependencyService taskDependencyService;

    // ================= DashBoard Scope =================
    // Person tasks of current user and team tasks assigned to the current user

    /**
     * Return the current user's personal tasks together with team tasks assigned to them.
     *
     * @param username authenticated username
     * @param status optional status filter
     * @param priority optional priority filter
     * @param keyword optional keyword filter
     * @param pageRequest pagination and sorting request
     * @return paged dashboard tasks
     */
    @Transactional(readOnly = true)
    public PageResult<TaskResponse> listTasks(
            @NotNull String username,
            @Nullable TaskStatus status,
            @Nullable TaskPriority priority,
            @Nullable String keyword,
            @NotNull PageRequest pageRequest) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Map<Long, TeamMembership> membershipIndex = buildMembershipIndex(currentUser);
        Specification<Task> specification = TaskSpecifications.dashboardVisibleTo(
                        currentUser.getId(),
                        membershipIndex.keySet())
                .and(TaskSpecifications.withStatus(status))
                .and(TaskSpecifications.withPriority(priority))
                .and(TaskSpecifications.withKeyword(keyword));
        return pageAndMap(
                specification,
                pageRequest,
                task -> toResponse(task, currentUser, membershipIndex.get(taskTeamId(task))));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(@NotNull String username, @NotNull Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Map<Long, TeamMembership> membershipIndex = buildMembershipIndex(currentUser);
        Task task = taskRepository.findOne(TaskSpecifications.dashboardVisibleTo(
                                currentUser.getId(),
                                membershipIndex.keySet())
                        .and(TaskSpecifications.withId(taskId)))
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在，或你无权访问该任务"));
        TeamMembership membership = membershipIndex.get(taskTeamId(task));
        return toResponse(task, currentUser, membership);
    }

    @Transactional
    public TaskResponse createTask(@NotNull String username, @NotNull TaskCreateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = new Task();
        task.setScope(TaskScope.PERSONAL);
        task.setOwner(currentUser);
        task.setAssignee(currentUser);
        task.setTeam(null);
        applyTaskChanges(task, request.title(), request.description(), request.status(), request.priority(), request.dueAt());
        return toResponse(taskRepository.save(task), currentUser, null);
    }

    @Transactional
    public TaskResponse updateTask(
            @NotNull String username,
            @NotNull Long taskId,
            @NotNull TaskUpdateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = taskRepository.findOne(TaskSpecifications.personalTaskOwnedBy(currentUser.getId())
                        .and(TaskSpecifications.withId(taskId)))
                .orElseThrow(() -> findPersonalTaskFailure(currentUser.getId(), taskId));

        checkPredecessorStatus(task.getStatus(), taskId, request.status());
        
        applyTaskChanges(task, request.title(), request.description(), request.status(), request.priority(), request.dueAt());
        return toResponse(taskRepository.save(task), currentUser, null);
    }

    @Transactional
    public void deleteTask(@NotNull String username, @NotNull Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = taskRepository.findOne(TaskSpecifications.personalTaskOwnedBy(currentUser.getId())
                        .and(TaskSpecifications.withId(taskId)))
                .orElseThrow(() -> findPersonalTaskFailure(currentUser.getId(), taskId));
        
        // delete all dependency by taskId
        // Caution: only when task have no successor tasks can be deleted
        // deleteAllDependenciesByTaskId() will check whether this task has successor tasks
        taskDependencyService.deleteAllDependenciesByTaskId(taskId);
        taskRepository.delete(task);
    }

    // ================= Team Tasks Scope =================

    @Transactional(readOnly = true)
    public PageResult<TaskResponse> listTeamTasks(
            @NotNull String username,
            @NotNull Long teamId,
            @Nullable TaskStatus status,
            @Nullable TaskPriority priority,
            @Nullable String keyword,
            @NotNull PageRequest pageRequest) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership membership = teamAuthorizationService.requireMembership(teamId, currentUser.getId());
        Specification<Task> specification = TaskSpecifications.teamTasks(teamId)
                .and(TaskSpecifications.withStatus(status))
                .and(TaskSpecifications.withPriority(priority))
                .and(TaskSpecifications.withKeyword(keyword));
        return pageAndMap(
                specification,
                pageRequest,
                task -> toResponse(task, currentUser, membership));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTeamTask(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership membership = teamAuthorizationService.requireMembership(teamId, currentUser.getId());
        Task task = findTeamTaskOrThrow(teamId, taskId);
        return toResponse(task, currentUser, membership);
    }

    @Transactional
    public TaskResponse createTeamTask(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull TeamTaskCreateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership membership = teamAuthorizationService.requireAdminOrOwner(teamId, currentUser.getId());
        User assignee = resolveTeamAssignee(teamId, request.assigneeId());

        Task task = new Task();
        task.setScope(TaskScope.TEAM);
        task.setTeam(membership.getTeam());
        task.setOwner(currentUser);
        task.setAssignee(assignee);
        applyTaskChanges(task, request.title(), request.description(), request.status(), request.priority(), request.dueAt());
        return toResponse(taskRepository.save(task), currentUser, membership);
    }

    @Transactional
    public TaskResponse updateTeamTask(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long taskId,
            @NotNull TeamTaskUpdateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership membership = teamAuthorizationService.requireAdminOrOwner(teamId, currentUser.getId());
        Task task = findTeamTaskOrThrow(teamId, taskId);
        User assignee = resolveTeamAssignee(teamId, request.assigneeId());

        checkPredecessorStatus(task.getStatus(), taskId, request.status());

        applyTaskChanges(task, request.title(), request.description(), request.status(), request.priority(), request.dueAt());
        task.setAssignee(assignee);
        return toResponse(taskRepository.save(task), currentUser, membership);
    }

    @Transactional
    public TaskResponse updateTeamTaskStatus(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long taskId,
            @NotNull TeamTaskStatusUpdateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        TeamMembership membership = teamAuthorizationService.requireMembership(teamId, currentUser.getId());
        Task task = findTeamTaskOrThrow(teamId, taskId);

        if (membership.getRole() == TeamRole.MEMBER
                && (task.getAssignee() == null || !Objects.equals(task.getAssignee().getId(), currentUser.getId()))) {
            throw new ForbiddenOperationException("团队成员只能修改分配给自己的任务状态");
        }

        checkPredecessorStatus(task.getStatus(), taskId, request.status());

        task.setStatus(request.status());
        return toResponse(taskRepository.save(task), currentUser, membership);
    }

    @Transactional
    public void deleteTeamTask(@NotNull String username, @NotNull Long teamId, @NotNull Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireAdminOrOwner(teamId, currentUser.getId());
        Task task = findTeamTaskOrThrow(teamId, taskId);
        
        // delete all dependency by taskId
        // Caution: only when task have no successor tasks can be deleted
        // deleteAllDependenciesByTaskId() will check whether this task has successor tasks
        taskDependencyService.deleteAllDependenciesByTaskId(taskId);
        taskRepository.delete(task);
    }

    // =================== Private Helper Methods ==================

    private void checkPredecessorStatus(TaskStatus taskStatus, Long taskId, TaskStatus requestStatus) {
        if (requestStatus == TaskStatus.DONE && taskStatus != TaskStatus.DONE) {
            if (taskDependencyService.hasIncompletePredecessors(taskId)) {
                List<Task> incomplete = taskDependencyService.getIncompletePredecessors(taskId);
                String titles = incomplete.stream()
                            .map(Task::getTitle)
                            .collect(Collectors.joining("、"));
                    throw new BusinessException("该任务存在未完成的前置任务：" + titles + "，请先完成这些任务");
            }
        }
    }
    
    private RuntimeException findPersonalTaskFailure(Long currentUserId, Long taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getScope() == TaskScope.TEAM)
                .filter(task -> task.getTeam() != null
                        && teamMembershipRepository.existsByTeamIdAndUserId(
                                task.getTeam().getId(), currentUserId))
                .<RuntimeException>map(task -> new ForbiddenOperationException("团队任务请在团队空间中按角色权限进行修改"))
                .orElse(new ResourceNotFoundException("任务不存在，或你无权访问该任务"));
    }

    private Task findTeamTaskOrThrow(Long teamId, Long taskId) {
        return taskRepository.findOne(TaskSpecifications.teamTasks(teamId)
                        .and(TaskSpecifications.withId(taskId)))
                .orElseThrow(() -> new ResourceNotFoundException("团队任务不存在，或不属于当前团队"));
    }

    private User resolveTeamAssignee(Long teamId, Long assigneeId) {
        return teamMembershipRepository.findByTeamIdAndUserId(teamId, assigneeId)
                .map(TeamMembership::getUser)
                .orElseThrow(() -> new BusinessException("被分配用户不是该团队成员"));
    }

    private void applyTaskChanges(
            Task task,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDateTime dueAt) {
        task.setTitle(title.trim());
        task.setDescription(description == null ? "" : description.trim());
        task.setStatus(status == null ? TaskStatus.TODO : status);
        task.setPriority(priority == null ? TaskPriority.MEDIUM : priority);
        task.setDueAt(dueAt);
    }

    private List<Task> sortTasks(List<Task> tasks, SortBy sortBy) {
        return switch (sortBy) {
            case RANK -> taskRankingService.sortTasks(tasks);
            case DUE_AT -> tasks.stream()
                    .sorted(Comparator
                            .comparing(Task::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Task::getUpdatedAt, Comparator.reverseOrder()))
                    .toList();
            case CREATED_AT -> tasks.stream()
                    .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.reverseOrder()))
                    .toList();
            case PRIORITY -> tasks.stream()
                    .sorted(Comparator
                            .comparingInt((Task task) -> priorityWeight(task.getPriority()))
                            .reversed()
                            .thenComparing(Task::getUpdatedAt, Comparator.reverseOrder()))
                    .toList();
            case STATUS -> tasks.stream()
                    .sorted(Comparator
                            .comparingInt((Task task) -> statusWeight(task.getStatus()))
                            .thenComparing(Task::getUpdatedAt, Comparator.reverseOrder()))
                    .toList();
            case UPDATED_AT -> tasks.stream()
                    .sorted(Comparator.comparing(Task::getUpdatedAt, Comparator.reverseOrder()))
                    .toList();
        };
    }

    private PageResult<TaskResponse> paginateAndMap(
            List<Task> tasks,
            PageRequest pageRequest,
            Function<Task, TaskResponse> mapper) {
        int totalRecords = tasks.size();
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
                    java.util.Collections.emptyList());
        }

        int end = Math.min(start + pageRequest.size(), totalRecords);
        List<TaskResponse> records = tasks.subList(start, end)
                .stream()
                .map(mapper)
                .toList();

        return new PageResult<>(
                totalRecords,
                totalPages,
                pageRequest.page(),
                pageRequest.size(),
                records);
    }

    private PageResult<TaskResponse> pageAndMap(
            Specification<Task> specification,
            PageRequest pageRequest,
            Function<Task, TaskResponse> mapper) {
        if (!supportsDatabasePaging(pageRequest.sortBy())) {
            List<Task> sortedTasks = sortTasks(taskRepository.findAll(specification), pageRequest.sortBy());
            return paginateAndMap(sortedTasks, pageRequest, mapper);
        }

        Page<Task> taskPage = taskRepository.findAll(specification, toPageable(pageRequest));
        List<TaskResponse> records = taskPage.getContent()
                .stream()
                .map(mapper)
                .toList();
        return new PageResult<>(
                Math.toIntExact(taskPage.getTotalElements()),
                taskPage.getTotalPages(),
                pageRequest.page(),
                pageRequest.size(),
                records);
    }

    private boolean supportsDatabasePaging(SortBy sortBy) {
        return sortBy == SortBy.UPDATED_AT
                || sortBy == SortBy.CREATED_AT
                || sortBy == SortBy.DUE_AT;
    }

    private Pageable toPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.page() - 1,
                pageRequest.size(),
                toDatabaseSort(pageRequest.sortBy()));
    }

    private Sort toDatabaseSort(SortBy sortBy) {
        return switch (sortBy) {
            case DUE_AT -> Sort.by(Sort.Order.asc("dueAt").nullsLast(), Sort.Order.desc("updatedAt"));
            case CREATED_AT -> Sort.by(Sort.Order.desc("createdAt"));
            case UPDATED_AT -> Sort.by(Sort.Order.desc("updatedAt"));
            case RANK, PRIORITY, STATUS -> Sort.unsorted();
        };
    }

    private Map<Long, TeamMembership> buildMembershipIndex(User currentUser) {
        return teamMembershipRepository.findAllByUserId(currentUser.getId())
                .stream()
                .collect(Collectors.toMap(membership -> membership.getTeam().getId(), Function.identity()));
    }

    private TaskResponse toResponse(Task task, User currentUser, TeamMembership teamMembership) {
        boolean isPersonal = task.getScope() == TaskScope.PERSONAL;
        boolean canEditDetails = isPersonal;
        boolean canEditStatus = isPersonal;
        boolean canDelete = isPersonal;

        if (task.getScope() == TaskScope.TEAM && teamMembership != null) {
            if (teamMembership.getRole() == TeamRole.OWNER || teamMembership.getRole() == TeamRole.ADMIN) {
                canEditDetails = true;
                canEditStatus = true;
                canDelete = true;
            } else {
                canEditDetails = false;
                canDelete = false;
                canEditStatus = task.getAssignee() != null
                        && Objects.equals(task.getAssignee().getId(), currentUser.getId());
            }
        }

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getScope(),
                taskTeamId(task),
                task.getTeam() == null ? null : task.getTeam().getName(),
                task.getOwner().getId(),
                task.getOwner().getUsername(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getUsername(),
                canEditDetails,
                canEditStatus,
                canDelete);
    }

    private Long taskTeamId(Task task) {
        return task.getTeam() == null ? null : task.getTeam().getId();
    }

    private int priorityWeight(TaskPriority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int statusWeight(TaskStatus status) {
        return switch (status) {
            case TODO -> 1;
            case IN_PROGRESS -> 2;
            case DONE -> 3;
        };
    }
}
