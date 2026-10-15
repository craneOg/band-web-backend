package com.crane.bandwebbackend.domain.song.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// API-084 담당 팀 지정. teamId NULL = 해제(LOCKED 공연에서는 불가)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongTeamRequest {

    @NotNull
    private Long version;

    private Long teamId;
}
