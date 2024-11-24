package ru.mssecondteam.taskservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicResponseDto;
import ru.mssecondteam.taskservice.model.TaskStatus;

import java.time.LocalDateTime;

@Builder
public record TaskDto(
        Long id,
        String title,
        String description,
        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime deadline,
        TaskStatus status,
        Long assigneeId,
        Long authorId,
        Long eventId,
        EpicResponseDto epic
) {
}
