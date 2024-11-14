package ru.mssecondteam.taskservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        Map<String, String> errors,
        Integer statusCode,
        LocalDateTime timestamp
) {
}
