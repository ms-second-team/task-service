package ru.mssecondteam.taskservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mssecondteam.taskservice.client.EventClient;
import ru.mssecondteam.taskservice.dto.event.EventDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberDto;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceHelper {

    private final EventClient eventClient;

    public void checkIfEventExistsAndUsersAreEventTeamMembers(Long userId, Long eventId, Long teamMemberId) {
        final EventDto event = eventClient.getEventById(userId, eventId).getBody();
        final List<Long> eventTeamMembersId = eventClient.getTeamsByEventId(userId, eventId).getBody().stream()
                .map(TeamMemberDto::userId)
                .collect(Collectors.toList());
        eventTeamMembersId.add(event.ownerId());

        checkIfUserIsATeamMember(eventTeamMembersId, userId, eventId);
        checkIfUserIsATeamMember(eventTeamMembersId, teamMemberId, eventId);
    }

    private void checkIfUserIsATeamMember(List<Long> teamMembersIds, Long userId, Long eventId) {
        if (!teamMembersIds.contains(userId)) {
            throw new NotAuthorizedException(String.format("User is with id '%s' not a team member for event with id '%s'",
                    userId, eventId));
        }
    }
}
