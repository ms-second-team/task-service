package ru.mssecondteam.taskservice.dto.epic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NewEpicRequest(
        @NotBlank(message = "Title can not be blank")
        String title,

        @Positive(message = "Assignee Id must be positive")
        Long executiveId,

        @Positive(message = "Event Id must be positive")
        Long eventId,

        @Future(message = "Deadline must be in future")
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        LocalDateTime deadline
) {
}
