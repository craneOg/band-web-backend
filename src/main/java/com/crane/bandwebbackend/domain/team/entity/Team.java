package com.crane.bandwebbackend.domain.team.entity;

import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.performance.entity.Performance;
import com.crane.bandwebbackend.domain.user.entity.User;
import com.crane.bandwebbackend.global.entity.BaseTimeEntity;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "team",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_team_id_performance",
                    columnNames = {"id", "performance_id"}),
            @UniqueConstraint(
                    name = "uk_team_performance_name",
                    columnNames = {"performance_id", "name"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(nullable = false, length = 100)
    private String name;

    // 단일 팀 밴드의 기본 팀. 공연당 1개(서비스 검증)
    @Column(nullable = false)
    private boolean isDefault;

    // fk_team_leader_is_member: (id, leader_member_id) → team_member. 이 팀의 팀원이어야 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_member_id")
    private PerformanceMember leader;

    // 임원이 만든 팀은 바로 APPROVED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String rejectReason;

    @Version
    @Column(nullable = false)
    private Long version;

    // 임원이 만든 팀은 바로 APPROVED
    public static Team createApproved(Performance performance, String name, TeamSource source, User createdBy) {
        Team team = new Team();
        team.performance = performance;
        team.name = name;
        team.source = source;
        team.createdBy = createdBy;
        team.status = TeamStatus.APPROVED;
        team.decidedAt = now();
        return team;
    }

    public void submit() {
        requireStatus(TeamStatus.DRAFT);
        this.status = TeamStatus.SUBMITTED;
        this.submittedAt = now();
    }

    // SUBMITTED → DRAFT(제출 취소)
    public void unsubmit() {
        requireStatus(TeamStatus.SUBMITTED);
        this.status = TeamStatus.DRAFT;
        this.submittedAt = null;
    }

    public void approve() {
        requireStatus(TeamStatus.SUBMITTED);
        this.status = TeamStatus.APPROVED;
        this.decidedAt = now();
    }

    public void reject(String reason) {
        requireStatus(TeamStatus.SUBMITTED);
        this.status = TeamStatus.REJECTED;
        this.rejectReason = reason;
        this.decidedAt = now();
    }

    // REJECTED → DRAFT(다시 작성)
    public void reopen() {
        requireStatus(TeamStatus.REJECTED);
        this.status = TeamStatus.DRAFT;
        this.rejectReason = null;
        this.decidedAt = null;
        this.submittedAt = null;
    }

    // 팀원인지는 서비스 검증 + fk_team_leader_is_member
    public void changeLeader(PerformanceMember leader) {
        if (status == TeamStatus.WITHDRAWN) {
            throw invalidTransition();
        }
        this.leader = leader;
    }

    // 화면이 본 버전과 다르면 409(낙관적 락 대상). 실제 갱신 충돌은 @Version이 한 번 더 막음
    public void verifyVersion(Long expected) {
        if (!Objects.equals(version, expected)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "다른 사람이 먼저 수정했습니다. 새로고침 후 다시 시도해 주세요.");
        }
    }

    private void requireStatus(TeamStatus expected) {
        if (status != expected) {
            throw invalidTransition();
        }
    }

    private BusinessException invalidTransition() {
        return new BusinessException(ErrorCode.STATE_CONFLICT, "현재 팀 상태(" + status + ")에서는 할 수 없습니다.");
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
