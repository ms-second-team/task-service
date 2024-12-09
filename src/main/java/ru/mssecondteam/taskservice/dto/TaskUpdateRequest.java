package ru.mssecondteam.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Update task data")
public record TaskUpdateRequest(

        @Pattern(regexp = "^(?!\\s*$).+", message = "Title can not be empty")
        @Schema(description = "Task title")
        String title,

        @Schema(description = "Task description")
        String description,

        @Future(message = "Deadline must be in future")
        @Schema(description = "Task deadline")
        LocalDateTime deadline,

        @Schema(description = "Task status")
        TaskStatus status,

        @Positive(message = "Event Id must be positive")
        @Schema(description = "Task event id")
        Long eventId
) {
}
