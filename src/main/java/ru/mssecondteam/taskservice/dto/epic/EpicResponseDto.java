package ru.mssecondteam.taskservice.dto.epic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.mssecondteam.taskservice.dto.TaskDto;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Epic")
public record EpicResponseDto(

        @Schema(description = "Epic id")
        Long id,

        @Schema(description = "Epic title")
        String title,

        @Schema(description = "Epic executive id")
        Long executiveId,

        @Schema(description = "Epic event id")
        Long eventId,

        @Schema(description = "Epic deadline")
        LocalDateTime deadline,

        @Schema(description = "Epic tasks")
        List<TaskDto> epicsTasks
) {
}
