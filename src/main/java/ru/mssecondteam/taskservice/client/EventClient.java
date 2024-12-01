package ru.mssecondteam.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.mssecondteam.taskservice.config.EventClientConfig;
import ru.mssecondteam.taskservice.dto.event.EventDto;
import ru.mssecondteam.taskservice.dto.event.TeamMemberDto;

import java.util.List;

@FeignClient(name = "eventClient", url = "${event-service.url}", configuration = EventClientConfig.class)
public interface EventClient {

    @GetMapping("/events/{eventId}")
    ResponseEntity<EventDto> getEventById(@RequestHeader("X-User-Id") Long userId, @PathVariable Long eventId);

    @GetMapping("/events/teams/{eventId}")
    ResponseEntity<List<TeamMemberDto>> getTeamsByEventId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long eventId);
}
