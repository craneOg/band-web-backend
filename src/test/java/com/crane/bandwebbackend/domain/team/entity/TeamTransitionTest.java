package com.crane.bandwebbackend.domain.team.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crane.bandwebbackend.global.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TeamTransitionTest {

    private Team draft() {
        Team team = Team.createApproved(null, "1팀", TeamSource.SELF, null);
        ReflectionTestUtils.setField(team, "status", TeamStatus.DRAFT);
        return team;
    }

    @Test
    @DisplayName("제출 → 제출 취소(unsubmit) → 다시 DRAFT")
    void unsubmit() {
        Team team = draft();
        team.submit();
        assertThat(team.getSubmittedAt()).isNotNull();
        team.unsubmit();
        assertThat(team.getStatus()).isEqualTo(TeamStatus.DRAFT);
        assertThat(team.getSubmittedAt()).isNull();
    }

    @Test
    @DisplayName("반려 → reopen → DRAFT, 반려 사유 해제")
    void reopen() {
        Team team = draft();
        team.submit();
        team.reject("인원 부족");
        assertThat(team.getRejectReason()).isEqualTo("인원 부족");
        team.reopen();
        assertThat(team.getStatus()).isEqualTo(TeamStatus.DRAFT);
        assertThat(team.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("승인은 SUBMITTED에서만 409")
    void approveOnlySubmitted() {
        assertThatThrownBy(() -> draft().approve()).isInstanceOf(BusinessException.class);
    }
}
