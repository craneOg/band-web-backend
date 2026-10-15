package com.crane.bandwebbackend.domain.song.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 확정 취소·탈락·재검토·삭제 공통 본문. decisionNote = 결정 사유(삭제에서는 감사 기록 사유)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongDecisionRequest {

    @NotNull
    private Long version;

    @Size(max = 500)
    private String decisionNote;
}
