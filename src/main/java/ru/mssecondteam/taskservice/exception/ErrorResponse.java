package ru.mssecondteam.taskservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Error")
public record ErrorResponse(

        @Schema(description = "List of errors")
        Map<String, String> errors,

        @Schema(description = "Response status")
        Integer status,

        @Schema(description = "Timestamp")
        LocalDateTime timestamp
) {
}
