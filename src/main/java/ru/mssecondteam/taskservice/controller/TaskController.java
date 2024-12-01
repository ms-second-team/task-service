package ru.mssecondteam.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
import ru.mssecondteam.taskservice.exception.ErrorResponse;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Tasks API")
public class TaskController {

    private final TaskService taskService;

    private final TaskMapper taskMapper;

    @Operation(summary = "Create task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created new task", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@RequestHeader("X-User-Id") Long userId,
                              @Parameter(description = "New task data")
                              @RequestBody @Valid NewTaskRequest newTask) {
        log.debug("Creating task '{}' by user with id '{}'", newTask.title(), userId);
        final Task task = taskMapper.toModel(newTask);
        final Task createdTask = taskService.createTask(userId, task);
        return taskMapper.toDto(createdTask);
    }

    @Operation(summary = "Update task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task is updated", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "403", description = "User not authorized to modify task", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Task is not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PatchMapping("/{taskId}")
    public TaskDto updateTask(@Parameter(description = "Task's id to update")
                              @PathVariable Long taskId,
                              @RequestHeader("X-User-Id") Long userId,
                              @Parameter(description = "Update task data")
                              @RequestBody @Valid TaskUpdateRequest updateRequest) {
        log.debug("Updating task with id '{}' by user with id '{}'", taskId, userId);
        final Task updatedTask = taskService.updateTask(taskId, userId, updateRequest);
        return taskMapper.toDto(updatedTask);
    }

    @Operation(summary = "Find task by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task is found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))
            }),
            @ApiResponse(responseCode = "404", description = "Task is not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping("/{taskId}")
    public TaskDto findTaskById(@Parameter(description = "Task's id")
                                @PathVariable Long taskId,
                                @RequestHeader("X-User-Id") Long userId) {
        log.debug("User with id '{}' requesting task with id '{}", userId, taskId);
        final Task task = taskService.findTaskById(taskId);
        return taskMapper.toDto(task);
    }

    @Operation(summary = "Search tasks")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returned tasks", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = TaskDto.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping
    public List<TaskDto> searchTasks(@Parameter(description = "Page number")
                                     @RequestParam(defaultValue = "0") @PositiveOrZero Integer page,
                                     @Parameter(description = "Number of tasks per page")
                                     @RequestParam(defaultValue = "10") @Positive Integer size,
                                     @Parameter(description = "Search filer")
                                     TaskSearchFilter searchFilter,
                                     @RequestHeader("X-User-Id") Long userId) {
        List<Task> tasks = taskService.searchTasks(page, size, searchFilter);
        return taskMapper.toDtoList(tasks);
    }

    @Operation(summary = "Delete task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task has been deleted"),
            @ApiResponse(responseCode = "403", description = "User not authorized to delete task", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskById(@Parameter(description = "Task's id")
                               @PathVariable Long taskId,
                               @RequestHeader("X-User-Id") Long userId) {
        log.debug("User with id '{}' deleting task with id '{}'", userId, taskId);
        taskService.deleteTaskById(taskId, userId);
    }
}
