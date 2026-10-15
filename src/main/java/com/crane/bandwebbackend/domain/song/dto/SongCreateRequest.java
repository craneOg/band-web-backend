package com.crane.bandwebbackend.domain.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// API-076 후보곡 등록 + 참고 링크·메모(UI-11)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String artist;

    // http/https만(UI-11)
    @Size(max = 500)
    @Pattern(regexp = "^https?://\\S+$", message = "http 또는 https 링크만 입력할 수 있습니다.")
    private String referenceUrl;

    @Size(max = 500)
    private String memo;

    // 값 있으면 팀 범위 후보
    private Long candidateTeamId;
}
