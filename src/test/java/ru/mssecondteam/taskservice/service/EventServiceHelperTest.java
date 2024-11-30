package ru.mssecondteam.taskservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.mssecondteam.taskservice.client.EventClient;
import ru.mssecondteam.taskservice.dto.event.EventDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberRole;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceHelperTest {

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private EventServiceHelper eventServiceHelper;

    private Long userId;
    private Long assigneeId;
    private EventDto eventDto;
    private TeamMemberDto teamMemberDto1;
    private TeamMemberDto teamMemberDto2;

    @BeforeEach
    void init() {
        userId = 144L;
        assigneeId = 245L;
        eventDto = createEvent(1);
        teamMemberDto1 = TeamMemberDto.builder()
                .eventId(eventDto.id())
                .userId(userId)
                .role(TeamMemberRole.MANAGER)
                .build();
        teamMemberDto2 = TeamMemberDto.builder()
                .eventId(eventDto.id())
                .userId(assigneeId)
                .role(TeamMemberRole.MANAGER)
                .build();
    }


    @Test
    @DisplayName("User and assignee are part of the team")
    void checkIfEventExistsAndUsersAreEventTeamMembers_whenUserAndAssigneeArePartOfTheTeam() {
        when(eventClient.getEventById(userId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(eventDto, HttpStatus.OK));
        when(eventClient.getTeamsByEventId(userId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(List.of(teamMemberDto1, teamMemberDto2), HttpStatus.OK));

        eventServiceHelper.checkIfEventExistsAndUsersAreEventTeamMembers(userId, eventDto.id(), assigneeId);

        verify(eventClient, times(1)).getEventById(userId, eventDto.id());
        verify(eventClient, times(1)).getTeamsByEventId(userId, eventDto.id());
    }

    @Test
    @DisplayName("User (owner) and assignee are part of the team")
    void checkIfEventExistsAndUsersAreEventTeamMembers_whenUserOwnerAndAssigneeArePartOfTheTeam() {
        Long ownerId = eventDto.ownerId();
        when(eventClient.getEventById(ownerId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(eventDto, HttpStatus.OK));
        when(eventClient.getTeamsByEventId(ownerId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(List.of(teamMemberDto1, teamMemberDto2), HttpStatus.OK));

        eventServiceHelper.checkIfEventExistsAndUsersAreEventTeamMembers(ownerId, eventDto.id(), assigneeId);

        verify(eventClient, times(1)).getEventById(ownerId, eventDto.id());
        verify(eventClient, times(1)).getTeamsByEventId(ownerId, eventDto.id());
    }

    @Test
    @DisplayName("User is not a part of the team")
    void checkIfEventExistsAndUsersAreEventTeamMembers_whenUserIsNotATeamMember() {
        when(eventClient.getEventById(userId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(eventDto, HttpStatus.OK));
        when(eventClient.getTeamsByEventId(userId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(List.of(teamMemberDto2), HttpStatus.OK));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> eventServiceHelper.checkIfEventExistsAndUsersAreEventTeamMembers(userId, eventDto.id(), assigneeId));

        assertThat(ex.getMessage(), is(String.format("User is with id '%s' not a team member for event with id '%s'",
                userId, eventDto.id())));

        verify(eventClient, times(1)).getEventById(userId, eventDto.id());
        verify(eventClient, times(1)).getTeamsByEventId(userId, eventDto.id());
    }

    @Test
    @DisplayName("Assignee is not a part of the team")
    void checkIfEventExistsAndUsersAreEventTeamMembers_whenAssigneeIsNotATeamMember() {
        when(eventClient.getEventById(userId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(eventDto, HttpStatus.OK));
        when(eventClient.getTeamsByEventId(userId, eventDto.id()))
                .thenReturn(new ResponseEntity<>(List.of(teamMemberDto1), HttpStatus.OK));

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> eventServiceHelper.checkIfEventExistsAndUsersAreEventTeamMembers(userId, eventDto.id(), assigneeId));

        assertThat(ex.getMessage(), is(String.format("User is with id '%s' not a team member for event with id '%s'",
                assigneeId, eventDto.id())));

        verify(eventClient, times(1)).getEventById(userId, eventDto.id());
        verify(eventClient, times(1)).getTeamsByEventId(userId, eventDto.id());
    }

    private EventDto createEvent(int id) {
        return EventDto.builder()
                .id((long) id)
                .name("event name " + id)
                .description("event description " + id)
                .ownerId(id + 10L)
                .startDateTime(LocalDateTime.now().plusDays(id))
                .endDateTime(LocalDateTime.now().plusMonths(id))
                .build();
    }
}