package com.crane.bandwebbackend.domain.team.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamLeaderRequest {

    @NotNull
    private Long version;

    @NotNull
    private Long performanceMemberId;
}
