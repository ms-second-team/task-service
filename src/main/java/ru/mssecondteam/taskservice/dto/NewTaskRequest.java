package ru.mssecondteam.taskservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
@Schema(description = "New task data")
public record NewTaskRequest(
        @NotBlank(message = "Title can not be blank")
        @Schema(description = "Task title")
        String title,

        @Schema(description = "Task description")
        String description,

        @Future(message = "Deadline must be in future")
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        @Schema(description = "Task deadline")
        LocalDateTime deadline,

        @NotNull(message = "Task must have status")
        @Schema(description = "Task status")
        TaskStatus status,

        @Positive(message = "Assignee Id must be positive")
        @Schema(description = "Task assignee id")
        Long assigneeId,

        @Positive(message = "Event Id must be positive")
        @NotNull(message = "Event Id must be positive")
        @Schema(description = "Task event id")
        Long eventId
) {
}
