package com.crane.bandwebbackend.domain.song.dto;

import com.crane.bandwebbackend.domain.song.entity.Song;
import com.crane.bandwebbackend.domain.song.entity.SongStatus;
import com.crane.bandwebbackend.domain.song.entity.WithdrawalType;
import com.crane.bandwebbackend.global.time.Kst;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SongResponse {

    private final Long id;
    private final Long performanceId;
    private final Long candidateTeamId;
    private final Long assignedTeamId;
    private final String title;
    private final String artist;
    private final String referenceUrl;
    private final String memo;
    private final SongStatus status;
    private final WithdrawalType withdrawalType;
    private final Long proposedByMembershipId;
    private final Integer setlistOrder;
    // 결정 사유(SONG-003 "결정 사유" 박스)
    private final String decisionNote;
    private final OffsetDateTime decidedAt;
    private final Long version;
    // "경고" 강도 규칙 결과. 없으면 빈 목록
    private final List<String> warnings;

    public static SongResponse from(Song song) {
        return from(song, List.of());
    }

    public static SongResponse from(Song song, List<String> warnings) {
        return SongResponse.builder()
                .id(song.getId())
                .performanceId(song.getPerformance().getId())
                .candidateTeamId(song.getCandidateTeam() == null ? null : song.getCandidateTeam().getId())
                .assignedTeamId(song.getAssignedTeam() == null ? null : song.getAssignedTeam().getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .referenceUrl(song.getReferenceUrl())
                .memo(song.getMemo())
                .status(song.getStatus())
                .withdrawalType(song.getWithdrawalType())
                .proposedByMembershipId(song.getProposedBy().getId())
                .setlistOrder(song.getSetlistOrder())
                .decisionNote(song.getDecisionNote())
                .decidedAt(Kst.fromUtc(song.getDecidedAt()))
                .version(song.getVersion())
                .warnings(warnings)
                .build();
    }
}
