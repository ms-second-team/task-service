package ru.mssecondteam.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import ru.mssecondteam.taskservice.dto.NewTaskRequest;
import ru.mssecondteam.taskservice.dto.TaskDto;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;
import ru.mssecondteam.taskservice.service.TaskService;

import java.time.LocalDateTime;
import java.util.Collections;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private TaskMapper taskMapper;

    private TaskDto taskDto;

    private Task task;

    private NewTaskRequest newTask;

    private TaskUpdateRequest updateRequest;

    private Long userId;

    private Long taskId;

    @Value("${spring.jackson.date-format}")
    private String dateTimeFormat;


    @BeforeEach
    void setup() {
        taskDto = TaskDto.builder()
                .id(1L)
                .title("taskDto")
                .description("taskDto description")
                .deadline(LocalDateTime.of(2025, 10, 10, 12, 34, 33))
                .status(TaskStatus.DONE)
                .assigneeId(2L)
                .eventId(5L)
                .authorId(4L)
                .build();
        newTask = NewTaskRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(TaskStatus.TODO)
                .assigneeId(3L)
                .eventId(4L)
                .build();
        updateRequest = TaskUpdateRequest.builder()
                .title("updated title")
                .description("updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        userId = 1L;
        taskId = 4L;
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task")
    void createTask_whenAllFieldsValid_shouldReturn201Status() {
        when(taskMapper.toModel(newTask))
                .thenReturn(task);
        when(taskService.createTask(userId, task))
                .thenReturn(task);
        when(taskMapper.toDto(task))
                .thenReturn(taskDto);

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(taskDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(taskDto.title())))
                .andExpect(jsonPath("$.description", is(taskDto.description())))
                .andExpect(jsonPath("$.deadline", is(taskDto.deadline()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.status", is(taskDto.status().name())))
                .andExpect(jsonPath("$.assigneeId", is(taskDto.assigneeId()), Long.class))
                .andExpect(jsonPath("$.eventId", is(taskDto.eventId()), Long.class))
                .andExpect(jsonPath("$.authorId", is(taskDto.authorId()), Long.class));

        verify(taskMapper, times(1)).toModel(newTask);
        verify(taskService, times(1)).createTask(userId, task);
        verify(taskMapper, times(1)).toDto(task);
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task, request without header")
    void createTask_whenUserIdHeaderIsMissing_shouldReturn400Status() {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MissingRequestHeaderException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task with blank title")
    void createTask_whenTitleIsBlank_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title("")
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(TaskStatus.TODO)
                .assigneeId(3L)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be blank")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task with null title")
    void createTask_whenTitleIsNull_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title(null)
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(TaskStatus.TODO)
                .assigneeId(3L)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be blank")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task with null status")
    void createTask_whenStatusIsNull_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(null)
                .assigneeId(3L)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Task must have status")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task with negative assignee id")
    void createTask_whenAssigneeIdIsNegative_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(TaskStatus.DONE)
                .assigneeId(-4L)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Assignee Id must be positive")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task without event id")
    void createTask_whenEventIdIsNull_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(TaskStatus.DONE)
                .eventId(null)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Event Id must be positive")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task with negative event id")
    void createTask_whenEventIdIsNegative_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2025, 12, 10, 12, 34, 33))
                .status(TaskStatus.DONE)
                .eventId(-3L)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Event Id must be positive")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create task with past deadline")
    void createTask_whenDeadlineInPast_shouldReturn400Status() {
        newTask = NewTaskRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2023, 12, 10, 12, 34, 33))
                .status(TaskStatus.DONE)
                .eventId(2L)
                .build();

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Deadline must be in future")));

        verify(taskMapper, never()).toModel(any());
        verify(taskService, never()).createTask(any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task")
    void updateTask_whenAllFieldsValid_shouldReturn200Status() {
        when(taskService.updateTask(taskId, userId, updateRequest))
                .thenReturn(task);
        when(taskMapper.toDto(task))
                .thenReturn(taskDto);

        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(taskDto.title())))
                .andExpect(jsonPath("$.description", is(taskDto.description())))
                .andExpect(jsonPath("$.deadline", is(taskDto.deadline()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.status", is(taskDto.status().name())))
                .andExpect(jsonPath("$.assigneeId", is(taskDto.assigneeId()), Long.class))
                .andExpect(jsonPath("$.eventId", is(taskDto.eventId()), Long.class))
                .andExpect(jsonPath("$.authorId", is(taskDto.authorId()), Long.class));

        verify(taskService, times(1)).updateTask(taskId, userId, updateRequest);
        verify(taskMapper, times(1)).toDto(task);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task without title")
    void updateTask_whenWithoutTitle_shouldReturn200Status() {
        when(taskService.updateTask(taskId, userId, updateRequest))
                .thenReturn(task);
        when(taskMapper.toDto(task))
                .thenReturn(taskDto);

        updateRequest = TaskUpdateRequest.builder()
                .status(TaskStatus.CANCELLED)
                .description("desc")
                .build();

        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(taskDto.title())))
                .andExpect(jsonPath("$.description", is(taskDto.description())))
                .andExpect(jsonPath("$.deadline", is(taskDto.deadline()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.status", is(taskDto.status().name())))
                .andExpect(jsonPath("$.assigneeId", is(taskDto.assigneeId()), Long.class))
                .andExpect(jsonPath("$.eventId", is(taskDto.eventId()), Long.class))
                .andExpect(jsonPath("$.authorId", is(taskDto.authorId()), Long.class));

        verify(taskService, times(1)).updateTask(taskId, userId, updateRequest);
        verify(taskMapper, times(1)).toDto(task);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task, empty title")
    void updateTask_whenEmptyTitle_shouldReturn200Status() {
        when(taskService.updateTask(taskId, userId, updateRequest))
                .thenReturn(task);
        when(taskMapper.toDto(task))
                .thenReturn(taskDto);

        updateRequest = TaskUpdateRequest.builder()
                .title("  ")
                .status(TaskStatus.CANCELLED)
                .description("desc")
                .build();

        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be empty")));

        verify(taskService, never()).updateTask(any(), any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task, request without header")
    void updateTask_whenUserIdHeaderIsMissing_shouldReturn400Status() {
        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MissingRequestHeaderException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

        verify(taskService, never()).updateTask(any(), any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task, task not found")
    void updateTask_whenTaskNotFound_shouldReturn404Status() {
        when(taskService.updateTask(taskId, userId, updateRequest))
                .thenThrow(new NotFoundException("Task was not found"));

        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotFoundException))
                .andExpect(jsonPath("$.errors", hasValue("Task was not found")));

        verify(taskService, times(1)).updateTask(taskId, userId, updateRequest);
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task, user not authorized to modify task")
    void updateTask_whenUserNotAuthorizedToModifyTask_shouldReturn403Status() {
        when(taskService.updateTask(taskId, userId, updateRequest))
                .thenThrow(new NotAuthorizedException("Not authorized"));

        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotAuthorizedException))
                .andExpect(jsonPath("$.errors", hasValue("Not authorized")));

        verify(taskService, times(1)).updateTask(taskId, userId, updateRequest);
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update task with past deadline")
    void updateTask_whenDeadlineInPast_shouldReturn400Status() {
        updateRequest = TaskUpdateRequest.builder()
                .title("updated title")
                .description("updated description")
                .status(TaskStatus.IN_PROGRESS)
                .deadline(LocalDateTime.of(2023, 12, 10, 12, 34, 33))
                .build();

        mvc.perform(patch("/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Deadline must be in future")));

        verify(taskService, never()).updateTask(any(), any(), any());
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Find task by id")
    void findTaskById_whenTaskExists_shouldReturnTask() {
        when(taskService.findTaskById(taskId))
                .thenReturn(task);
        when(taskMapper.toDto(task))
                .thenReturn(taskDto);

        mvc.perform(get("/tasks/{taskId}", taskId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(taskDto.title())))
                .andExpect(jsonPath("$.description", is(taskDto.description())))
                .andExpect(jsonPath("$.deadline", is(taskDto.deadline()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.status", is(taskDto.status().name())))
                .andExpect(jsonPath("$.assigneeId", is(taskDto.assigneeId()), Long.class))
                .andExpect(jsonPath("$.eventId", is(taskDto.eventId()), Long.class))
                .andExpect(jsonPath("$.authorId", is(taskDto.authorId()), Long.class));

        verify(taskService, times(1)).findTaskById(taskId);
        verify(taskMapper, times(1)).toDto(task);
    }

    @Test
    @SneakyThrows
    @DisplayName("Find task by id, task not found")
    void findTaskById_whenTaskNotFound_shouldReturn404Status() {
        when(taskService.findTaskById(taskId))
                .thenThrow(new NotFoundException("Task was not found"));

        mvc.perform(get("/tasks/{taskId}", taskId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotFoundException));

        verify(taskService, times(1)).findTaskById(taskId);
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Search tasks")
    void searchTasks_shouldReturnListOfTasks() {
        Integer page = 1;
        Integer size = 12;

        TaskSearchFilter filter = TaskSearchFilter.builder().build();
        when(taskService.searchTasks(page, size, filter))
                .thenReturn(Collections.singletonList(task));
        when(taskMapper.toDtoList(Collections.singletonList(task)))
                .thenReturn(Collections.singletonList(taskDto));

        mvc.perform(get("/tasks")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$.[0].id", is(taskDto.id()), Long.class))
                .andExpect(jsonPath("$.[0].title", is(taskDto.title())))
                .andExpect(jsonPath("$.[0].description", is(taskDto.description())))
                .andExpect(jsonPath("$.[0].deadline", is(taskDto.deadline()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.[0].status", is(taskDto.status().name())))
                .andExpect(jsonPath("$.[0].assigneeId", is(taskDto.assigneeId()), Long.class))
                .andExpect(jsonPath("$.[0].eventId", is(taskDto.eventId()), Long.class))
                .andExpect(jsonPath("$.[0].authorId", is(taskDto.authorId()), Long.class));

        verify(taskService, times(1)).searchTasks(page, size, filter);
        verify(taskMapper, times(1)).toDtoList(Collections.singletonList(task));
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task")
    void deleteTaskById_whenTaskExists_shouldReturn204() {
        mvc.perform(delete("/tasks/{taskId}", taskId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTaskById(taskId, userId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task, task not found")
    void deleteTaskById_whenTaskNotFound_shouldReturn404() {
        doThrow(new NotFoundException("Task not found"))
                .when(taskService).deleteTaskById(taskId, userId);

        mvc.perform(delete("/tasks/{taskId}", taskId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotFoundException));

        verify(taskService, times(1)).deleteTaskById(taskId, userId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task, user not authorized to delete task")
    void deleteTaskById_whenUserNotAuthorizedToDeleteTask_shouldReturn403() {
        doThrow(new NotAuthorizedException("Not authorized"))
                .when(taskService).deleteTaskById(taskId, userId);

        mvc.perform(delete("/tasks/{taskId}", taskId)
                        .header("X-User-Id", userId))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotAuthorizedException));

        verify(taskService, times(1)).deleteTaskById(taskId, userId);
    }
}