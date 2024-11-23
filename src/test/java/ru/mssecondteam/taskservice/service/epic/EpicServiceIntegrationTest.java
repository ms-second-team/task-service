package ru.mssecondteam.taskservice.service.epic;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.exception.OperationNotAllowedException;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;
import ru.mssecondteam.taskservice.service.EpicService;
import ru.mssecondteam.taskservice.service.TaskService;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public class EpicServiceIntegrationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EpicService epicService;

    @Autowired
    private TaskService taskService;

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
        task = createNewTask(1);
        userId = 4L;
        epic = createNewEpic();
    }

    @Test
    @DisplayName("Create Epic, success")
    void createEpicSuccessfully() {
        Epic createdEpic = epicService.createEpic(epic);

        assertThat(createdEpic, notNullValue());
        assertThat(createdEpic.getId(), greaterThan(0L));
        assertThat(createdEpic.getExecutiveId(), is(epic.getExecutiveId()));
        assertThat(createdEpic.getDeadline(), is(epic.getDeadline()));
        assertThat(createdEpic.getEventId(), is(createdEpic.getEventId()));
    }

    @Test
    @DisplayName("Update epic assigneeId. Executive must change")
    void updateEpicAssigneeIdSuccess() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .executiveId(3L)
                .build();

        Epic epicToUpdate = epicService.createEpic(epic);
        Epic updatedEpic = epicService.updateEpic(epicToUpdate.getId(), updateRequest);

        assertThat(updatedEpic, notNullValue());
        assertThat(updatedEpic.getId(), is(epicToUpdate.getId()));
        assertThat(updatedEpic.getExecutiveId(), is(updateRequest.executiveId()));
        assertThat(updatedEpic.getTitle(), is(epicToUpdate.getTitle()));
        assertThat(updatedEpic.getDeadline(), is(epicToUpdate.getDeadline()));
        assertThat(updatedEpic.getEventId(), is(epicToUpdate.getEventId()));
    }

    @Test
    @DisplayName("Update epic title. Title must change")
    void updateEpicTitleSuccess() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("epic 2")
                .build();

        Epic epicToUpdate = epicService.createEpic(epic);
        Epic updatedEpic = epicService.updateEpic(epicToUpdate.getId(), updateRequest);

        assertThat(updatedEpic, notNullValue());
        assertThat(updatedEpic.getId(), is(epicToUpdate.getId()));
        assertThat(updatedEpic.getExecutiveId(), is(epicToUpdate.getExecutiveId()));
        assertThat(updatedEpic.getTitle(), is(updateRequest.title()));
        assertThat(updatedEpic.getDeadline(), is(epicToUpdate.getDeadline()));
        assertThat(updatedEpic.getEventId(), is(epicToUpdate.getEventId()));
    }

    @Test
    @DisplayName("Update epic deadline. Deadline must change")
    void updateEpicDeadlineSuccess() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .deadline(LocalDateTime.now().plusMonths(1))
                .build();

        Epic epicToUpdate = epicService.createEpic(epic);
        Epic updatedEpic = epicService.updateEpic(epicToUpdate.getId(), updateRequest);

        assertThat(updatedEpic, notNullValue());
        assertThat(updatedEpic.getId(), is(epicToUpdate.getId()));
        assertThat(updatedEpic.getExecutiveId(), is(epicToUpdate.getExecutiveId()));
        assertThat(updatedEpic.getTitle(), is(epicToUpdate.getTitle()));
        assertThat(updatedEpic.getDeadline(), is(updateRequest.deadline()));
        assertThat(updatedEpic.getEventId(), is(epicToUpdate.getEventId()));
    }

    @Test
    @DisplayName("Update epic, when epic not found")
    void updateEpicWhenNotFoundShouldThrowNotFoundException() {
        EpicUpdateRequest updateRequest = EpicUpdateRequest.builder()
                .title("epic 2").build();
        Epic epicToUpdate = epicService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.updateEpic(epicToUpdate.getId() + 1, updateRequest));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToUpdate.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic. Success")
    void addTaskToEpicSuccess() {
        Task taskToAdd = taskService.createTask(userId, task);
        Epic epicToAdd = epicService.createEpic(epic);

        Assertions.assertNull(taskToAdd.getEpic());

        Task addedTask = epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId());

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
    void addTaskToEpicWhenTaskAlreadyBelongsToEpicMustThrowNotAuthorizedException() {
        Epic epicToAdd = epicService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        Task addedTask = epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId());

        OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), addedTask.getId()));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' already belongs to epic with id '%s'",
                addedTask.getId(), addedTask.getEpic().getId())));

    }

    @Test
    @DisplayName("Add task to epic when epic not found")
    void addTaskToEpicWhenEpicNotFoundMustThrowNotFoundException() {
        Epic epicToAdd = epicService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId() + 1, 10L));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToAdd.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic when task not found")
    void addTaskToEpicWhenTaskNotFoundMustThrowNotFoundException() {
        Epic epicToAdd = epicService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' was not found", taskToAdd.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic when user is not epic executive")
    void addTaskToEpicWhenUserIsNotAuthorizedMustThrowNotAuthorizedException() {
        Epic epicToAdd = epicService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId() + 1, epicToAdd.getId(), taskToAdd.getId()));

        assertThat(ex.getMessage(), is(String.format("User with id '%s' is not authorized to add tasks to " +
                "epic with id '%s'", epicToAdd.getExecutiveId() + 1, epicToAdd.getId())));

    }

    @Test
    @DisplayName("Add task to epic when epic and task have different eventId")
    void addTaskToEpicWhenEventIdDifferentMustThrowOperationNotAllowedException() {
        epic.setEventId(epic.getEventId() + 1);
        Epic epicToAdd = epicService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
                () -> epicService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId()));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' can not be added to epic " +
                "with id '%s' as they belong to different events", taskToAdd.getId(), epicToAdd.getId())));
    }

    @Test
    @DisplayName("Delete task from epic. Success")
    void deleteTaskFromEpicTest() {
        Epic createdEpic = epicService.createEpic(epic);
        Task createdTask = taskService.createTask(userId, task);

        Assertions.assertNull(createdTask.getEpic());

        Task addedTask = epicService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask.getId());

        assertThat(addedTask.getEpic().getId(), is(createdEpic.getId()));

        Task deletedFromEpicTask =
                epicService.deleteTaskFromEpic(createdEpic.getExecutiveId(), createdEpic.getId(), addedTask.getId());

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
        Assertions.assertNull(deletedFromEpicTask.getEpic());
    }

    @Test
    @DisplayName("Delete task from epic when epic not found")
    void deleteTaskFromEpicWhenEpicNotFoundMustThrowNotFoundException() {
        Epic epicToDeleteFrom = epicService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.deleteTaskFromEpic(epicToDeleteFrom.getExecutiveId(),
                        epicToDeleteFrom.getId() + 1, 10L));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToDeleteFrom.getId() + 1)));
    }

    @Test
    @DisplayName("Delete task from epic when task not found")
    void deleteTaskFromEpicWhenTaskNotFoundMustThrowNotFoundException() {
        Epic epicToDeleteFrom = epicService.createEpic(epic);
        Task taskToDelete = taskService.createTask(userId, task);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> epicService.deleteTaskFromEpic(epicToDeleteFrom.getExecutiveId(), epicToDeleteFrom.getId(),
                        taskToDelete.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' was not found", taskToDelete.getId() + 1)));
    }

    @Test
    @DisplayName("Delete task from epic when user is not authorized")
    void deleteTaskFromEpicWhenUserIsNotAuthorizedMustThrowNotAuthorizedException() {
        Epic epicToDeleteFrom = epicService.createEpic(epic);
        Task taskToDelete = taskService.createTask(userId, task);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> epicService.addTaskToEpic(epicToDeleteFrom.getExecutiveId() + 1, epicToDeleteFrom.getId(),
                        taskToDelete.getId()));

        assertThat(ex.getMessage(), is(String.format("User with id '%s' is not authorized to add tasks to " +
                "epic with id '%s'", epicToDeleteFrom.getExecutiveId() + 1, epicToDeleteFrom.getId())));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when tasks are empty")
    void findEpicByIdWithEmptyTasks() {
        Epic createdEpic = epicService.createEpic(epic);

        Epic retrievedEpic = epicService.findEpicById(createdEpic.getId());

        assertThat(retrievedEpic, notNullValue());
        assertThat(retrievedEpic.getId(), is(createdEpic.getId()));
        assertThat(retrievedEpic.getTitle(), is(createdEpic.getTitle()));
        assertThat(retrievedEpic.getEventId(), is(createdEpic.getEventId()));
        assertThat(retrievedEpic.getDeadline(), is(createdEpic.getDeadline()));
        assertThat(retrievedEpic.getEpicsTasks(), notNullValue());
        assertThat(retrievedEpic.getEpicsTasks().size(), is(0));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when epic has 1 task")
    void findEpicByIdWithOneTask() {
        Epic createdEpic = epicService.createEpic(epic);
        Task createdTask = taskService.createTask(userId, task);
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
    void findEpicByIdWithTwoTasks() {
        Epic createdEpic = epicService.createEpic(epic);
        Task createdTask = taskService.createTask(userId, task);
        Task task2 = createNewTask(2);
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
    void findEpicNotExistMustThrowNotFoundException() {
        Epic createdEpic = epicService.createEpic(epic);

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
}
