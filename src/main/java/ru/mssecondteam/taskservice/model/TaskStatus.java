package ru.mssecondteam.taskservice.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task status")
public enum TaskStatus {

    @Schema(description = "Default status after creation")
    TODO,

    @Schema(description = "Task is in progress")
    IN_PROGRESS,

    @Schema(description = "Task is completed")
    DONE,

    @Schema(description = "Task is cancelled")
    CANCELLED
}
