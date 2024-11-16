package ru.mssecondteam.taskservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
public record TaskUpdateRequest(
        @NotEmpty(message = "Title can not be empty")
        String title,
        String description,
        @Future(message = "Deadline must be in future")
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        LocalDateTime deadline,
        TaskStatus status,
        @Positive(message = "Event Id must be positive")
        Long eventId
) {
}
