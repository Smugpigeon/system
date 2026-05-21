package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.common.exception.ForbiddenOperationException;
import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.dto.DependencyResponse;
import com.lab.taskmanager.task.dto.DependencyTaskInfo;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskDependency;
import com.lab.taskmanager.task.entity.TaskScope;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskDependencyRepository;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.entity.TeamMembership;
import com.lab.taskmanager.team.entity.TeamRole;
import com.lab.taskmanager.team.service.TeamAuthorizationService;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final UserService userService;
    private final TeamAuthorizationService teamAuthorizationService;

    // ================= Public API =================

    @Transactional
    public void addDependency(String username, Long taskId, Long predecessorTaskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        
        Task successorTask = getTaskWithAccess(currentUser, taskId, true);
        Task predecessorTask = getTaskWithAccess(currentUser, predecessorTaskId, true);
        
        validateSelfDependency(taskId, predecessorTaskId);
        validateDependencyCompatibility(successorTask, predecessorTask);
        validateDuplicateDependency(taskId, predecessorTaskId);
        validateNoCycle(predecessorTaskId, taskId);
        
        TaskDependency dependency = new TaskDependency();
        dependency.setPredecessorTaskId(predecessorTaskId);
        dependency.setSuccessorTaskId(taskId);
        taskDependencyRepository.save(dependency);
    }

    @Transactional
    public void removeDependency(String username, Long taskId, Long predecessorTaskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        
        getTaskWithAccess(currentUser, taskId, true);
        
        TaskDependency dependency = taskDependencyRepository
                .findByPredecessorTaskIdAndSuccessorTaskId(predecessorTaskId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("依赖关系不存在"));
        
        taskDependencyRepository.delete(dependency);
    }

    @Transactional(readOnly = true)
    public DependencyResponse getDependencies(String username, Long taskId) {
        User currentUser = userService.findByUsernameOrThrow(username);
        
        getTaskWithAccess(currentUser, taskId, false);
        
        List<DependencyTaskInfo> predecessors = taskDependencyRepository.findAllBySuccessorTaskId(taskId)
                .stream()
                .map(dep -> toDependencyTaskInfo(dep.getPredecessorTaskId()))
                .toList();
        
        List<DependencyTaskInfo> successors = taskDependencyRepository.findAllByPredecessorTaskId(taskId)
                .stream()
                .map(dep -> toDependencyTaskInfo(dep.getSuccessorTaskId()))
                .toList();
        
        return new DependencyResponse(predecessors, successors);
    }

    @Transactional(readOnly = true)
    public boolean hasIncompletePredecessors(Long taskId) {
        return taskDependencyRepository.findAllBySuccessorTaskId(taskId)
                .stream()
                .map(dep -> taskRepository.findById(dep.getPredecessorTaskId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(predecessor -> predecessor.getStatus() != TaskStatus.DONE);
    }

    @Transactional(readOnly = true)
    public List<Task> getIncompletePredecessors(Long taskId) {
        return taskDependencyRepository.findAllBySuccessorTaskId(taskId)
                .stream()
                .map(dep -> taskRepository.findById(dep.getPredecessorTaskId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(predecessor -> predecessor.getStatus() != TaskStatus.DONE)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasSuccessors(Long taskId) {
        return !taskDependencyRepository.findAllByPredecessorTaskId(taskId).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Task> getSuccessors(Long taskId) {
        return taskDependencyRepository.findAllByPredecessorTaskId(taskId)
                .stream()
                .map(dep -> taskRepository.findById(dep.getSuccessorTaskId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Transactional
    public void deleteAllDependenciesByTaskId(Long taskId) {
        if (hasSuccessors(taskId)) {
            List<Task> successors = getSuccessors(taskId);
            String titles = successors.stream()
                    .map(Task::getTitle)
                    .collect(Collectors.joining("、"));
            
            throw new BusinessException("该任务被以下后继任务依赖：" + titles + "，请先删除这些依赖关系");
        }
        taskDependencyRepository.deleteAllByTaskId(taskId);
    }

    // ================= Private Validations =================

    private void validateSelfDependency(Long taskId, Long predecessorTaskId) {
        if (taskId.equals(predecessorTaskId)) {
            throw new BusinessException("不能添加任务依赖自身");
        }
    }

    private void validateDuplicateDependency(Long taskId, Long predecessorTaskId) {
        if (taskDependencyRepository.findByPredecessorTaskIdAndSuccessorTaskId(predecessorTaskId, taskId).isPresent()) {
            throw new BusinessException("依赖关系已存在");
        }
    }

    private void validateDependencyCompatibility(Task successor, Task predecessor) {
        if (successor.getScope() != predecessor.getScope()) {
            throw new BusinessException("个人任务和团队任务之间不能建立依赖关系");
        }
        
        if (successor.getScope() == TaskScope.PERSONAL) {
            if (!successor.getOwner().getId().equals(predecessor.getOwner().getId())) {
                throw new BusinessException("个人任务只能依赖自己的其他个人任务");
            }
        } else {
            if (!successor.getTeam().getId().equals(predecessor.getTeam().getId())) {
                throw new BusinessException("团队任务只能依赖同一团队的其它任务");
            }
        }
    }

    private void validateNoCycle(Long fromTaskId, Long toTaskId) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(toTaskId);
        
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);
            
            if (current.equals(fromTaskId)) {
                throw new BusinessException("添加此依赖将形成循环依赖，不允许操作");
            }
            
            taskDependencyRepository.findAllByPredecessorTaskId(current)
                    .stream()
                    .map(TaskDependency::getSuccessorTaskId)
                    .filter(id -> !visited.contains(id))
                    .forEach(queue::add);
        }
    }

    // ================= Permission & Helper =================

    private Task getTaskWithAccess(User currentUser, Long taskId, boolean needWrite) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
        
        if (task.getScope() == TaskScope.PERSONAL) {
            if (needWrite) {
                if (!task.getOwner().getId().equals(currentUser.getId())) {
                    throw new ForbiddenOperationException("只有任务创建者可以管理依赖关系");
                }
            } else {
                if (!task.getOwner().getId().equals(currentUser.getId())) {
                    throw new ForbiddenOperationException("无权查看此个人任务");
                }
            }
            return task;
        } else {
            TeamMembership membership = teamAuthorizationService.requireMembership(
                    task.getTeam().getId(), currentUser.getId());
            
            if (needWrite) {
                if (membership.getRole() == TeamRole.MEMBER) {
                    throw new ForbiddenOperationException("只有团队管理员或拥有者可以管理任务依赖关系");
                }
            }
            return task;
        }
    }

    private DependencyTaskInfo toDependencyTaskInfo(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
        return new DependencyTaskInfo(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getOwner().getUsername(),
                task.getAssignee() == null ? null : task.getAssignee().getUsername()
        );
    }
}
