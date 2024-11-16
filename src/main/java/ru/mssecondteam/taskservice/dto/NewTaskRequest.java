package ru.mssecondteam.taskservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
public record NewTaskRequest(
        @NotBlank(message = "Title can not be blank")
        String title,
        String description,
        @Future(message = "Deadline must be in future")
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        LocalDateTime deadline,
        @NotNull(message = "Task must have status")
        TaskStatus status,
        @Positive(message = "Assignee Id must be positive")
        Long assigneeId,
        @Positive(message = "Event Id must be positive")
        @NotNull(message = "Event Id must be positive")
        Long eventId
) {
}
