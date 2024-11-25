package ru.mssecondteam.taskservice.service.task;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;
import ru.mssecondteam.taskservice.service.TaskService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
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

    private Task task;

    private Long userId;

    @BeforeEach
    void setUp() {
        task = createNewTask(1);
        userId = 4L;
    }

    @Test
    @DisplayName("Create task")
    void createTask_shouldReturnTaskWithPositiveId() {
        Task createdTask = taskService.createTask(userId, task);

        assertThat(createdTask, notNullValue());
        assertThat(createdTask.getId(), greaterThan(0L));
        assertThat(createdTask.getAuthorId(), is(userId));
        assertThat(createdTask.getCreatedAt(), notNullValue());
        assertThat(createdTask.getCreatedAt(), lessThanOrEqualTo(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Update task")
    void updateTask_whenUserIsAuthorized_shouldUpdateTask() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .description("new description")
                .deadline(LocalDateTime.of(2040, 11, 11, 11, 11, 11))
                .eventId(54L)
                .status(TaskStatus.DONE)
                .build();

        Task createdTask = taskService.createTask(userId, task);
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
    @DisplayName("Update task's title")
    void updateTask_whenOnlyTitleIsUpdated_shouldUpdateOnlyTaskTitle() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .build();

        Task createdTask = taskService.createTask(userId, task);
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
    void updateTask_whenUserIsNotAuthorized_shouldThrowNotAuthorizedException() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("new title")
                .build();
        Task createdTask = taskService.createTask(userId, task);
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
    void findTaskById_whenTasksExists_shouldReturnTasks() {
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
    void searchTasks_whenTwoTasksWithDesiredEventId_shouldReturnTwoTasks() {
        Task task2 = createNewTask(2);
        taskService.createTask(userId, task);
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
    void searchTasks_whenTwoTasksWithDesiredEventIdButSecondPage_shouldReturnEmptyList() {
        Task task2 = createNewTask(2);
        taskService.createTask(userId, task);
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
    void searchTasks_whenTwoTasksWithDesiredEventIdOnePerPage_shouldReturnOneTask() {
        Task task2 = createNewTask(2);
        taskService.createTask(userId, task);
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
    void searchTasks_whenSearchByEventIdAndAssigneeId_shouldReturnOneTask() {
        Task task2 = createNewTask(2);
        task2.setAssigneeId(45L);
        taskService.createTask(userId, task);
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
    void searchTasks_whenSearchByAuthorId_shouldReturnOneTask() {
        Task task2 = createNewTask(2);
        taskService.createTask(userId + 1, task);
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
    void searchTasks_whenSearchByUnknownAuthorId_shouldReturnEmptyList() {
        Task task2 = createNewTask(2);
        taskService.createTask(userId, task);
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
    void deleteTaskById_whenTaskExists_ShouldDeleteTask() {
        Task createdTask = taskService.createTask(userId, task);

        taskService.deleteTaskById(createdTask.getId(), userId);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.findTaskById(createdTask.getId()));

        assertThat(ex.getMessage(), is("Task with id '" + createdTask.getId() + "' was not found"));
    }

    @Test
    @DisplayName("Delete task by other user")
    void deleteTaskById_whenTaskExistsButOtherUserTryToDelete_ShouldThrowNotAuthorizedException() {
        Task createdTask = taskService.createTask(userId, task);
        Long otherUserId = 99L;

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
}