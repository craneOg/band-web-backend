package com.crane.bandwebbackend.domain.team.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// API-086 대체: POST /teams + sourceSongId (곡 연주자로 팀 생성)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamCreateRequest {

    @NotNull
    private Long sourceSongId;

    // 비우면 "n팀" 기본값(T-07)
    @Size(max = 100)
    private String name;
}
