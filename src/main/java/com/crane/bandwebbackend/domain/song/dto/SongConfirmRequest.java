package com.crane.bandwebbackend.domain.song.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// API-080 곡 확정: 담당 팀·순서·연주자·사유를 한 요청으로(X-05, SONG-003 9-1)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongConfirmRequest {

    @NotNull
    private Long version;

    // NULL이면 팀 범위 후보의 후보 팀. LOCKED 공연에서는 담당 팀 필수
    private Long assignedTeamId;

    // NULL이면 미배정(LOCKED 공연에서는 맨 뒤)
    @Positive
    private Integer setlistOrder;

    // 모든 확정 곡에 연주자 명시
    @NotEmpty
    @Valid
    private List<SessionMemberRequest> sessionMembers;

    @Size(max = 500)
    private String decisionNote;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SessionMemberRequest {

        @NotNull
        private Long performanceMemberId;

        @NotNull
        private Long partId;
    }
}
