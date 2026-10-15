package com.crane.bandwebbackend.domain.team.dto;

import com.crane.bandwebbackend.domain.team.entity.Team;
import com.crane.bandwebbackend.domain.team.entity.TeamSource;
import com.crane.bandwebbackend.domain.team.entity.TeamStatus;
import com.crane.bandwebbackend.global.time.Kst;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamResponse {

    private final Long id;
    private final Long performanceId;
    private final String name;
    private final TeamStatus status;
    private final TeamSource source;
    private final Long leaderMemberId;
    private final OffsetDateTime submittedAt;
    private final OffsetDateTime decidedAt;
    private final String rejectReason;
    private final Long version;

    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .performanceId(team.getPerformance().getId())
                .name(team.getName())
                .status(team.getStatus())
                .source(team.getSource())
                .leaderMemberId(team.getLeader() == null ? null : team.getLeader().getId())
                .submittedAt(Kst.fromUtc(team.getSubmittedAt()))
                .decidedAt(Kst.fromUtc(team.getDecidedAt()))
                .rejectReason(team.getRejectReason())
                .version(team.getVersion())
                .build();
    }
}
