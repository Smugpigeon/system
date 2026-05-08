package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskDependencyRepository;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.team.service.TeamAuthorizationService;
import com.lab.taskmanager.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Mock
    private TeamAuthorizationService teamAuthorizationService;

    @InjectMocks
    private TaskDependencyService taskDependencyService;

    @Test
    void statusTransitionShouldRejectDoneWhenPredecessorIsUnfinished() {
        Task task = task(10L, TaskStatus.IN_PROGRESS);
        when(taskDependencyRepository.countUnfinishedPredecessors(10L, TaskStatus.DONE)).thenReturn(1L);

        assertThrows(
                BusinessException.class,
                () -> taskDependencyService.assertStatusTransitionAllowed(task, TaskStatus.DONE));
    }

    @Test
    void statusTransitionShouldAllowDoneWhenAllPredecessorsAreDone() {
        Task task = task(10L, TaskStatus.IN_PROGRESS);
        when(taskDependencyRepository.countUnfinishedPredecessors(10L, TaskStatus.DONE)).thenReturn(0L);

        assertDoesNotThrow(() -> taskDependencyService.assertStatusTransitionAllowed(task, TaskStatus.DONE));
    }

    @Test
    void deleteShouldRejectTaskThatStillHasSuccessors() {
        Task task = task(20L, TaskStatus.TODO);
        when(taskDependencyRepository.countByPredecessorId(20L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> taskDependencyService.assertCanDeleteTask(task));
    }

    private Task task(Long id, TaskStatus status) {
        Task task = new Task();
        task.setId(id);
        task.setStatus(status);
        return task;
    }
}
