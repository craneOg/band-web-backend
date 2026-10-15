package com.crane.bandwebbackend.domain.song.controller;

import com.crane.bandwebbackend.domain.song.dto.SongConfirmRequest;
import com.crane.bandwebbackend.domain.song.dto.SongCreateRequest;
import com.crane.bandwebbackend.domain.song.dto.SongDecisionRequest;
import com.crane.bandwebbackend.domain.song.dto.SongResponse;
import com.crane.bandwebbackend.domain.song.dto.SongTeamRequest;
import com.crane.bandwebbackend.domain.song.service.SongService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 인증 주체 = users.id(Long). 보안 필터가 principal로 넣어 준다는 전제
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances/{performanceId}/songs")
public class SongController {

    private final SongService songService;

    @GetMapping
    public List<SongResponse> list(@PathVariable Long performanceId, @AuthenticationPrincipal Long userId) {
        return songService.list(performanceId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SongResponse create(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongCreateRequest req) {
        return songService.create(performanceId, userId, req);
    }

    @PostMapping("/{songId}/confirm")
    public SongResponse confirm(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongConfirmRequest req) {
        return songService.confirm(performanceId, songId, userId, req);
    }

    @PostMapping("/{songId}/unconfirm")
    public SongResponse unconfirm(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongDecisionRequest req) {
        return songService.unconfirm(performanceId, songId, userId, req);
    }

    @PostMapping("/{songId}/reject")
    public SongResponse reject(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongDecisionRequest req) {
        return songService.reject(performanceId, songId, userId, req);
    }

    @PostMapping("/{songId}/reopen")
    public SongResponse reopen(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongDecisionRequest req) {
        return songService.reopen(performanceId, songId, userId, req);
    }

    @DeleteMapping("/{songId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongDecisionRequest req) {
        songService.withdraw(performanceId, songId, userId, req);
    }

    @PutMapping("/{songId}/assigned-team")
    public SongResponse assignTeam(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SongTeamRequest req) {
        return songService.assignTeam(performanceId, songId, userId, req);
    }
}
