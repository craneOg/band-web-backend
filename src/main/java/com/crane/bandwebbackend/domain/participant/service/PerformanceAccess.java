package com.crane.bandwebbackend.domain.participant.service;

import com.crane.bandwebbackend.domain.membership.entity.Membership;
import com.crane.bandwebbackend.domain.membership.entity.MembershipRole;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMemberStatus;
import com.crane.bandwebbackend.domain.performance.entity.FeatureState;
import com.crane.bandwebbackend.domain.performance.entity.Performance;
import com.crane.bandwebbackend.domain.performance.entity.PerformanceStatus;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 요청자 기준 공연 접근 정보. 역할은 요청마다 membership.role로 판단(G-07)
@Getter
@RequiredArgsConstructor
public class PerformanceAccess {

    private final Performance performance;
    private final Membership membership;
    // 공연 참여자가 아니면 NULL
    private final PerformanceMember performanceMember;

    public boolean isAdmin() {
        return membership.getRole() == MembershipRole.CLUB_ADMIN;
    }

    public Long clubId() {
        return membership.getClub().getId();
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "임원만 할 수 있습니다.");
        }
    }

    // 희망자(APPLIED)도 후보곡 제출·가불 가능
    public PerformanceMember requireParticipant() {
        if (performanceMember == null
                || (performanceMember.getStatus() != PerformanceMemberStatus.APPLIED
                        && performanceMember.getStatus() != PerformanceMemberStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "공연 참여자만 할 수 있습니다.");
        }
        return performanceMember;
    }

    public PerformanceMember requireApprovedParticipant() {
        if (performanceMember == null || performanceMember.getStatus() != PerformanceMemberStatus.APPROVED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "승인된 공연 참여자만 할 수 있습니다.");
        }
        return performanceMember;
    }

    // 종료·취소된 공연은 내용 변경 불가
    public void requireEditable() {
        PerformanceStatus status = performance.getStatus();
        if (status == PerformanceStatus.FINISHED || status == PerformanceStatus.CANCELED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "종료되었거나 취소된 공연입니다.");
        }
    }

    public boolean isLocked() {
        return performance.getStatus() == PerformanceStatus.LOCKED;
    }

    // 정책 기능 열림 여부: OPEN이고 마감 시각(UTC) 전
    public static void requireOpen(FeatureState state, LocalDateTime deadline, String feature) {
        if (state != FeatureState.OPEN
                || (deadline != null && !LocalDateTime.now(ZoneOffset.UTC).isBefore(deadline))) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, feature + " 기간이 아닙니다.");
        }
    }
}
