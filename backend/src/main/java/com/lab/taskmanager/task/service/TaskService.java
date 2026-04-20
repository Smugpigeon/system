package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.algorithm.TaskRankingService;
import com.lab.taskmanager.task.dto.PageRequest;
import com.lab.taskmanager.task.dto.TaskAssignRequest;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.dto.TeamTaskCreateRequest;
import com.lab.taskmanager.task.entity.PageResult;
import com.lab.taskmanager.task.entity.SortBy;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.task.repository.TaskSpecifications;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.entity.Team;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.repository.TeamMembershipRepository;
import com.lab.taskmanager.team.repository.TeamRepository;
import com.lab.taskmanager.team.service.TeamAuthorizationService;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.util.List;
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
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    // Get personal tasks (including both personal tasks and assigned tasks) of the current user with sorting and pagination
    @Transactional
    public PageResult<TaskResponse> getDashboardTasks(String username,
                                             TaskStatus status,
                                             TaskPriority priority,
                                             String keyword,
                                             PageRequest pageRequest) {
        Long currentUserId = userService.findByUsernameOrThrow(username).getId();

        if (pageRequest.sortBy() == SortBy.RANK) {
            return getTasksWithRankSorting(currentUserId, null, status, priority,keyword, pageRequest);
        }

        // Handles filtering, sorting, and pagination in database layer
        Specification<Task> spec = TaskSpecifications.buildDashboardTasks(
            currentUserId, status, priority, keyword);

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

    // Get team tasks of the current teamwith sorting and pagination
    @Transactional
    public PageResult<TaskResponse> getTeamTasks(String username,
                                             Long teamId,
                                             TaskStatus status,
                                             TaskPriority priority,
                                             String keyword,
                                             PageRequest pageRequest) {
        // Authenticate: Get current user and check whether the user is a member of the team
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireMembership(teamId, currentUser.getId());

        // Build Specification: Handles filtering, sorting, and pagination in database layer
        Specification<Task> spec = TaskSpecifications.buildTeamTasks(
            teamId, status, priority, keyword);
        
        if (pageRequest.sortBy() == SortBy.RANK) {
            return getTasksWithRankSorting(null, teamId, status, priority,keyword, pageRequest);
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

    private PageResult<TaskResponse> getTasksWithRankSorting(   @Nullable Long currentUserId,
                                                                @Nullable Long teamId,
                                                                TaskStatus status,
                                                                TaskPriority priority,
                                                                String keyword,
                                                                PageRequest pageRequest) {
        
        // Filtering
        Specification<Task> spec = null;
        if (currentUserId != null) {
            spec = TaskSpecifications.buildDashboardTasks(
                currentUserId, status, priority, keyword);
        } else if (teamId != null) {
            spec = TaskSpecifications.buildTeamTasks(
                teamId, status, priority, keyword);
        } else {
            throw new IllegalArgumentException("currentUserId and teamId cannot both be null");
        }
    
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

    // Get a single task by id with the constraint 
    // that user can only access their personal tasks or tasks of the team to which they belong
    @Transactional
    public TaskResponse getTask(String username, Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = findTaskOrThrow(taskId);
        
        if (task.getTeam() != null) {
            teamAuthorizationService.requireMembership(task.getTeam().getId(), currentUser.getId());
        } else {
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenOperationException("无权查看该任务");
            }
        }

        return toResponse(task);
    }

    // ====== Create Task ======
    // Create personal task
    @Transactional
    public TaskResponse createPersonalTask(String username, TaskCreateRequest request) {
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

    // Create team task(needs Admin or Owner role in the team)
    @Transactional
    public TaskResponse createTeamTask(String username, TeamTaskCreateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);

        // Check if the current user is an admin or owner of the team
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("团队不存在"));
        teamAuthorizationService.requireAdminOrOwner(team.getId(), currentUser.getId());

        // Create task
        Task task = new Task();
        task.setOwner(currentUser);
        task.setTeam(team);

        // Check if the current user is an admin or owner of the team and set assignee if provided
        if (request.assigneeId() != null) {
            User assignee = userService.findByIdOrThrow(request.assigneeId());
            if (!teamMembershipRepository.existsByTeamIdAndUserId(team.getId(), request.assigneeId())) {
                throw new BusinessException("分配人不是团队成员");
            }
            task.setAssignee(assignee);
        }
        
        // Apply other changes
        applyTaskChanges(
                task,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueAt());
        
        return toResponse(taskRepository.save(task));
    }

    // ====== Update Task ======
    // Assign team task to a user(needs Admin or Owner role in the team)
    @Transactional
    public TaskResponse assignTask(String username, Long taskId, TaskAssignRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        
        Task task = findTaskOrThrow(taskId);
        if (task.getTeam() == null) {
            throw new BusinessException("个人任务不能分配给其他人");
        }
        
        teamAuthorizationService.requireAdminOrOwner(task.getTeam().getId(), currentUser.getId());
        
        User assignee = userService.findByIdOrThrow(request.assigneeId());
        if (!teamMembershipRepository.existsByTeamIdAndUserId(task.getTeam().getId(), assignee.getId())) {
            throw new BusinessException("分配人不是团队成员");
        }
        
        task.setAssignee(assignee);
        
        return toResponse(taskRepository.save(task));
    }

    // Update task
    @Transactional
    public TaskResponse updateTask(String username, Long taskId, TaskUpdateRequest request) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = findTaskOrThrow(taskId);
        
        // Check if the task is a team task
        if (task.getTeam() != null) {
            // Team task
            TeamMembership membership = teamAuthorizationService.requireMembership(task.getTeam().getId(), currentUser.getId());
            TeamRole role = membership.getRole();
            
            boolean onlyStatusChange = isOnlyStatusChange(task, request);
            
            if (role == TeamRole.MEMBER) {
                if (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId())) {
                    throw new ForbiddenOperationException("只能修改分配给你自己的任务");
                }
                if (!onlyStatusChange) {
                    throw new ForbiddenOperationException("成员只能修改任务状态，不能修改标题、描述等其他字段");
                }
            }
            // Else continue: Admin or Owner role / Member role but only modify status of the task assigned to him
        } else {
            // Personal task
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenOperationException("无权修改该任务");
            }
        }
        
        applyTaskChanges(
                task,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueAt());
        
        return toResponse(taskRepository.save(task));
    }

    private boolean isOnlyStatusChange(Task existing, TaskUpdateRequest request) {
        boolean titleSame = request.title() == null || existing.getTitle().equals(request.title());
        boolean descSame = request.description() == null || existing.getDescription().equals(request.description());
        boolean prioritySame = request.priority() == null || existing.getPriority().equals(request.priority());
        boolean dueAtSame = request.dueAt() == null ||
                (existing.getDueAt() != null && existing.getDueAt().equals(request.dueAt()));
        return titleSame && descSame && prioritySame && dueAtSame && request.status() != null;
    }

    // ====== Delete Task ======
    // Delete task
    @Transactional
    public void deleteTask(String username, Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = findTaskOrThrow(taskId);
        
        if (task.getTeam() != null) {
            teamAuthorizationService.requireAdminOrOwner(task.getTeam().getId(), currentUser.getId());
        } else {
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenOperationException("无权删除该任务");
            }
        }

        taskRepository.delete(task);
    }

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
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
                TeamMembership membership = teamAuthorizationService.requireMembership(
                    task.getTeam().getId(), task.getAssignee().getId());
                assigneeRole = membership.getRole();
            } catch (ResourceNotFoundException e) {
                // The assignee is not a member of the team, ignore it.
                // Do not throw exception, just set assigneeRole to null
                assigneeRole = null;
            }
        }
        return TaskResponse.fromTeamTask(task, assigneeRole);
    }
}
