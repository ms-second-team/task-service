package ru.mssecondteam.taskservice.dto.epic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EpicUpdateRequest(
        @Pattern(regexp = "^(?!\\s*$).+", message = "Title can not be empty")
        String title,
        @Positive(message = "Assignee Id must be positive")
        Long executiveId,
        @Future(message = "Deadline must be in future")
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        LocalDateTime deadline
) {
}
