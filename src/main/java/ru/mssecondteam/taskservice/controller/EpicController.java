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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mssecondteam.taskservice.dto.epic.EpicResponseDto;
import ru.mssecondteam.taskservice.dto.epic.EpicUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.NewEpicRequest;
import ru.mssecondteam.taskservice.exception.ErrorResponse;
import ru.mssecondteam.taskservice.mapper.EpicMapper;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.service.EpicService;

@RestController
@RequestMapping("/epics")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Epics API")
public class EpicController {

    private final EpicService epicService;

    private final TaskMapper taskMapper;

    private final EpicMapper epicMapper;

    @Operation(summary = "Create epic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created new epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = EpicResponseDto.class))
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
    public EpicResponseDto createEpic(@Parameter(description = "New epic")
                                      @RequestBody @Valid NewEpicRequest newEpic) {
        log.debug("Creating epic '{}'", newEpic.title());
        final Epic epic = epicMapper.toEpicModel(newEpic);
        final Epic createdEpic = epicService.createEpic(epic);
        return epicMapper.toEpicResponseDto(createdEpic);
    }

    @Operation(summary = "Update epic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Epic is updated", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = EpicResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Validation error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Epic not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PatchMapping("/{epicId}")
    public EpicResponseDto updateEpic(@Parameter(description = "Epic id")
                                      @PathVariable @Positive Long epicId,
                                      @Parameter(description = "Epic update data")
                                      @RequestBody @Valid EpicUpdateRequest updateRequest) {
        log.debug("Updating epic with id '{}'", epicId);
        final Epic updatedEpic = epicService.updateEpic(epicId, updateRequest);
        return epicMapper.toEpicResponseDto(updatedEpic);
    }

    @Operation(summary = "Add task to epic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task is added to epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = EpicResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "403", description = "User is not authorized to modify epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "403", description = "Task can not be added to epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Epic not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @PatchMapping("/{epicId}/tasks/{taskId}")
    public EpicResponseDto addTaskToEpic(@RequestHeader("X-User-Id") @Positive Long userId,
                                         @Parameter(description = "Epic id")
                                         @PathVariable @Positive Long epicId,
                                         @Parameter(description = "Task id")
                                         @PathVariable @Positive Long taskId) {
        log.debug("Adding task with id '{}' to epic with id '{}' by user with id '{}'", taskId, epicId, userId);
        final Epic epicWithAddedTask = epicService.addTaskToEpic(userId, epicId, taskId);
        return epicMapper.toEpicResponseDto(epicWithAddedTask);
    }

    @Operation(summary = "Delete task from epic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task is deleted from epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = EpicResponseDto.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "403", description = "User is not authorized to modify epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "403", description = "Task does not belong to epic", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "404", description = "Epic not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @DeleteMapping("/{epicId}/tasks/{taskId}/delete")
    public EpicResponseDto deleteTaskFromEpic(@RequestHeader("X-User-Id") @Positive Long userId,
                                              @Parameter(description = "Epic id")
                                              @PathVariable @Positive Long epicId,
                                              @Parameter(description = "Task id")
                                              @PathVariable @Positive Long taskId) {
        log.debug("Deleting task with id '{}' from epic with id '{}' by user with id '{}'", taskId, epicId, userId);
        final Epic EpicWithDeletedTask = epicService.deleteTaskFromEpic(userId, epicId, taskId);
        return epicMapper.toEpicResponseDto(EpicWithDeletedTask);
    }

    @Operation(summary = "Find epic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Epic is found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = EpicResponseDto.class))
            }),
            @ApiResponse(responseCode = "404", description = "Epic not found", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            }),
            @ApiResponse(responseCode = "500", description = "Unknown error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            })
    })
    @GetMapping("/{epicId}")
    public EpicResponseDto findEpicById(@Parameter(description = "Event id")
                                        @PathVariable @Positive Long epicId) {
        log.debug("Retrieving Epic with id '{}'", epicId);
        final Epic epic = epicService.findEpicById(epicId);
        return epicMapper.toEpicResponseDto(epic);
    }
}
