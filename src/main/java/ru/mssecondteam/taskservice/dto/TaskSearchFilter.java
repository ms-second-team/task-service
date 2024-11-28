package ru.mssecondteam.taskservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Search filter")
public record TaskSearchFilter(

        @Schema(description = "Task event id")
        Long eventId,

        @Schema(description = "Task assignee id")
        Long assigneeId,

        @Schema(description = "Task author id")
        Long authorId
) {
}
