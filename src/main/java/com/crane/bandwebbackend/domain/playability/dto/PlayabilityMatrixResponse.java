package com.crane.bandwebbackend.domain.playability.dto;

import com.crane.bandwebbackend.domain.playability.entity.PlayabilityAnswer;
import com.crane.bandwebbackend.domain.song.entity.SongStatus;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * SONG-005 곡×사람 응답 표 + 응답률(UI-25) + 미응답자를 한 번에.
 * 결과 비공개(V-09)면 부원에게는 집계 숫자만(answers·members·nonResponders = NULL)
 */
@Getter
@Builder
public class PlayabilityMatrixResponse {

    // 응답한 (곡, 대상자) 쌍 / 전체 (곡, 대상자) 쌍. 대상이 없으면 0
    private final int respondedPairs;
    private final int targetPairs;
    private final boolean namesVisible;
    private final List<SongRow> songs;
    private final List<Member> members;
    private final List<Member> nonResponders;

    @Getter
    @Builder
    public static class SongRow {

        private final Long songId;
        private final String title;
        private final SongStatus status;
        private final int targetCount;
        private final int respondedCount;
        private final Map<PlayabilityAnswer, Long> answerCounts;
        private final List<Answer> answers;
    }

    @Getter
    @Builder
    public static class Answer {

        private final Long performanceMemberId;
        private final Long partId;
        private final PlayabilityAnswer answer;
        private final String reason;
    }

    @Getter
    @Builder
    public static class Member {

        private final Long performanceMemberId;
        private final String name;
    }
}
