package ru.mssecondteam.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.mssecondteam.taskservice.dto.epic.EpicResponseDto;
import ru.mssecondteam.taskservice.dto.epic.EpicUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.NewEpicRequest;
import ru.mssecondteam.taskservice.mapper.EpicMapper;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.service.impl.EpicServiceImpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
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

@WebMvcTest(controllers = EpicController.class)
public class EpicControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EpicServiceImpl epicService;

    @MockBean
    private EpicMapper epicMapper;

    @MockBean
    private TaskMapper taskMapper;

    private Epic epic;

    private Long userId;

    @BeforeEach
    void setup() {
        epic = Epic.builder()
                .id(1L)
                .title("epic")
                .eventId(1L)
                .executiveId(1L)
                .build();
        userId = 1L;
    }

    @Test
    @SneakyThrows
    @DisplayName("New Epic creation test. Successful")
    void createEpicOk() {
        NewEpicRequest epicRequest = createEpicRequest("epic", LocalDateTime.now().plusYears(1), 2L, 3L);
        EpicResponseDto epicResponseDto = createShortDto(epicRequest.title(), epicRequest.eventId(),
                epicRequest.executiveId(), epicRequest.deadline());

        when(epicMapper.toEpicModel(any()))
                .thenReturn(epic);
        when(epicService.createEpic(any(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(epicResponseDto);

        mvc.perform(post("/epics")
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(epicRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(epicResponseDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicResponseDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicResponseDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", lessThanOrEqualTo(epicResponseDto.deadline().toString())))
                .andExpect(jsonPath("$.executiveId", is(epicResponseDto.executiveId()), Long.class));

        verify(epicService, times(1)).createEpic(any(), any());
        verify(epicMapper, times(1)).toEpicModel(any());
        verify(epicMapper, times(1)).toEpicResponseDto(any());
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
        verify(epicService, never()).createEpic(any(), any());
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
        verify(epicService, never()).createEpic(any(), any());
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
        verify(epicService, never()).createEpic(any(), any());
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
        verify(epicService, never()).createEpic(any(), any());
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
        verify(epicService, never()).createEpic(any(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic only title, successful")
    void updateEpicTitleShouldReturnStatus200() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("new title")
                .build();
        EpicResponseDto epicResponseDto = createShortDto(updateRequest.title(), 2L,
                3L, LocalDateTime.now().plusYears(1));

        when(epicService.updateEpic(anyLong(), anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(epicResponseDto);

        mvc.perform(patch("/epics/1")
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicResponseDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(updateRequest.title())))
                .andExpect(jsonPath("$.eventId", is(epicResponseDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", lessThanOrEqualTo(epicResponseDto.deadline().toString())))
                .andExpect(jsonPath("$.executiveId", is(epicResponseDto.executiveId()), Long.class));

        verify(epicService, times(1)).updateEpic(anyLong(), anyLong(), any());
        verify(epicMapper, times(1)).toEpicResponseDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic only executive, successful")
    void updateEpicExecutiveShouldReturnStatus200() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .executiveId(10L)
                .build();
        EpicResponseDto epicResponseDto = createShortDto(updateRequest.title(), 2L,
                updateRequest.executiveId(), LocalDateTime.now().plusYears(1));

        when(epicService.updateEpic(anyLong(), anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(epicResponseDto);

        mvc.perform(patch("/epics/1")
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicResponseDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicResponseDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicResponseDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", lessThanOrEqualTo(epicResponseDto.deadline().toString())))
                .andExpect(jsonPath("$.executiveId", is(updateRequest.executiveId()), Long.class));

        verify(epicService, times(1)).updateEpic(anyLong(), anyLong(), any());
        verify(epicMapper, times(1)).toEpicResponseDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update epic only deadline, successful")
    void updateEpicDeadlineShouldReturnStatus200() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .deadline(LocalDateTime.now().plusYears(2))
                .build();
        EpicResponseDto epicResponseDto = createShortDto(updateRequest.title(), 2L,
                3L, updateRequest.deadline());

        when(epicService.updateEpic(anyLong(), anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(epicResponseDto);

        mvc.perform(patch("/epics/1")
                        .header("X-User-Id", userId)
                        .content(mapper.writeValueAsString(updateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicResponseDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicResponseDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicResponseDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", lessThanOrEqualTo(updateRequest.deadline().toString())))
                .andExpect(jsonPath("$.executiveId", is(epicResponseDto.executiveId()), Long.class));

        verify(epicService, times(1)).updateEpic(anyLong(), anyLong(), any());
        verify(epicMapper, times(1)).toEpicResponseDto(any());
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

        verify(epicMapper, never()).toEpicResponseDto(any());
        verify(epicService, never()).updateEpic(anyLong(), anyLong(), any());
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

        verify(epicMapper, never()).toEpicResponseDto(any());
        verify(epicService, never()).updateEpic(anyLong(), anyLong(), any());
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

        verify(epicMapper, never()).toEpicResponseDto(any());
        verify(epicService, never()).updateEpic(anyLong(), anyLong(), any());
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

        verify(epicMapper, never()).toEpicResponseDto(any());
        verify(epicService, never()).updateEpic(anyLong(), anyLong(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic. Success")
    void addTaskToEpicShouldReturnStatus200() {
        EpicResponseDto responseDto = EpicResponseDto.builder()
                .id(epic.getId())
                .build();

        when(epicService.addTaskToEpic(anyLong(), anyLong(), anyLong()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(responseDto);

        mvc.perform(patch("/epics/1/tasks/4")
                        .header("X-User-Id", 2))
                .andExpect(status().isOk());

        verify(epicMapper, times(1)).toEpicResponseDto(any());
        verify(epicService, times(1)).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic when epic id negative")
    void addTaskToEpicFailNegativeEpicIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/-1/tasks/4")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toDto(any());
        verify(epicService, never()).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic when task id negative")
    void addTaskToEpicFailNegativeTaskIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/1/tasks/-4")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toDto(any());
        verify(epicService, never()).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Add task to epic when user id negative")
    void addTaskToEpicFailNegativeUserIdShouldReturnStatus400() {
        mvc.perform(patch("/epics/1/tasks/4")
                        .header("X-User-Id", -2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toDto(any());
        verify(epicService, never()).addTaskToEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic. Success")
    void deleteTaskFromEpicShouldReturnStatus200() {
        EpicResponseDto responseDto = EpicResponseDto.builder()
                .id(epic.getId())
                .build();

        when(epicService.deleteTaskFromEpic(anyLong(), anyLong(), anyLong()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(responseDto);

        mvc.perform(delete("/epics/1/tasks/4/delete")
                        .header("X-User-Id", 2))
                .andExpect(status().isOk());

        verify(epicMapper, times(1)).toEpicResponseDto(any());
        verify(epicService, times(1)).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic when epic id negative")
    void deleteTaskFromEpicFailNegativeEpicIdShouldReturnStatus400() {
        mvc.perform(delete("/epics/-1/tasks/4/delete")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toDto(any());
        verify(epicService, never()).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic when task id negative")
    void deleteTaskFromEpicFailNegativeTaskIdShouldReturnStatus400() {
        mvc.perform(delete("/epics/1/tasks/-4/delete")
                        .header("X-User-Id", 2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toDto(any());
        verify(epicService, never()).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Delete task from epic when user id negative")
    void deleteTaskFromEpicNegativeUserIdShouldReturnStatus400() {
        mvc.perform(delete("/epics/1/tasks/4/delete")
                        .header("X-User-Id", -2))
                .andExpect(status().isBadRequest());

        verify(taskMapper, never()).toDto(any());
        verify(epicService, never()).deleteTaskFromEpic(anyLong(), anyLong(), anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Get epic by id with tasks. Success")
    void findEpicByIdWithTasks() {
        EpicResponseDto epicResponseDto = createShortDto("title", epic.getEventId(), 2L, LocalDateTime.now().plusYears(1));

        when(epicService.updateEpic(anyLong(), anyLong(), any()))
                .thenReturn(epic);
        when(epicMapper.toEpicResponseDto(any()))
                .thenReturn(epicResponseDto);

        mvc.perform(get("/epics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(epicResponseDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(epicResponseDto.title())))
                .andExpect(jsonPath("$.eventId", is(epicResponseDto.eventId()), Long.class))
                .andExpect(jsonPath("$.deadline", lessThanOrEqualTo(epicResponseDto.deadline().toString())))
                .andExpect(jsonPath("$.executiveId", is(epicResponseDto.executiveId()), Long.class))
                .andExpect(jsonPath("$.epicsTasks", is(notNullValue())));

        verify(epicMapper, times(1)).toEpicResponseDto(any());
        verify(epicService, times(1)).findEpicById(anyLong());
    }

    @Test
    @SneakyThrows
    @DisplayName("Get epic by id when epic id negative")
    void findEpicByIdWithTasksNegativeEpicId() {
        mvc.perform(get("/epics/-1"))
                .andExpect(status().isBadRequest());

        verify(epicMapper, never()).toEpicResponseDto(any());
        verify(epicService, never()).findEpicById(anyLong());
    }

    private NewEpicRequest createEpicRequest(String title, LocalDateTime deadline, Long eventId, Long executiveId) {
        return NewEpicRequest.builder()
                .title(title)
                .deadline(deadline)
                .eventId(eventId)
                .executiveId(executiveId)
                .build();
    }

    private EpicResponseDto createShortDto(String title, Long eventId, Long executiveId, LocalDateTime deadline) {
        return EpicResponseDto.builder()
                .id(1L)
                .title(title)
                .eventId(eventId)
                .executiveId(executiveId)
                .deadline(deadline)
                .epicsTasks(new ArrayList<>())
                .build();
    }
}
