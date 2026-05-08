package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.dto.TaskDependencyItemResponse;
import com.lab.taskmanager.task.dto.TaskDependencyResponse;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskDependency;
import com.lab.taskmanager.task.entity.TaskScope;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskDependencyRepository;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.service.TeamAuthorizationService;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class TaskDependencyService {

    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final TeamAuthorizationService teamAuthorizationService;

    /**
     * Return predecessor and successor tasks after verifying task visibility.
     *
     * @param username authenticated username
     * @param taskId target task
     * @return dependency graph around the target task
     */
    @Transactional(readOnly = true)
    public TaskDependencyResponse getPersonalDependencies(@NotNull String username, @NotNull Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task task = findPersonalTaskOrThrow(currentUser.getId(), taskId);
        return buildDependencyResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskDependencyResponse getTeamDependencies(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireMembership(teamId, currentUser.getId());
        Task task = findTeamTaskOrThrow(teamId, taskId);
        return buildDependencyResponse(task);
    }

    @Transactional
    public TaskDependencyResponse addPersonalDependency(
            @NotNull String username,
            @NotNull Long taskId,
            @NotNull Long predecessorTaskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task successor = findPersonalTaskOrThrow(currentUser.getId(), taskId);
        Task predecessor = findPersonalTaskOrThrow(currentUser.getId(), predecessorTaskId);
        createDependency(predecessor, successor, currentUser);
        return buildDependencyResponse(successor);
    }

    @Transactional
    public TaskDependencyResponse addTeamDependency(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long taskId,
            @NotNull Long predecessorTaskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireAdminOrOwner(teamId, currentUser.getId());
        Task successor = findTeamTaskOrThrow(teamId, taskId);
        Task predecessor = findTeamTaskOrThrow(teamId, predecessorTaskId);
        createDependency(predecessor, successor, currentUser);
        return buildDependencyResponse(successor);
    }

    @Transactional
    public TaskDependencyResponse removePersonalDependency(
            @NotNull String username,
            @NotNull Long taskId,
            @NotNull Long predecessorTaskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        Task successor = findPersonalTaskOrThrow(currentUser.getId(), taskId);
        findPersonalTaskOrThrow(currentUser.getId(), predecessorTaskId);
        deleteDependency(predecessorTaskId, taskId);
        return buildDependencyResponse(successor);
    }

    @Transactional
    public TaskDependencyResponse removeTeamDependency(
            @NotNull String username,
            @NotNull Long teamId,
            @NotNull Long taskId,
            @NotNull Long predecessorTaskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        teamAuthorizationService.requireAdminOrOwner(teamId, currentUser.getId());
        Task successor = findTeamTaskOrThrow(teamId, taskId);
        findTeamTaskOrThrow(teamId, predecessorTaskId);
        deleteDependency(predecessorTaskId, taskId);
        return buildDependencyResponse(successor);
    }

    public void assertCanDeleteTask(@NotNull Task task) {
        if (taskDependencyRepository.countByPredecessorId(task.getId()) > 0) {
            throw new BusinessException("该任务仍被其他任务依赖，请先移除后继任务依赖");
        }
    }

    public void deleteDependenciesOwnedByTask(@NotNull Long taskId) {
        taskDependencyRepository.deleteAllBySuccessorId(taskId);
    }

    public void assertStatusTransitionAllowed(@NotNull Task task, @NotNull TaskStatus targetStatus) {
        if (task.getId() == null) {
            return;
        }
        if (targetStatus == TaskStatus.DONE && hasUnfinishedPredecessor(task.getId())) {
            throw new BusinessException("存在未完成的前置任务，当前任务不能标记为 DONE");
        }
        if (task.getStatus() == TaskStatus.DONE
                && targetStatus != TaskStatus.DONE
                && taskDependencyRepository.countDoneSuccessors(task.getId(), TaskStatus.DONE) > 0) {
            throw new BusinessException("该任务已有已完成后继任务依赖，不能从 DONE 回退");
        }
    }

    private void createDependency(Task predecessor, Task successor, User currentUser) {
        validateDependencyPair(predecessor, successor);
        if (taskDependencyRepository.existsByPredecessorIdAndSuccessorId(
                predecessor.getId(),
                successor.getId())) {
            throw new BusinessException("该前置任务依赖已经存在");
        }
        if (wouldCreateCycle(predecessor.getId(), successor.getId())) {
            throw new BusinessException("任务依赖不能形成循环");
        }
        if (successor.getStatus() == TaskStatus.DONE && predecessor.getStatus() != TaskStatus.DONE) {
            throw new BusinessException("已完成任务不能新增未完成前置任务");
        }

        TaskDependency dependency = new TaskDependency();
        dependency.setPredecessor(predecessor);
        dependency.setSuccessor(successor);
        dependency.setCreatedBy(currentUser);
        taskDependencyRepository.save(dependency);
    }

    private void validateDependencyPair(Task predecessor, Task successor) {
        if (Objects.equals(predecessor.getId(), successor.getId())) {
            throw new BusinessException("任务不能依赖自己");
        }
        if (predecessor.getScope() != successor.getScope()) {
            throw new BusinessException("个人任务和团队任务之间不能建立依赖");
        }
        if (successor.getScope() == TaskScope.PERSONAL) {
            if (!Objects.equals(predecessor.getOwner().getId(), successor.getOwner().getId())) {
                throw new BusinessException("个人任务只能依赖当前用户自己的个人任务");
            }
            return;
        }
        if (!Objects.equals(predecessor.getTeam().getId(), successor.getTeam().getId())) {
            throw new BusinessException("团队任务只能依赖同一团队中的其他任务");
        }
    }

    private void deleteDependency(Long predecessorTaskId, Long successorTaskId) {
        TaskDependency dependency = taskDependencyRepository
                .findByPredecessorIdAndSuccessorId(predecessorTaskId, successorTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("任务依赖关系不存在"));
        taskDependencyRepository.delete(dependency);
    }

    private boolean wouldCreateCycle(Long predecessorId, Long successorId) {
        return reachesTask(successorId, predecessorId, new HashSet<>());
    }

    private boolean reachesTask(Long currentTaskId, Long targetTaskId, Set<Long> visitedTaskIds) {
        if (Objects.equals(currentTaskId, targetTaskId)) {
            return true;
        }
        if (!visitedTaskIds.add(currentTaskId)) {
            return false;
        }
        return taskDependencyRepository.findAllByPredecessorId(currentTaskId)
                .stream()
                .map(TaskDependency::getSuccessor)
                .anyMatch(successor -> reachesTask(successor.getId(), targetTaskId, visitedTaskIds));
    }

    private TaskDependencyResponse buildDependencyResponse(Task task) {
        List<TaskDependencyItemResponse> predecessors = taskDependencyRepository.findAllBySuccessorId(task.getId())
                .stream()
                .map(TaskDependency::getPredecessor)
                .map(this::toItem)
                .toList();
        List<TaskDependencyItemResponse> successors = taskDependencyRepository.findAllByPredecessorId(task.getId())
                .stream()
                .map(TaskDependency::getSuccessor)
                .map(this::toItem)
                .toList();
        List<TaskDependencyItemResponse> unfinished = predecessors.stream()
                .filter(predecessor -> predecessor.status() != TaskStatus.DONE)
                .toList();
        return new TaskDependencyResponse(
                task.getId(),
                predecessors,
                successors,
                !unfinished.isEmpty(),
                unfinished);
    }

    private TaskDependencyItemResponse toItem(Task task) {
        return new TaskDependencyItemResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getScope(),
                task.getTeam() == null ? null : task.getTeam().getId(),
                task.getAssignee().getUsername());
    }

    private Task findPersonalTaskOrThrow(Long currentUserId, Long taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getScope() == TaskScope.PERSONAL)
                .filter(task -> Objects.equals(task.getOwner().getId(), currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("个人任务不存在，或你无权访问该任务"));
    }

    private Task findTeamTaskOrThrow(Long teamId, Long taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getScope() == TaskScope.TEAM)
                .filter(task -> task.getTeam() != null)
                .filter(task -> Objects.equals(task.getTeam().getId(), teamId))
                .orElseThrow(() -> new ResourceNotFoundException("团队任务不存在，或不属于当前团队"));
    }

    private boolean hasUnfinishedPredecessor(Long taskId) {
        return taskDependencyRepository.countUnfinishedPredecessors(taskId, TaskStatus.DONE) > 0;
    }
}
