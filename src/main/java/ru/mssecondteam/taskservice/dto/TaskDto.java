package ru.mssecondteam.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Task")
public record TaskDto(

        @Schema(description = "Task id")
        Long id,

        @Schema(description = "Task title")
        String title,

        @Schema(description = "Task description")
        String description,

        @Schema(description = "Task creation date")
        LocalDateTime createdAt,

//        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        @Schema(description = "Task deadline")
        LocalDateTime deadline,

        @Schema(description = "Task status")
        TaskStatus status,

        @Schema(description = "Task assignee id")
        Long assigneeId,

        @Schema(description = "Task author id")
        Long authorId,

        @Schema(description = "Task's event id")
        Long eventId,

        @Schema(description = "Task's epic id")
        Long epicId
) {
}
