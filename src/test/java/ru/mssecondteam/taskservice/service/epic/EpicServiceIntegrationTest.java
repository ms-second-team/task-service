package ru.mssecondteam.taskservice.service.epic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.mssecondteam.taskservice.dto.epic.EpicUpdateRequest;
import ru.mssecondteam.taskservice.dto.event.EventDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberRole;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.exception.OperationNotAllowedException;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;
import ru.mssecondteam.taskservice.service.EpicService;
import ru.mssecondteam.taskservice.service.TaskService;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "event-service.url=localhost:${wiremock.server.port}"
})
public class EpicServiceIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EpicService epicService;

    @Autowired
    private TaskService taskService;

    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void beforeAll() {
        POSTGRES.start();
    }

    @AfterAll
    static void afterAll() {
        POSTGRES.stop();
    }

    private Task task;

    private Long userId;

    private Epic epic;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        task = createNewTask(1);
        userId = 4L;
        epic = createNewEpic();
    }

    @Test
    @DisplayName("Create Epic, success")
    @SneakyThrows
    void createEpicSuccessfully() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic createdEpic = epicService.createEpic(userId, epic);

        assertThat(createdEpic, notNullValue());
        assertThat(createdEpic.getId(), greaterThan(0L));
        assertThat(createdEpic.getExecutiveId(), is(epic.getExecutiveId()));
        assertThat(createdEpic.getDeadline(), is(epic.getDeadline()));
        assertThat(createdEpic.getEventId(), is(createdEpic.getEventId()));
    }

    @Test
    @DisplayName("Update epic assigneeId. Executive must change")
    @SneakyThrows
    void updateEpicAssigneeIdSuccess() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic epicToUpdate = epicService.createEpic(userId, epic);

        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .executiveId(3L)
                .build();

        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(3L)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epicToUpdate.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epicToUpdate.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto, teamMemberDto2)))
                        .withStatus(200)));

        Epic updatedEpic = epicService.updateEpic(userId, epicToUpdate.getId(), updateRequest);

        assertThat(updatedEpic, notNullValue());
        assertThat(updatedEpic.getId(), is(epicToUpdate.getId()));
        assertThat(updatedEpic.getExecutiveId(), is(updateRequest.executiveId()));
        assertThat(updatedEpic.getTitle(), is(epicToUpdate.getTitle()));
        assertThat(updatedEpic.getDeadline(), is(epicToUpdate.getDeadline()));
        assertThat(updatedEpic.getEventId(), is(epicToUpdate.getEventId()));
    }

    @Test
    @DisplayName("Update epic title. Title must change")
    @SneakyThrows
    void updateEpicTitleSuccess() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic epicToUpdate = epicService.createEpic(userId, epic);

        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("epic 2")
                .build();

        stubFor(get(urlEqualTo("/events/" + epicToUpdate.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epicToUpdate.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));


        Epic updatedEpic = epicService.updateEpic(userId, epicToUpdate.getId(), updateRequest);

        assertThat(updatedEpic, notNullValue());
        assertThat(updatedEpic.getId(), is(epicToUpdate.getId()));
        assertThat(updatedEpic.getExecutiveId(), is(epicToUpdate.getExecutiveId()));
        assertThat(updatedEpic.getTitle(), is(updateRequest.title()));
        assertThat(updatedEpic.getDeadline(), is(epicToUpdate.getDeadline()));
        assertThat(updatedEpic.getEventId(), is(epicToUpdate.getEventId()));
    }

    @Test
    @DisplayName("Update epic deadline. Deadline must change")
    @SneakyThrows
    void updateEpicDeadlineSuccess() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic epicToUpdate = epicService.createEpic(userId, epic);

        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .deadline(LocalDateTime.now().plusMonths(1))
                .build();

        stubFor(get(urlEqualTo("/events/" + epicToUpdate.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epicToUpdate.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic updatedEpic = epicService.updateEpic(userId, epicToUpdate.getId(), updateRequest);

        assertThat(updatedEpic, notNullValue());
        assertThat(updatedEpic.getId(), is(epicToUpdate.getId()));
        assertThat(updatedEpic.getExecutiveId(), is(epicToUpdate.getExecutiveId()));
        assertThat(updatedEpic.getTitle(), is(epicToUpdate.getTitle()));
        assertThat(updatedEpic.getDeadline(), is(updateRequest.deadline()));
        assertThat(updatedEpic.getEventId(), is(epicToUpdate.getEventId()));
    }

    @Test
    @DisplayName("Update epic, when epic not found")
    @SneakyThrows
    void updateEpicWhenNotFoundShouldThrowNotFoundException() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic epicToUpdate = epicService.createEpic(userId, epic);

        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("epic 2").build();

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.updateEpic(userId, epicToUpdate.getId() + 1, updateRequest));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToUpdate.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic. Success")
    @SneakyThrows
    void addTaskToEpicSuccess() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToAdd = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToAdd = epicService.createEpic(userId, epic);

        assertThat(taskToAdd.getEpic(), is(nullValue()));

        epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId());
        Task addedTask = taskService.findTaskById(taskToAdd.getId());

        assertThat(addedTask, notNullValue());
        assertThat(addedTask.getId(), is(taskToAdd.getId()));
        assertThat(addedTask.getAuthorId(), is(taskToAdd.getAuthorId()));
        assertThat(addedTask.getAssigneeId(), is(taskToAdd.getAssigneeId()));
        assertThat(addedTask.getTitle(), is(taskToAdd.getTitle()));
        assertThat(addedTask.getDescription(), is(taskToAdd.getDescription()));
        assertThat(addedTask.getDeadline(), is(taskToAdd.getDeadline()));
        assertThat(addedTask.getStatus(), is(taskToAdd.getStatus()));
        assertThat(addedTask.getEventId(), is(taskToAdd.getEventId()));
        assertThat(addedTask.getCreatedAt(), is(taskToAdd.getCreatedAt()));
        assertThat(addedTask.getEpic().getId(), is(epicToAdd.getId()));
    }

    @Test
    @DisplayName("Add task to epic when task already belongs to epic")
    @SneakyThrows
    void addTaskToEpicWhenTaskAlreadyBelongsToEpicMustThrowNotAuthorizedException() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToAdd = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToAdd = epicService.createEpic(userId, epic);


        epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId());
        Task addedTask = taskService.findTaskById(taskToAdd.getId());

        OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), addedTask.getId()));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' already belongs to epic with id '%s'",
                addedTask.getId(), addedTask.getEpic().getId())));

    }

    @Test
    @DisplayName("Add task to epic when epic not found")
    @SneakyThrows
    void addTaskToEpicWhenEpicNotFoundMustThrowNotFoundException() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic epicToAdd = epicService.createEpic(userId, epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId() + 1, 10L));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToAdd.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic when task not found")
    @SneakyThrows
    void addTaskToEpicWhenTaskNotFoundMustThrowNotFoundException() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToAdd = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToAdd = epicService.createEpic(userId, epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' was not found", taskToAdd.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic when user is not epic executive")
    @SneakyThrows
    void addTaskToEpicWhenUserIsNotAuthorizedMustThrowNotAuthorizedException() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToAdd = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToAdd = epicService.createEpic(userId, epic);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId() + 1, epicToAdd.getId(), taskToAdd.getId()));

        assertThat(ex.getMessage(), is(String.format("User with id '%s' is not authorized to add tasks to " +
                "epic with id '%s'", epicToAdd.getExecutiveId() + 1, epicToAdd.getId())));

    }

    @Test
    @DisplayName("Add task to epic when epic and task have different eventId")
    @SneakyThrows
    void addTaskToEpicWhenEventIdDifferentMustThrowOperationNotAllowedException() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToAdd = taskService.createTask(userId, task);

        epic.setEventId(epic.getEventId() + 1);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToAdd = epicService.createEpic(userId, epic);

        OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId()));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' can not be added to epic " +
                "with id '%s' as they belong to different events", taskToAdd.getId(), epicToAdd.getId())));
    }

    @Test
    @DisplayName("Delete task from epic. Success")
    @SneakyThrows
    void deleteTaskFromEpicTest() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task createdTask = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic createdEpic = epicService.createEpic(userId, epic);

        assertThat(createdTask.getEpic(), is(nullValue()));

        epicService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask.getId());
        Task addedTask = taskService.findTaskById(createdTask.getId());

        assertThat(addedTask.getEpic().getId(), is(createdEpic.getId()));

        epicService.deleteTaskFromEpic(createdEpic.getExecutiveId(), createdEpic.getId(), addedTask.getId());
        Task deletedFromEpicTask = taskService.findTaskById(addedTask.getId());

        assertThat(deletedFromEpicTask, notNullValue());
        assertThat(deletedFromEpicTask.getId(), is(createdTask.getId()));
        assertThat(deletedFromEpicTask.getAuthorId(), is(createdTask.getAuthorId()));
        assertThat(deletedFromEpicTask.getAssigneeId(), is(createdTask.getAssigneeId()));
        assertThat(deletedFromEpicTask.getTitle(), is(createdTask.getTitle()));
        assertThat(deletedFromEpicTask.getDescription(), is(createdTask.getDescription()));
        assertThat(deletedFromEpicTask.getDeadline(), is(createdTask.getDeadline()));
        assertThat(deletedFromEpicTask.getStatus(), is(createdTask.getStatus()));
        assertThat(deletedFromEpicTask.getEventId(), is(createdTask.getEventId()));
        assertThat(deletedFromEpicTask.getCreatedAt(), is(createdTask.getCreatedAt()));
        assertThat(deletedFromEpicTask.getEpic(), is(nullValue()));
    }

    @Test
    @DisplayName("Delete task from epic when epic not found")
    @SneakyThrows
    void deleteTaskFromEpicWhenEpicNotFoundMustThrowNotFoundException() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic epicToDeleteFrom = epicService.createEpic(userId, epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.deleteTaskFromEpic(epicToDeleteFrom.getExecutiveId(),
                        epicToDeleteFrom.getId() + 1, 10L));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToDeleteFrom.getId() + 1)));
    }

    @Test
    @DisplayName("Delete task from epic when task not found")
    @SneakyThrows
    void deleteTaskFromEpicWhenTaskNotFoundMustThrowNotFoundException() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToDelete = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToDeleteFrom = epicService.createEpic(userId, epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.deleteTaskFromEpic(epicToDeleteFrom.getExecutiveId(), epicToDeleteFrom.getId(),
                        taskToDelete.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' was not found", taskToDelete.getId() + 1)));
    }

    @Test
    @DisplayName("Delete task from epic when user is not authorized")
    @SneakyThrows
    void deleteTaskFromEpicWhenUserIsNotAuthorizedMustThrowNotAuthorizedException() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task taskToDelete = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic epicToDeleteFrom = epicService.createEpic(userId, epic);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> epicService.addTaskToEpic(epicToDeleteFrom.getExecutiveId() + 1, epicToDeleteFrom.getId(),
                        taskToDelete.getId()));

        assertThat(ex.getMessage(), is(String.format("User with id '%s' is not authorized to add tasks to " +
                "epic with id '%s'", epicToDeleteFrom.getExecutiveId() + 1, epicToDeleteFrom.getId())));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when tasks are empty")
    @SneakyThrows
    void findEpicByIdWithEmptyTasks() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic createdEpic = epicService.createEpic(userId, epic);

        Epic retrievedEpic = epicService.findEpicById(createdEpic.getId());

        assertThat(retrievedEpic, notNullValue());
        assertThat(retrievedEpic.getId(), is(createdEpic.getId()));
        assertThat(retrievedEpic.getTitle(), is(createdEpic.getTitle()));
        assertThat(retrievedEpic.getEventId(), is(createdEpic.getEventId()));
        assertThat(retrievedEpic.getDeadline(), is(createdEpic.getDeadline()));
        assertThat(retrievedEpic.getEpicsTasks(), is(nullValue()));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when epic has 1 task")
    @SneakyThrows
    void findEpicByIdWithOneTask() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task createdTask = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic createdEpic = epicService.createEpic(userId, epic);

        epicService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask.getId());
        Epic retrievedEpic = epicService.findEpicById(createdEpic.getId());

        assertThat(retrievedEpic, notNullValue());
        assertThat(retrievedEpic.getId(), is(createdEpic.getId()));
        assertThat(retrievedEpic.getTitle(), is(createdEpic.getTitle()));
        assertThat(retrievedEpic.getEventId(), is(createdEpic.getEventId()));
        assertThat(retrievedEpic.getDeadline(), is(createdEpic.getDeadline()));
        assertThat(retrievedEpic.getEpicsTasks(), notNullValue());
        assertThat(retrievedEpic.getEpicsTasks().size(), is(1));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when epic has 2 tasks")
    @SneakyThrows
    void findEpicByIdWithTwoTasks() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task createdTask = taskService.createTask(userId, task);

        EventDto event2 = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        Epic createdEpic = epicService.createEpic(userId, epic);

        Task task2 = createNewTask(2);

        EventDto event3 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto3 = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event3))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto3)))
                        .withStatus(200)));

        Task createdTask2 = taskService.createTask(userId, task2);
        epicService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask.getId());
        epicService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask2.getId());
        Epic retrievedEpic = epicService.findEpicById(createdEpic.getId());

        assertThat(retrievedEpic, notNullValue());
        assertThat(retrievedEpic.getId(), is(createdEpic.getId()));
        assertThat(retrievedEpic.getTitle(), is(createdEpic.getTitle()));
        assertThat(retrievedEpic.getEventId(), is(createdEpic.getEventId()));
        assertThat(retrievedEpic.getDeadline(), is(createdEpic.getDeadline()));
        assertThat(retrievedEpic.getEpicsTasks(), notNullValue());
        assertThat(retrievedEpic.getEpicsTasks().size(), is(2));
    }

    @Test
    @DisplayName("Find epic when epic not found")
    @SneakyThrows
    void findEpicNotExistMustThrowNotFoundException() {
        EventDto event = createEvent(epic.getExecutiveId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + epic.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Epic createdEpic = epicService.createEpic(userId, epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.findEpicById(createdEpic.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", createdEpic.getId() + 1)));
    }

    private Epic createNewEpic() {
        return Epic.builder()
                .title("epic 1")
                .executiveId(1L)
                .eventId(5L)
                .deadline(LocalDateTime.now().plusYears(1))
                .build();
    }

    private Task createNewTask(int id) {
        return Task.builder()
                .title("task " + id)
                .description("task description " + id)
                .deadline(LocalDateTime.of(2025, 10, 10, 12, 34, 33))
                .status(TaskStatus.TODO)
                .assigneeId(3L)
                .eventId(5L)
                .build();
    }

    private EventDto createEvent(long id) {
        return EventDto.builder()
                .id(1L)
                .name("event name " + id)
                .description("event description " + id)
                .ownerId(id)
                .startDateTime(LocalDateTime.now().plusDays(id))
                .endDateTime(LocalDateTime.now().plusMonths(id))
                .build();
    }
}

