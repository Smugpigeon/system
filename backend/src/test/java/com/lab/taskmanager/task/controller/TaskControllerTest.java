package com.lab.taskmanager.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.taskmanager.common.exception.GlobalExceptionHandler;
import com.lab.taskmanager.task.dto.TaskResponse;
import com.lab.taskmanager.task.entity.TaskPriority;
import com.lab.taskmanager.task.entity.TaskStatus;
import com.lab.taskmanager.task.service.TaskService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void listTasksShouldReturnCurrentUsersTasks() throws Exception {
        when(taskService.listTasks("alice", "updatedBy")).thenReturn(List.of(buildResponse(1L, "write report")));

        mockMvc.perform(get("/api/tasks").principal(() -> "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("任务列表获取成功"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("write report"));
    }

    @Test
    void createTaskShouldReturnCreated() throws Exception {
        when(taskService.createTask(eq("alice"), any())).thenReturn(buildResponse(2L, "finish lab"));

        String requestBody = objectMapper.writeValueAsString(
                new TaskCreateRequestBody("finish lab", "backend part", "TODO", "HIGH"));

        mockMvc.perform(post("/api/tasks")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("任务创建成功"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void createTaskShouldReturnBadRequestWhenTitleMissing() throws Exception {
        String requestBody = """
                {
                  "title": "",
                  "description": "backend part"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .principal(() -> "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteTaskShouldReturnSuccess() throws Exception {
        doNothing().when(taskService).deleteTask("alice", 3L);

        mockMvc.perform(delete("/api/tasks/3").principal(() -> "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("任务删除成功"));
    }

    private TaskResponse buildResponse(Long id, String title) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 15, 10);
        return new TaskResponse(
                id,
                title,
                "sample description",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                now.plusDays(1),
                now.minusDays(1),
                now);
    }

    private record TaskCreateRequestBody(
            String title,
            String description,
            String status,
            String priority) {
    }
}
