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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.mssecondteam.taskservice.dto.TaskDto;
import ru.mssecondteam.taskservice.dto.TaskFullDto;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicFullDto;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicShortDto;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.dto.NewEpicRequest;
import ru.mssecondteam.taskservice.mapper.EpicMapper;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.service.TaskService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EpicController.class)
public class EpicControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private EpicMapper epicMapper;

    @MockBean
    private TaskMapper taskMapper;

    @Value("${spring.jackson.date-format}")
    private String dateTimeFormat;

    private Epic epic;
    private Task task;

    @BeforeEach
    void setup() {
        epic = Epic.builder()
                .id(1L)
                .title("epic")
                .eventId(1L)
                .executiveId(1L)
                .build();
    }

    @Test
    @SneakyThrows
    @DisplayName("New Epic creation test. Successful")
    void createEpicOk() {
        NewEpicRequest epicRequest = createEpicRequest("epic", LocalDateTime.now().plusYears(1), 2L, 3L);
        EpicShortDto epicShortDto = createShortDto(epicRequest.title(), epicRequest.eventId(),
                epicRequest.executiveId(), epicRequest.deadline());

        when(epicMapper.toEpicModel(any()))
                .thenReturn(epic);
        when(taskService.createEpic(any()))
                .thenReturn(epic);
        when(epicMapper.toEpicShortDto(any()))
                .thenReturn(epicShortDto);

        mvc.perform(post("/epics")
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(epicShortDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicShortDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicShortDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", is(epicShortDto.deadline().format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.executiveId", is(epicShortDto.executiveId()), Long.class));

        verify(taskService, times(1)).createEpic(any());
        verify(epicMapper, times(1)).toEpicModel(any());
        verify(epicMapper, times(1)).toEpicShortDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create epic failed, title null")
    void createEpicTitleNull() {
        NewEpicRequest epicRequest = createEpicRequest(null, LocalDateTime.now().plusYears(1), 2L, 3L);
        mvc.perform(post("/epics")
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicModel(any());
        verify(taskService, never()).createEpic(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create epic failed, title blank")
    void createEpicTitleBlank() {
        NewEpicRequest epicRequest = createEpicRequest("     ", LocalDateTime.now().plusYears(1), 2L, 3L);
        mvc.perform(post("/epics")
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicModel(any());
        verify(taskService, never()).createEpic(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create epic failed, executive id negative")
    void createEpicExecutiveIdNegative() {
        NewEpicRequest epicRequest = createEpicRequest("title", LocalDateTime.now().plusYears(1), 2L, 0L);
        mvc.perform(post("/epics")
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicModel(any());
        verify(taskService, never()).createEpic(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create epic failed, event id negative")
    void createEpicEventIdNegative() {
        NewEpicRequest epicRequest = createEpicRequest("title", LocalDateTime.now().plusYears(1), 0L, 3L);
        mvc.perform(post("/epics")
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicModel(any());
        verify(taskService, never()).createEpic(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create epic failed, deadline in the past")
    void createEpicDeadlineInPast() {
        NewEpicRequest epicRequest = createEpicRequest("title", LocalDateTime.now().minusYears(1), 2L, 3L);
        mvc.perform(post("/epics")
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicModel(any());
        verify(taskService, never()).createEpic(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic only title, successful")
    void updateEpicTitleShouldReturnStatus200() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("new title")
                .build();
        EpicShortDto epicShortDto = createShortDto(updateRequest.title(), 2L,
                3L, LocalDateTime.now().plusYears(1));

        when(taskService.updateEpic(anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicShortDto(any()))
                .thenReturn(epicShortDto);

        mvc.perform(patch("/epics/1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicShortDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(updateRequest.title())))
                .andExpect(jsonPath("$.eventId", is(epicShortDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", is(epicShortDto.deadline().format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.executiveId", is(epicShortDto.executiveId()), Long.class));

        verify(taskService, times(1)).updateEpic(anyLong(), any());
        verify(epicMapper, times(1)).toEpicShortDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic only executive, successful")
    void updateEpicExecutiveShouldReturnStatus200() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .executiveId(10L)
                .build();
        EpicShortDto epicShortDto = createShortDto(updateRequest.title(), 2L,
                updateRequest.executiveId(), LocalDateTime.now().plusYears(1));

        when(taskService.updateEpic(anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicShortDto(any()))
                .thenReturn(epicShortDto);

        mvc.perform(patch("/epics/1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicShortDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicShortDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicShortDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", is(epicShortDto.deadline().format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.executiveId", is(updateRequest.executiveId()), Long.class));

        verify(taskService, times(1)).updateEpic(anyLong(), any());
        verify(epicMapper, times(1)).toEpicShortDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic only deadline, successful")
    void updateEpicDeadlineShouldReturnStatus200() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .deadline(LocalDateTime.now().plusYears(2))
                .build();
        EpicShortDto epicShortDto = createShortDto(updateRequest.title(), 2L,
                3L, updateRequest.deadline());

        when(taskService.updateEpic(anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicShortDto(any()))
                .thenReturn(epicShortDto);

        mvc.perform(patch("/epics/1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicShortDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicShortDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicShortDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", is(updateRequest.deadline().format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.executiveId", is(epicShortDto.executiveId()), Long.class));

        verify(taskService, times(1)).updateEpic(anyLong(), any());
        verify(epicMapper, times(1)).toEpicShortDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic failed, epic id is negative")
    void updateEpicFailEpicIdNegativeShouldReturnStatus400() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .executiveId(1L)
                .build();
        mvc.perform(patch("/epics/-1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicShortDto(any());
        verify(taskService, never()).updateEpic(anyLong(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic failed, title is blank")
    void updateEpicFailTitleBlankShouldReturnStatus400() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("          ")
                .build();
        mvc.perform(patch("/epics/1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicShortDto(any());
        verify(taskService, never()).updateEpic(anyLong(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic failed, executive id is negative")
    void updateEpicFailExecutorIdNegativeShouldReturnStatus400() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .executiveId(0L)
                .build();
        mvc.perform(patch("/epics/1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicShortDto(any());
        verify(taskService, never()).updateEpic(anyLong(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic failed, deadline in past")
    void updateEpicFailDeadlineInPastShouldReturnStatus400() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .deadline(LocalDateTime.now().minusYears(2))
                .build();
        mvc.perform(patch("/epics/1")
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicShortDto(any());
        verify(taskService, never()).updateEpic(anyLong(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic. Success")
    void addTaskToEpicShouldReturnStatus200() {
        TaskFullDto taskFullDto = TaskFullDto.builder()
                .epicId(epic.getId())
                .build();
        when(taskService.addTaskToEpic(anyLong(), anyLong(), anyLong()))
                .thenReturn(task);
        when(taskMapper.toTaskFullDto(any()))
                .thenReturn(taskFullDto);

        mvc.perform(patch("/epics/1/tasks/4")
                        .header("X-User-Id", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epicId", is(epic.getId()), Long.class));

        verify(taskMapper, times(1)).toTaskFullDto(any());
        verify(taskService, times(1)).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic when epic id negative")
    void addTaskToEpicFailNegativeEpicIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/-1/tasks/4")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toTaskFullDto(any());
        verify(taskService, never()).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic when task id negative")
    void addTaskToEpicFailNegativeTaskIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/1/tasks/-4")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toTaskFullDto(any());
        verify(taskService, never()).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic when user id negative")
    void addTaskToEpicFailNegativeUserIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/1/tasks/4")
                        .header("X-User-Id", -2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toTaskFullDto(any());
        verify(taskService, never()).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic. Success")
    void deleteTaskFromEpicShouldReturnStatus200() {
        TaskFullDto taskFullDto = TaskFullDto.builder()
                .epicId(null)
                .build();

        when(taskService.deleteTaskFromEpic(anyLong(), anyLong(), anyLong()))
                .thenReturn(task);
        when(taskMapper.toTaskFullDto(any()))
                .thenReturn(taskFullDto);

        mvc.perform(patch("/epics/1/tasks/4/delete")
                        .header("X-User-Id", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.epicId", is(nullValue())));

        verify(taskMapper, times(1)).toTaskFullDto(any());
        verify(taskService, times(1)).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic when epic id negative")
    void deleteTaskFromEpicFailNegativeEpicIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/-1/tasks/4/delete")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toTaskFullDto(any());
        verify(taskService, never()).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic when task id negative")
    void deleteTaskFromEpicFailNegativeTaskIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/1/tasks/-4/delete")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toTaskFullDto(any());
        verify(taskService, never()).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic when user id negative")
    void deleteTaskFromEpicNegativeUserIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/1/tasks/4/delete")
                        .header("X-User-Id", -2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toTaskFullDto(any());
        verify(taskService, never()).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Get epic by id with tasks. Success")
    void findEpicByIdWithTasks() {
        EpicFullDto fullDto = createEpicFullDto(
                LocalDateTime.now().plusYears(1), new ArrayList<>());

        when(taskService.updateEpic(anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicFullDto(any()))
                .thenReturn(fullDto);

        mvc.perform(get("/epics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(fullDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(fullDto.title())))
                .andExpect(jsonPath("$.eventId", is(fullDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", is(fullDto.deadline().format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.executiveId", is(fullDto.executiveId()), Long.class))
                .andExpect(jsonPath("$.epicsTasks",is(notNullValue())));

        verify(epicMapper, times(1)).toEpicFullDto(any());
        verify(taskService, times(1)).findEpicById(anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Get epic by id when epic id negative")
    void findEpicByIdWithTasksNegativeEpicId() {
        mvc.perform(get("/epics/-1"))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicFullDto(any());
        verify(taskService, never()).findEpicById(anyLong());
    }

    private NewEpicRequest createEpicRequest(String title, LocalDateTime deadline, Long eventId, Long executiveId) {
        return NewEpicRequest.builder()
                .title(title)
                .deadline(deadline)
                .eventId(eventId)
                .executiveId(executiveId)
                .build();
    }

    private EpicShortDto createShortDto(String title, Long eventId, Long executiveId, LocalDateTime deadline) {
        return EpicShortDto.builder()
                .id(1L)
                .title(title)
                .eventId(eventId)
                .executiveId(executiveId)
                .deadline(deadline)
                .build();
    }

    private EpicFullDto createEpicFullDto(LocalDateTime deadline,
                                          List<TaskDto> epicsTasks) {
        return EpicFullDto.builder()
                .id(1L)
                .title("title")
                .eventId(2L)
                .executiveId(3L)
                .deadline(deadline)
                .epicsTasks(epicsTasks).build();
    }
}
