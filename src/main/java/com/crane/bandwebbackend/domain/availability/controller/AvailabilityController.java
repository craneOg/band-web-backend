package com.crane.bandwebbackend.domain.availability.controller;

import com.crane.bandwebbackend.domain.availability.dto.AvailabilityRequest;
import com.crane.bandwebbackend.domain.availability.dto.HeatmapResponse;
import com.crane.bandwebbackend.domain.availability.dto.MyAvailabilityResponse;
import com.crane.bandwebbackend.domain.availability.service.AvailabilityService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances/{performanceId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/me")
    public MyAvailabilityResponse getMine(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal Long userId,
            @RequestParam LocalDate weekStart) {
        return availabilityService.getMine(performanceId, userId, weekStart);
    }

    @PutMapping("/me")
    public MyAvailabilityResponse replaceMine(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AvailabilityRequest req) {
        return availabilityService.replaceMine(performanceId, userId, req);
    }

    @GetMapping("/heatmap")
    public HeatmapResponse heatmap(
            @PathVariable Long performanceId,
            @AuthenticationPrincipal Long userId,
            @RequestParam LocalDate weekStart) {
        return availabilityService.heatmap(performanceId, userId, weekStart);
    }
}
