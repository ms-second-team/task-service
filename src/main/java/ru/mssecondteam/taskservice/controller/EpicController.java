package ru.mssecondteam.taskservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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

@RestController
@RequestMapping("/epics")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EpicController {
    private final TaskService taskService;

    private final TaskMapper taskMapper;

    private final EpicMapper epicMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EpicShortDto createEpic(@RequestBody @Valid NewEpicRequest newEpic) {
        log.debug("Creating epic '{}'", newEpic.title());
        final Epic epic = epicMapper.toEpicModel(newEpic);
        final Epic createdEpic = taskService.createEpic(epic);
        return epicMapper.toEpicShortDto(createdEpic);
    }

    @PatchMapping("/{epicId}")
    public EpicShortDto updateEpic(@PathVariable @Positive Long epicId,
                                   @RequestBody @Valid EpicUpdateRequest updateRequest) {
        log.debug("Updating epic with id '{}'", epicId);
        final Epic updatedEpic = taskService.updateEpic(epicId, updateRequest);
        return epicMapper.toEpicShortDto(updatedEpic);
    }

    @PatchMapping("/{epicId}/tasks/{taskId}")
    public TaskFullDto addTaskToEpic(@RequestHeader("X-User-Id") @Positive Long userId,
                                     @PathVariable @Positive Long epicId,
                                     @PathVariable @Positive Long taskId) {
        log.debug("Adding task with id '{}' to epic with id '{}' by user with id '{}'", taskId, epicId, userId);
        final Task addedToEpicTask = taskService.addTaskToEpic(userId, epicId, taskId);
        return taskMapper.toTaskFullDto(addedToEpicTask);
    }

    @PatchMapping("/{epicId}/tasks/{taskId}/delete")
    public TaskFullDto deleteTaskFromEpic(@RequestHeader("X-User-Id") @Positive Long userId,
                                          @PathVariable @Positive Long epicId,
                                          @PathVariable @Positive Long taskId) {
        log.debug("Deleting task with id '{}' from epic with id '{}' by user with id '{}'", taskId, epicId, userId);
        final Task deletedFromEpicTask = taskService.deleteTaskFromEpic(userId, epicId, taskId);
        return taskMapper.toTaskFullDto(deletedFromEpicTask);
    }

    @GetMapping("/{epicId}")
    public EpicFullDto findEpicById(@PathVariable @Positive Long epicId) {
        log.debug("Retrieving Epic with id '{}'", epicId);
        final Epic epic = taskService.findEpicById(epicId);
        return epicMapper.toEpicFullDto(epic);
    }
}
