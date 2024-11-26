package ru.mssecondteam.taskservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.mssecondteam.taskservice.dto.epic.EpicResponseDto;
import ru.mssecondteam.taskservice.dto.epic.EpicUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.NewEpicRequest;
import ru.mssecondteam.taskservice.mapper.EpicMapper;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.service.EpicService;

@RestController
@RequestMapping("/epics")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EpicController {
    private final EpicService epicService;

    private final TaskMapper taskMapper;

    private final EpicMapper epicMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EpicResponseDto createEpic(@RequestBody @Valid NewEpicRequest newEpic) {
        log.debug("Creating epic '{}'", newEpic.title());
        final Epic epic = epicMapper.toEpicModel(newEpic);
        final Epic createdEpic = epicService.createEpic(epic);
        return epicMapper.toEpicResponseDto(createdEpic);
    }

    @PatchMapping("/{epicId}")
    public EpicResponseDto updateEpic(@PathVariable @Positive Long epicId,
                                      @RequestBody @Valid EpicUpdateRequest updateRequest) {
        log.debug("Updating epic with id '{}'", epicId);
        final Epic updatedEpic = epicService.updateEpic(epicId, updateRequest);
        return epicMapper.toEpicResponseDto(updatedEpic);
    }

    @PatchMapping("/{epicId}/tasks/{taskId}")
    public EpicResponseDto addTaskToEpic(@RequestHeader("X-User-Id") @Positive Long userId,
                                     @PathVariable @Positive Long epicId,
                                     @PathVariable @Positive Long taskId) {
        log.debug("Adding task with id '{}' to epic with id '{}' by user with id '{}'", taskId, epicId, userId);
        final Epic epicWithAddedTask = epicService.addTaskToEpic(userId, epicId, taskId);
        return epicMapper.toEpicResponseDto(epicWithAddedTask);
    }

    @DeleteMapping("/{epicId}/tasks/{taskId}/delete")
    public EpicResponseDto deleteTaskFromEpic(@RequestHeader("X-User-Id") @Positive Long userId,
                                      @PathVariable @Positive Long epicId,
                                      @PathVariable @Positive Long taskId) {
        log.debug("Deleting task with id '{}' from epic with id '{}' by user with id '{}'", taskId, epicId, userId);
        final Epic EpicWithDeletedTask = epicService.deleteTaskFromEpic(userId, epicId, taskId);
        return epicMapper.toEpicResponseDto(EpicWithDeletedTask);
    }

    @GetMapping("/{epicId}")
    public EpicResponseDto findEpicById(@PathVariable @Positive Long epicId) {
        log.debug("Retrieving Epic with id '{}'", epicId);
        final Epic epic = epicService.findEpicById(epicId);
        return epicMapper.toEpicResponseDto(epic);
    }
}
