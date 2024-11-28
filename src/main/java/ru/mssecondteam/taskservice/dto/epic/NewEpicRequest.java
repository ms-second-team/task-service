package ru.mssecondteam.taskservice.dto.epic;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "New epic")
public record NewEpicRequest(
        @NotBlank(message = "Title can not be blank")
        @Schema(description = "Epic title")
        String title,

        @Positive(message = "Assignee Id must be positive")
        @Schema(description = "Epic executive id")
        Long executiveId,

        @Positive(message = "Event Id must be positive")
        @Schema(description = "Epic event id")
        Long eventId,

        @Future(message = "Deadline must be in future")
        @JsonFormat(pattern = "HH:ss:mm dd.MM.yyyy")
        @Schema(description = "Epic deadline")
        LocalDateTime deadline
) {
}
