package ru.mssecondteam.taskservice.dto;

import lombok.Builder;

@Builder
public record TaskSearchFilter(
        Long eventId,
        Long assigneeId,
        Long authorId
) {
}
