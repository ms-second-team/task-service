package ru.mssecondteam.taskservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
public record TaskUpdateRequest(
        String title,
        String description,
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        LocalDateTime deadline,
        TaskStatus status,
        Long eventId
) {
}
