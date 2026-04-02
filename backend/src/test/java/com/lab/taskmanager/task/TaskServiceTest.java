package com.lab.taskmanager.task.service;

import com.lab.taskmanager.common.exception.ResourceNotFoundException;
import com.lab.taskmanager.task.dto.TaskCreateRequest;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.dto.TaskUpdateRequest;
import com.lab.taskmanager.task.algorithm.TaskRankingService;
import com.lab.taskmanager.task.entity.Task;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.repository.TaskRepository;
import com.lab.taskmanager.user.entity.User;
import com.lab.taskmanager.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @Mock
    private TaskRankingService taskRankingService;

    @InjectMocks
    private TaskService taskService;

    private final String USERNAME = "testuser";
    private final Long USER_ID = 1L;
    private final Long TASK_ID = 10L;

    private User createTestUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        return user;
    }

    private Task createTestTask(Long id, String title) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription("Desc " + title);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setOwner(createTestUser());
        task.setDueAt(LocalDateTime.now().plusDays(1));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    // ========== listTasks ==========

    @Test
    void listTasks_shouldReturnUserTasks() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        
        Task task1 = createTestTask(1L, "Task 1");
        Task task2 = createTestTask(2L, "Task 2");
        when(taskRepository.findAllByOwnerIdOrderByUpdatedAtDesc(USER_ID))
                .thenReturn(List.of(task1, task2));

        List<TaskResponse> result = taskService.listTasks(USERNAME);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Task 1");
        assertThat(result.get(1).title()).isEqualTo("Task 2");
    }

    // ========== listRecommendedTasks ==========

    @Test
    void listRecommendedTasks_shouldReturnRankedTasks() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        
        Task task1 = createTestTask(1L, "High Priority");
        Task task2 = createTestTask(2L, "Low Priority");
        when(taskRepository.findAllByOwnerIdOrderByUpdatedAtDesc(USER_ID))
                .thenReturn(List.of(task2, task1));
        when(taskRankingService.sortTasks(any())).thenReturn(List.of(task1, task2));

        List<TaskResponse> result = taskService.listRecommendedTasks(USERNAME);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("High Priority");
    }

    // ========== getTask ==========

    @Test
    void getTask_shouldReturnTask_whenOwnerAccesses() {
        User user = createTestUser();
        Task task = createTestTask(TASK_ID, "My Task");
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID))
                .thenReturn(Optional.of(task));

        TaskResponse result = taskService.getTask(USERNAME, TASK_ID);

        assertThat(result.id()).isEqualTo(TASK_ID);
        assertThat(result.title()).isEqualTo("My Task");
    }

    @Test
    void getTask_shouldThrow_whenTaskNotFound() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(USERNAME, TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("任务不存在");
    }

    // ========== createTask ==========

    @Test
    void createTask_shouldSaveAndReturnTask() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        
        Task savedTask = createTestTask(TASK_ID, "  New Task  ");
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskCreateRequest request = new TaskCreateRequest(
                "  New Task  ",
                "Description",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(3)
        );

        TaskResponse result = taskService.createTask(USERNAME, request);

        assertThat(result.title()).isEqualTo("New Task"); // trim验证
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldUseDefaultStatusAndPriority_whenNull() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(TASK_ID);
            return t;
        });

        TaskCreateRequest request = new TaskCreateRequest(
                "Task",
                null,
                null,
                null,
                null
        );

        TaskResponse result = taskService.createTask(USERNAME, request);

        assertThat(result.status()).isEqualTo(TaskStatus.TODO);
        assertThat(result.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(result.description()).isEqualTo("");
    }

    // ========== updateTask ==========

    @Test
    void updateTask_shouldUpdateAndReturnTask() {
        User user = createTestUser();
        Task existingTask = createTestTask(TASK_ID, "Old Title");
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID))
                .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Updated Title",
                "Updated Desc",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(5)
        );

        TaskResponse result = taskService.updateTask(USERNAME, TASK_ID, request);

        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTask_shouldThrow_whenTaskNotFound() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Title", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null
        );

        assertThatThrownBy(() -> taskService.updateTask(USERNAME, TASK_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ========== deleteTask ==========

    @Test
    void deleteTask_shouldDelete_whenOwnerAccesses() {
        User user = createTestUser();
        Task task = createTestTask(TASK_ID, "To Delete");
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID))
                .thenReturn(Optional.of(task));

        taskService.deleteTask(USERNAME, TASK_ID);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_shouldThrow_whenTaskNotFound() {
        User user = createTestUser();
        when(userService.findByUsernameOrThrow(USERNAME)).thenReturn(user);
        when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(USERNAME, TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(taskRepository, never()).delete(any());
    }
}