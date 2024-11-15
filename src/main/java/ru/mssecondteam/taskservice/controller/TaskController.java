package ru.mssecondteam.taskservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mssecondteam.taskservice.dto.NewTaskRequest;
import ru.mssecondteam.taskservice.dto.TaskDto;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.exception.DeadlineException;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.service.TaskService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    private final TaskMapper taskMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@RequestHeader("X-User-Id") Long userId,
                              @RequestBody @Valid NewTaskRequest newTask) {
        log.debug("Creating task '{}' by user with id '{}'", newTask.title(), userId);
        validateDeadline(newTask.deadline());
        final Task task = taskMapper.toModel(newTask);
        final Task createdTask = taskService.createTask(userId, task);
        return taskMapper.toDto(createdTask);
    }

    @PatchMapping("/{taskId}")
    public TaskDto updateTask(@PathVariable Long taskId,
                              @RequestHeader("X-User-Id") Long userId,
                              @RequestBody TaskUpdateRequest updateRequest) {
        log.debug("Updating task with id '{}' by user with id '{}'", taskId, userId);
        validateDeadline(updateRequest.deadline());
        final Task updatedTask = taskService.updateTask(taskId, userId, updateRequest);
        return taskMapper.toDto(updatedTask);
    }

    @GetMapping("/{taskId}")
    public TaskDto findTaskById(@PathVariable Long taskId,
                                @RequestHeader("X-User-Id") Long userId) {
        log.debug("User with id '{}' requesting task with id '{}", userId, taskId);
        final Task task = taskService.findTaskById(taskId);
        return taskMapper.toDto(task);
    }

    @GetMapping
    public List<TaskDto> searchTasks(@RequestParam(defaultValue = "0") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     TaskSearchFilter searchFilter,
                                     @RequestHeader("X-User-Id") Long userId) {
        List<Task> tasks = taskService.searchTasks(page, size, searchFilter);
        return taskMapper.toDtoList(tasks);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskById(@PathVariable Long taskId,
                               @RequestHeader("X-User-Id") Long userId) {
        log.debug("User with id '{}' deleting task with id '{}'", userId, taskId);
        taskService.deleteTaskById(taskId, userId);
    }

    private void validateDeadline(LocalDateTime deadline) {
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            throw new DeadlineException("Deadline can not be in past: " + deadline);
        }
    }
}
