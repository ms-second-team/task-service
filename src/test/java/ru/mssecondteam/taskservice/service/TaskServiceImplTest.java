package ru.mssecondteam.taskservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.model.TaskStatus;
import ru.mssecondteam.taskservice.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;

    private Long taskId;

    private Long userId;

    @Captor
    private ArgumentCaptor<Task> captor;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("task")
                .description("task description")
                .deadline(LocalDateTime.of(2025, 10, 10, 12, 34, 33))
                .status(TaskStatus.DONE)
                .assigneeId(3L)
                .eventId(5L)
                .authorId(4L)
                .build();
        taskId = 1L;
        userId = 2L;
    }

    @Test
    @DisplayName("Create task")
    void createTask_shouldSetAuthorId() {
        when(taskRepository.save(any()))
                .thenReturn(task);

        taskService.createTask(userId, task);

        verify(taskRepository).save(captor.capture());
        Task taskToSave = captor.getValue();

        assertThat(taskToSave.getAuthorId(), is(userId));

        verify(taskRepository, times(1)).save(taskToSave);
    }

    @Test
    @DisplayName("Update task by author")
    void updateTask_whenTaskExistsAndUserHasRightsAsAuthor_shouldUpdateTask() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("updated title")
                .description("updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        doNothing().when(taskMapper).updateTask(updateRequest, task);
        when(taskRepository.save(task))
                .thenReturn(task);
        task.setAuthorId(userId);

        taskService.updateTask(taskId, userId, updateRequest);

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, times(1)).updateTask(updateRequest, task);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    @DisplayName("Update task by assignee")
    void updateTask_whenTaskExistsAndUserHasRightsAsAssignee_shouldUpdateTask() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("updated title")
                .description("updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));
        doNothing().when(taskMapper).updateTask(updateRequest, task);
        when(taskRepository.save(task))
                .thenReturn(task);
        task.setAssigneeId(userId);

        taskService.updateTask(taskId, userId, updateRequest);

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, times(1)).updateTask(updateRequest, task);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    @DisplayName("Update task, user can not modify task")
    void updateTask_whenUserHasNotRightsToModify_shouldThrowNotAuthorizedException() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("updated title")
                .description("updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.updateTask(taskId, userId, updateRequest));

        assertThat(ex.getMessage(), is("User with id '" + userId + "' is not authorized to modify task with id '" +
                task.getId() + "'"));

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, never()).updateTask(any(), any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update task, task not found")
    void updateTask_whenTaskNotFound_shouldThrowNotFoundException() {
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("updated title")
                .description("updated description")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.updateTask(taskId, userId, updateRequest));

        assertThat(ex.getMessage(), is("Task with id '" + taskId + "' was not found"));

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, never()).updateTask(any(), any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Find task by id")
    void findTaskById_whenTaskIsFound_shouldReturnTask() {
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));

        taskService.findTaskById(taskId);

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("Find task by id, task not found")
    void findTaskById_whenTaskIsNotFound_shouldThrowNotFoundException() {
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.findTaskById(taskId));

        assertThat(ex.getMessage(), is("Task with id '" + taskId + "' was not found"));

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("Search tasks")
    void searchTasks() {
        TaskSearchFilter filter = TaskSearchFilter.builder().build();
        int page = 1;
        int size = 23;
        Specification<Task> spec = null;
        Pageable pageable = PageRequest.of(page, size);
        when(taskRepository.findAll(spec, pageable))
                .thenReturn(Page.empty());

        taskService.searchTasks(page, size, filter);

        verify(taskRepository, times(1)).findAll(spec, PageRequest.of(page, size));
    }

    @Test
    @DisplayName("Delete task by author")
    void deleteTaskById_whenTaskExistsByAuthor_shouldInvokeDelete() {
        task.setAuthorId(userId);
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));

        taskService.deleteTaskById(taskId, userId);

        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    @DisplayName("Delete task by assignee")
    void deleteTaskById_whenTaskExistsByAssignee_shouldThrowNotAuthorizedException() {
        task.setAssigneeId(userId);
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.deleteTaskById(taskId, userId));

        assertThat(ex.getMessage(), is("User with id '" + userId + "' is not authorized to delete task with id '" +
                task.getId() + "'"));

        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Delete task by other user")
    void deleteTaskById_whenTaskExistsByOtherUser_shouldThrowNotAuthorizedException() {
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(task));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> taskService.deleteTaskById(taskId, userId));

        assertThat(ex.getMessage(), is("User with id '" + userId + "' is not authorized to delete task with id '" +
                task.getId() + "'"));

        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Delete, task not found")
    void deleteTaskById_whenTaskIsNotFound_shouldThrowNotFoundException() {
        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> taskService.deleteTaskById(taskId, userId));

        assertThat(ex.getMessage(), is("Task with id '" + taskId + "' was not found"));

        verify(taskRepository, never()).deleteById(any());

    }
}
