package com.crane.bandwebbackend.domain.team.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 팀 상태 전이 공통 본문. reason은 반려에서만 사용
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamVersionRequest {

    @NotNull
    private Long version;

    @Size(max = 500)
    private String reason;
}
