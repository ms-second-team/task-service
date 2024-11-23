package ru.mssecondteam.taskservice.dto.epic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EpicShortDto(
        Long id,
        String title,
        Long executiveId,
        Long eventId,
        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime deadline
) {
}
