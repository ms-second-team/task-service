package ru.mssecondteam.taskservice.service.task;

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
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.dto.event.EventDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberRole;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;
import ru.mssecondteam.taskservice.service.TaskService;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "event-service.url=localhost:${wiremock.server.port}"
})
class TaskServiceImplIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

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

    @Autowired
    private TaskService taskService;

    private ObjectMapper objectMapper;

    private Task task;

    private Long userId;

    @BeforeEach
    void setUp() {
        task = createNewTask(1);
        userId = 4L;
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @SneakyThrows
    @Test
    @DisplayName("Create task")
    void createTask_shouldReturnTaskWithPositiveId() {
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

        assertThat(createdTask, notNullValue());
        assertThat(createdTask.getId(), greaterThan(0L));
        assertThat(createdTask.getAuthorId(), is(userId));
        assertThat(createdTask.getCreatedAt(), notNullValue());
        assertThat(createdTask.getCreatedAt(), lessThanOrEqualTo(LocalDateTime.now()));
    }

    @SneakyThrows
    @Test
    @DisplayName("Create task, assignee is not a team member")
    void createTask_whenAssigneeIsNotATeamMember_shouldThrowNotAuthorizedException() {
        EventDto event = createEvent(44);
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

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.createTask(userId, task));

        assertThat(ex.getMessage(), is(String.format("User is with id '%s' not a team member for event with id '%s'",
                task.getAssigneeId(), task.getEventId())));
    }

    @SneakyThrows
    @Test
    @DisplayName("Create task, user is not a team member or author")
    void createTask_whenUserIsNotATeamMember_shouldThrowNotAuthorizedException() {
        EventDto event = createEvent(44);
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(task.getAssigneeId())
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

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.createTask(userId, task));

        assertThat(ex.getMessage(), is(String.format("User is with id '%s' not a team member for event with id '%s'",
                userId, task.getEventId())));
    }

    @SneakyThrows
    @Test
    @DisplayName("Create task, event not found")
    void createTask_whenEventNotFound_shouldThrowNotAuthorizedException() {
        EventDto event = createEvent(44);
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(task.getAssigneeId())
                .role(TeamMemberRole.MANAGER)
                .build();
        stubFor(get(urlEqualTo("/events/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withStatus(404)));

        stubFor(get(urlEqualTo("/events/teams/" + task.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.createTask(userId, task));

        assertThat(ex.getMessage(), is("Event was not found"));
    }

    @Test
    @DisplayName("Update task")
    @SneakyThrows
    void updateTask_whenUserIsAuthorized_shouldUpdateTask() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2040, 11, 11, 11, 11, 11))
                .eventId(54L)
                .status(TaskStatus.DONE)
                .build();
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

        stubFor(get(urlEqualTo("/events/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));


        Task updatedTask = taskService.updateTask(createdTask.getId(), userId, updateRequest);

        assertThat(updatedTask, notNullValue());
        assertThat(updatedTask.getId(), is(createdTask.getId()));
        assertThat(updatedTask.getAuthorId(), is(userId));
        assertThat(updatedTask.getAssigneeId(), is(createdTask.getAssigneeId()));
        assertThat(updatedTask.getTitle(), is(updateRequest.title()));
        assertThat(updatedTask.getDescription(), is(updatedTask.getDescription()));
        assertThat(updatedTask.getDeadline(), is(updateRequest.deadline()));
        assertThat(updatedTask.getStatus(), is(updateRequest.status()));
        assertThat(updatedTask.getEventId(), is(updateRequest.eventId()));
        assertThat(updatedTask.getCreatedAt(), is(createdTask.getCreatedAt()));
    }

    @Test
    @DisplayName("Update task, user is not a team member of author")
    @SneakyThrows
    void updateTask_whenUserInNotATeamMemberOfAuthor_shouldUpdateTask() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2040, 11, 11, 11, 11, 11))
                .eventId(54L)
                .status(TaskStatus.DONE)
                .build();
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

        teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(task.getAssigneeId())
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.updateTask(createdTask.getId(), userId, updateRequest));

        assertThat(ex.getMessage(), is(String.format("User is with id '%s' not a team member for event with id '%s'",
                userId, updateRequest.eventId())));
    }

    @Test
    @DisplayName("Update task's title")
    @SneakyThrows
    void updateTask_whenOnlyTitleIsUpdated_shouldUpdateOnlyTaskTitle() {
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

        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .eventId(21L)
                .build();

        stubFor(get(urlEqualTo("/events/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));

        Task updatedTask = taskService.updateTask(createdTask.getId(), userId, updateRequest);

        assertThat(updatedTask, notNullValue());
        assertThat(updatedTask.getId(), is(createdTask.getId()));
        assertThat(updatedTask.getAuthorId(), is(userId));
        assertThat(updatedTask.getAssigneeId(), is(createdTask.getAssigneeId()));
        assertThat(updatedTask.getTitle(), is(updateRequest.title()));
        assertThat(updatedTask.getDescription(), is(createdTask.getDescription()));
        assertThat(updatedTask.getDeadline(), is(createdTask.getDeadline()));
        assertThat(updatedTask.getStatus(), is(createdTask.getStatus()));
        assertThat(updatedTask.getEventId(), is(createdTask.getEventId()));
        assertThat(updatedTask.getCreatedAt(), is(createdTask.getCreatedAt()));
    }

    @Test
    @DisplayName("Update task by an unauthorized user")
    @SneakyThrows
    void updateTask_whenUserIsNotAuthorized_shouldThrowNotAuthorizedException() {
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

        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .eventId(21L)
                .build();

        stubFor(get(urlEqualTo("/events/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + updateRequest.eventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto)))
                        .withStatus(200)));
        Long unAuthorizedId = 999L;

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.updateTask(createdTask.getId(), unAuthorizedId, updateRequest));

        assertThat(ex.getMessage(), is("User with id '" + unAuthorizedId + "' is not authorized to modify task with id '" +
                task.getId() + "'"));
    }

    @Test
    @DisplayName("Update task with unknown id")
    void updateTask_whenTaskNotFound_shouldThrowNotFoundException() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .build();
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.updateTask(unknownId, userId, updateRequest));

        assertThat(ex.getMessage(), is("Task with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Find task by id")
    @SneakyThrows
    void findTaskById_whenTasksExists_shouldReturnTasks() {
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

        Task foundTask = taskService.findTaskById(createdTask.getId());

        assertThat(foundTask, notNullValue());
        assertThat(foundTask.getId(), is(createdTask.getId()));
    }

    @Test
    @DisplayName("Find task by unknown id")
    void findTaskById_whenTaskNotFound_shouldThrowNotFoundException() {
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.findTaskById(unknownId));

        assertThat(ex.getMessage(), is("Task with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Search tasks by event id")
    @SneakyThrows
    void searchTasks_whenTwoTasksWithDesiredEventId_shouldReturnTwoTasks() {
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

        taskService.createTask(userId, task);

        Task task2 = createNewTask(2);

        EventDto event2 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        taskService.createTask(userId, task2);

        TaskSearchFilter filter = TaskSearchFilter.builder()
                .eventId(task.getEventId())
                .build();

        List<Task> tasks = taskService.searchTasks(0, 10, filter);

        assertThat(tasks, notNullValue());
        assertThat(tasks.size(), is(2));
        assertThat(tasks.get(0).getId(), is(task.getId()));
        assertThat(tasks.get(1).getId(), is(task2.getId()));
    }

    @Test
    @DisplayName("Search tasks by event id, second page")
    @SneakyThrows
    void searchTasks_whenTwoTasksWithDesiredEventIdButSecondPage_shouldReturnEmptyList() {
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

        taskService.createTask(userId, task);

        Task task2 = createNewTask(2);

        EventDto event2 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        taskService.createTask(userId, task2);

        TaskSearchFilter filter = TaskSearchFilter.builder()
                .eventId(task.getEventId())
                .build();

        List<Task> tasks = taskService.searchTasks(1, 10, filter);

        assertThat(tasks, notNullValue());
        assertThat(tasks, emptyIterable());
    }

    @Test
    @DisplayName("Search tasks by event id")
    @SneakyThrows
    void searchTasks_whenTwoTasksWithDesiredEventIdOnePerPage_shouldReturnOneTask() {
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

        taskService.createTask(userId, task);

        Task task2 = createNewTask(2);

        EventDto event2 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        taskService.createTask(userId, task2);

        TaskSearchFilter filter = TaskSearchFilter.builder()
                .eventId(task.getEventId())
                .build();

        List<Task> tasks = taskService.searchTasks(0, 1, filter);

        assertThat(tasks, notNullValue());
        assertThat(tasks.size(), is(1));
        assertThat(tasks.get(0).getId(), is(task.getId()));
    }

    @Test
    @DisplayName("Search tasks by event id and assigneeId")
    @SneakyThrows
    void searchTasks_whenSearchByEventIdAndAssigneeId_shouldReturnOneTask() {
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

        taskService.createTask(userId, task);

        Task task2 = createNewTask(2);
        task2.setAssigneeId(45L);

        EventDto event2 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        taskService.createTask(userId, task2);

        TaskSearchFilter filter = TaskSearchFilter.builder()
                .eventId(task.getEventId())
                .assigneeId(task.getAssigneeId())
                .build();

        List<Task> tasks = taskService.searchTasks(0, 10, filter);

        assertThat(tasks, notNullValue());
        assertThat(tasks.size(), is(1));
        assertThat(tasks.get(0).getId(), is(task.getId()));
    }

    @Test
    @DisplayName("Search tasks by author id")
    @SneakyThrows
    void searchTasks_whenSearchByAuthorId_shouldReturnOneTask() {
        EventDto event = createEvent(task.getAssigneeId());
        TeamMemberDto teamMemberDto = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(userId + 1)
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

        taskService.createTask(userId + 1, task);

        Task task2 = createNewTask(2);

        EventDto event2 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));


        taskService.createTask(userId, task2);

        TaskSearchFilter filter = TaskSearchFilter.builder()
                .authorId(userId)
                .build();

        List<Task> tasks = taskService.searchTasks(0, 10, filter);

        assertThat(tasks, notNullValue());
        assertThat(tasks.size(), is(1));
        assertThat(tasks.get(0).getId(), is(task2.getId()));
    }

    @Test
    @DisplayName("Search tasks by unknown author id")
    @SneakyThrows
    void searchTasks_whenSearchByUnknownAuthorId_shouldReturnEmptyList() {
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

        taskService.createTask(userId, task);

        Task task2 = createNewTask(2);

        EventDto event2 = createEvent(task2.getAssigneeId());
        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event2.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();

        stubFor(get(urlEqualTo("/events/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(event2))
                        .withStatus(200)));

        stubFor(get(urlEqualTo("/events/teams/" + task2.getEventId()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto2)))
                        .withStatus(200)));

        taskService.createTask(userId, task2);

        Long unknownId = 999L;
        TaskSearchFilter filter = TaskSearchFilter.builder()
                .authorId(unknownId)
                .build();

        List<Task> tasks = taskService.searchTasks(0, 10, filter);

        assertThat(tasks, notNullValue());
        assertThat(tasks, emptyIterable());
    }

    @Test
    @DisplayName("Delete task by author")
    @SneakyThrows
    void deleteTaskById_whenTaskExists_ShouldDeleteTask() {
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

        taskService.createTask(userId, task);

        Task createdTask = taskService.createTask(userId, task);

        taskService.deleteTaskById(createdTask.getId(), userId);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.findTaskById(createdTask.getId()));

        assertThat(ex.getMessage(), is("Task with id '" + createdTask.getId() + "' was not found"));
    }

    @Test
    @DisplayName("Delete task by other user")
    @SneakyThrows
    void deleteTaskById_whenTaskExistsButOtherUserTryToDelete_ShouldThrowNotAuthorizedException() {
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

        taskService.createTask(userId, task);

        Task createdTask = taskService.createTask(userId, task);
        Long otherUserId = 99L;

        TeamMemberDto teamMemberDto2 = TeamMemberDto.builder()
                .eventId(event.id())
                .userId(otherUserId)
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
                        .withBody(objectMapper.writeValueAsString(List.of(teamMemberDto, teamMemberDto2)))
                        .withStatus(200)));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.deleteTaskById(createdTask.getId(), otherUserId));

        assertThat(ex.getMessage(), is("User with id '" + otherUserId + "' is not authorized to delete task with id '" +
                task.getId() + "'"));
    }

    @Test
    @DisplayName("Delete task, task not found")
    void deleteTaskById_whenTaskNotExists_ShouldThrowNotFoundException() {
        Long unknownTaskId = 99L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.deleteTaskById(unknownTaskId, userId));

        assertThat(ex.getMessage(), is("Task with id '" + unknownTaskId + "' was not found"));
    }

    private Task createNewTask(int id) {
        return Task.builder()
                .title("task " + id)
                .description("task description" + id)
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