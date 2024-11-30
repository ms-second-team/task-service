package ru.mssecondteam.taskservice.dto.event;

import lombok.Builder;

@Builder
public record TeamMemberDto(
        Long eventId,
        Long userId,
        TeamMemberRole role
) {
}
