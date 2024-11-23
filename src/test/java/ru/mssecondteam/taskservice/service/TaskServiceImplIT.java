package ru.mssecondteam.taskservice.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.exception.OperationNotAllowedException;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;

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

    private Epic epic;

    private Long userId;

    @BeforeEach
    void setUp() {
        task = createNewTask(1);
        userId = 4L;

        epic = createNewEpic();
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

    @Test
    @DisplayName("Create Epic, success")
    void createEpicSuccessfully() {
        Epic createdEpic = taskService.createEpic(epic);

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

        Epic epicToUpdate = taskService.createEpic(epic);
        Epic updatedEpic = taskService.updateEpic(epicToUpdate.getId(), updateRequest);

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

        Epic epicToUpdate = taskService.createEpic(epic);
        Epic updatedEpic = taskService.updateEpic(epicToUpdate.getId(), updateRequest);

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

        Epic epicToUpdate = taskService.createEpic(epic);
        Epic updatedEpic = taskService.updateEpic(epicToUpdate.getId(), updateRequest);

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
        Epic epicToUpdate = taskService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.updateEpic(epicToUpdate.getId() + 1, updateRequest));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToUpdate.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic. Success")
    void addTaskToEpicSuccess() {
        Task taskToAdd = taskService.createTask(userId, task);
        Epic epicToAdd = taskService.createEpic(epic);

        Assertions.assertNull(taskToAdd.getEpic());

        Task addedTask = taskService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId());

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
    @DisplayName("Add task to epic when epic not found")
    void addTaskToEpicWhenEpicNotFoundMustThrowNotFoundException() {
        Epic epicToAdd = taskService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId() + 1, 10L));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToAdd.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic when task not found")
    void addTaskToEpicWhenTaskNotFoundMustThrowNotFoundException() {
        Epic epicToAdd = taskService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' was not found", taskToAdd.getId() + 1)));
    }

    @Test
    @DisplayName("Add task to epic when user is not epic executive")
    void addTaskToEpicWhenUserIsNotAuthorizedMustThrowNotAuthorizedException() {
        Epic epicToAdd = taskService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.addTaskToEpic(epicToAdd.getExecutiveId() + 1, epicToAdd.getId(), taskToAdd.getId()));

        assertThat(ex.getMessage(), is(String.format("User with id '%s' is not authorized to add tasks to " +
                "epic with id '%s'", epicToAdd.getExecutiveId() + 1, epicToAdd.getId())));

    }

    @Test
    @DisplayName("Add task to epic when epic and task have different eventId")
    void addTaskToEpicWhenEventIdDifferentMustThrowOperationNotAllowedException() {
        epic.setEventId(epic.getEventId() + 1);
        Epic epicToAdd = taskService.createEpic(epic);
        Task taskToAdd = taskService.createTask(userId, task);

        OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
                () -> taskService.addTaskToEpic(epicToAdd.getExecutiveId(), epicToAdd.getId(), taskToAdd.getId()));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' can not be added to epic " +
                "with id '%s' as they belong to different events", taskToAdd.getId(), epicToAdd.getId())));
    }

    @Test
    @DisplayName("Delete task from epic. Success")
    void deleteTaskFromEpicTest() {
        Epic createdEpic = taskService.createEpic(epic);
        Task createdTask = taskService.createTask(userId, task);

        Assertions.assertNull(createdTask.getEpic());

        Task addedTask = taskService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask.getId());

        assertThat(addedTask.getEpic().getId(), is(createdEpic.getId()));

        Task deletedFromEpicTask =
                taskService.deleteTaskFromEpic(createdEpic.getExecutiveId(), createdEpic.getId(), addedTask.getId());

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
        Epic epicToDeleteFrom = taskService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.deleteTaskFromEpic(epicToDeleteFrom.getExecutiveId(),
                        epicToDeleteFrom.getId() + 1, 10L));

        assertThat(ex.getMessage(), is(String.format("Epic with id '%s' was not found", epicToDeleteFrom.getId() + 1)));
    }

    @Test
    @DisplayName("Delete task from epic when task not found")
    void deleteTaskFromEpicWhenTaskNotFoundMustThrowNotFoundException() {
        Epic epicToDeleteFrom = taskService.createEpic(epic);
        Task taskToDelete = taskService.createTask(userId, task);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.deleteTaskFromEpic(epicToDeleteFrom.getExecutiveId(), epicToDeleteFrom.getId(),
                        taskToDelete.getId() + 1));

        assertThat(ex.getMessage(), is(String.format("Task with id '%s' was not found", taskToDelete.getId() + 1)));
    }

    @Test
    @DisplayName("Delete task from epic when user is not authorized")
    void deleteTaskFromEpicWhenUserIsNotAuthorizedMustThrowNotAuthorizedException() {
        Epic epicToDeleteFrom = taskService.createEpic(epic);
        Task taskToDelete = taskService.createTask(userId, task);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.addTaskToEpic(epicToDeleteFrom.getExecutiveId() + 1, epicToDeleteFrom.getId(),
                        taskToDelete.getId()));

        assertThat(ex.getMessage(), is(String.format("User with id '%s' is not authorized to add tasks to " +
                "epic with id '%s'", epicToDeleteFrom.getExecutiveId() + 1, epicToDeleteFrom.getId())));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when tasks are empty")
    void findEpicByIdWithEmptyTasks() {
        Epic createdEpic = taskService.createEpic(epic);

        Epic retrievedEpic = taskService.findEpicById(createdEpic.getId());

        assertThat(retrievedEpic, notNullValue());
        assertThat(retrievedEpic.getId(), is(createdEpic.getId()));
        assertThat(retrievedEpic.getTitle(), is(createdEpic.getTitle()));
        assertThat(retrievedEpic.getEventId(), is(createdEpic.getEventId()));
        assertThat(retrievedEpic.getDeadline(), is(createdEpic.getDeadline()));
        assertThat(retrievedEpic.getEpicsTasks(), notNullValue());
        assertThat(retrievedEpic.getEpicsTasks().size(), is(0));
    }

    @Test
    @DisplayName("Find epic by id with tasks, when tasks are empty")
    void findEpicByIdWithTasks() {
        Epic createdEpic = taskService.createEpic(epic);
        Task createdTask = taskService.createTask(userId, task);
        taskService.addTaskToEpic(createdEpic.getExecutiveId(), createdEpic.getId(), createdTask.getId());
        Epic retrievedEpic = taskService.findEpicById(createdEpic.getId());

        assertThat(retrievedEpic, notNullValue());
        assertThat(retrievedEpic.getId(), is(createdEpic.getId()));
        assertThat(retrievedEpic.getTitle(), is(createdEpic.getTitle()));
        assertThat(retrievedEpic.getEventId(), is(createdEpic.getEventId()));
        assertThat(retrievedEpic.getDeadline(), is(createdEpic.getDeadline()));
        assertThat(retrievedEpic.getEpicsTasks(), notNullValue());
        assertThat(retrievedEpic.getEpicsTasks().size(), is(1));
    }

    @Test
    @DisplayName("Find epic when epic not found")
    void findEpicNotExistMustThrowNotFoundException() {
        Epic createdEpic = taskService.createEpic(epic);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.findEpicById(createdEpic.getId() + 1));

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
                .description("task description" + id)
                .deadline(LocalDateTime.of(2025, 10, 10, 12, 34, 33))
                .status(TaskStatus.TODO)
                .assigneeId(3L)
                .eventId(5L)
                .build();
    }
}