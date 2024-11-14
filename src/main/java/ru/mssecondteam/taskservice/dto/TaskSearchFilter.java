package ru.mssecondteam.taskservice.dto;

public record TaskSearchFilter(
        Long eventId,
        Long assignId,
        Long authorId
) {
}
