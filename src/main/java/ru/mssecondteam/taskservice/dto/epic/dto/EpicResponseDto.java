package ru.mssecondteam.taskservice.dto.epic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import ru.mssecondteam.taskservice.dto.TaskDto;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record EpicResponseDto(
        Long id,
        String title,
        Long executiveId,
        Long eventId,
        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime deadline,
        List<TaskDto> epicsTasks
) {
}
