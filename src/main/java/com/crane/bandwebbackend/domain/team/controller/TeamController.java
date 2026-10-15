package com.crane.bandwebbackend.domain.team.controller;

import com.crane.bandwebbackend.domain.song.service.SongService;
import com.crane.bandwebbackend.domain.team.dto.TeamCreateRequest;
import com.crane.bandwebbackend.domain.team.dto.TeamLeaderRequest;
import com.crane.bandwebbackend.domain.team.dto.TeamResponse;
import com.crane.bandwebbackend.domain.team.dto.TeamVersionRequest;
import com.crane.bandwebbackend.domain.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances/{performanceId}/teams")
public class TeamController {

    private final TeamService teamService;
    private final SongService songService;

    // ponytail: 지금은 곡 연주자로 만드는 경우(sourceSongId)만. 일반 팀 편성은 명세 확정 후 같은 URL에 추가
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamCreateRequest req) {
        return songService.createTeamFromSong(performanceId, userId, req);
    }

    @PostMapping("/{teamId}/submit")
    public TeamResponse submit(
            @PathVariable Long performanceId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamVersionRequest req) {
        return teamService.submit(performanceId, teamId, userId, req);
    }

    @PostMapping("/{teamId}/unsubmit")
    public TeamResponse unsubmit(
            @PathVariable Long performanceId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamVersionRequest req) {
        return teamService.unsubmit(performanceId, teamId, userId, req);
    }

    @PostMapping("/{teamId}/approve")
    public TeamResponse approve(
            @PathVariable Long performanceId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamVersionRequest req) {
        return teamService.approve(performanceId, teamId, userId, req);
    }

    @PostMapping("/{teamId}/reject")
    public TeamResponse reject(
            @PathVariable Long performanceId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamVersionRequest req) {
        return teamService.reject(performanceId, teamId, userId, req);
    }

    @PostMapping("/{teamId}/reopen")
    public TeamResponse reopen(
            @PathVariable Long performanceId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamVersionRequest req) {
        return teamService.reopen(performanceId, teamId, userId, req);
    }

    @PutMapping("/{teamId}/leader")
    public TeamResponse changeLeader(
            @PathVariable Long performanceId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TeamLeaderRequest req) {
        return teamService.changeLeader(performanceId, teamId, userId, req);
    }
}
