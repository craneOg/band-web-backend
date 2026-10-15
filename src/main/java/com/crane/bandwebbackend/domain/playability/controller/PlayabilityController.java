package com.crane.bandwebbackend.domain.playability.controller;

import com.crane.bandwebbackend.domain.playability.dto.PlayabilityMatrixResponse;
import com.crane.bandwebbackend.domain.playability.dto.PlayabilityRequest;
import com.crane.bandwebbackend.domain.playability.service.PlayabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances/{performanceId}")
public class PlayabilityController {

    private final PlayabilityService playabilityService;

    @GetMapping("/playability")
    public PlayabilityMatrixResponse matrix(@PathVariable Long performanceId, @AuthenticationPrincipal Long userId) {
        return playabilityService.matrix(performanceId, userId);
    }

    @PutMapping("/songs/{songId}/playability/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(
            @PathVariable Long performanceId,
            @PathVariable Long songId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlayabilityRequest req) {
        playabilityService.answer(performanceId, songId, userId, req);
    }
}
