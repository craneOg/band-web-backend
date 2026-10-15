package com.crane.bandwebbackend.domain.team.service;

import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccess;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccessService;
import com.crane.bandwebbackend.domain.part.entity.Part;
import com.crane.bandwebbackend.domain.song.entity.Song;
import com.crane.bandwebbackend.domain.song.entity.SongSessionMember;
import com.crane.bandwebbackend.domain.team.dto.TeamLeaderRequest;
import com.crane.bandwebbackend.domain.team.dto.TeamResponse;
import com.crane.bandwebbackend.domain.team.dto.TeamVersionRequest;
import com.crane.bandwebbackend.domain.team.entity.Team;
import com.crane.bandwebbackend.domain.team.entity.TeamMember;
import com.crane.bandwebbackend.domain.team.entity.TeamSource;
import com.crane.bandwebbackend.domain.team.entity.TeamStatus;
import com.crane.bandwebbackend.domain.team.repository.TeamMemberRepository;
import com.crane.bandwebbackend.domain.team.repository.TeamRepository;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PerformanceAccessService accessService;

    public Team getTeam(Long performanceId, Long teamId) {
        return teamRepository
                .findByIdAndPerformanceId(teamId, performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "팀을 찾을 수 없습니다."));
    }

    // 곡 담당 팀은 승인된 팀만
    public Team getApprovedTeam(Long performanceId, Long teamId) {
        Team team = getTeam(performanceId, teamId);
        if (team.getStatus() != TeamStatus.APPROVED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "승인된 팀만 담당 팀으로 지정할 수 있습니다.");
        }
        return team;
    }

    public boolean isMember(Long teamId, Long performanceMemberId) {
        return teamMemberRepository.existsByTeamIdAndPerformanceMemberId(teamId, performanceMemberId);
    }

    // 팀 ID → 팀원 performance_member ID
    public Map<Long, Set<Long>> memberIdsByTeam(Long performanceId) {
        return teamMemberRepository.findByPerformanceId(performanceId).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getTeam().getId(),
                        Collectors.mapping(m -> m.getPerformanceMember().getId(), Collectors.toSet())));
    }

    /**
     * 곡 연주자로 팀 생성(API-086 → POST /teams + sourceSongId). 임원이 만든 팀은 바로 APPROVED.
     * 팀원 = 곡 연주자(사람별 첫 파트를 대표 파트로), 생성한 팀을 곡 담당 팀으로 지정.
     */
    @Transactional
    public Team createFromSessions(PerformanceAccess access, Song song, List<SongSessionMember> sessions, String name) {
        if (!song.isConfirmed()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "확정된 곡만 연주자로 팀을 만들 수 있습니다.");
        }
        if (sessions.isEmpty()) {
            throw new BusinessException(ErrorCode.RULE_VIOLATION, "곡에 연주자가 없습니다.");
        }
        Long performanceId = access.getPerformance().getId();
        String teamName = (name == null || name.isBlank()) ? defaultName(performanceId) : name.strip();
        if (teamRepository.existsByPerformanceIdAndName(performanceId, teamName)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "같은 이름의 팀이 이미 있습니다.");
        }
        Team team = teamRepository.save(Team.createApproved(
                access.getPerformance(), teamName, TeamSource.FROM_SESSIONS, access.getMembership().getUser()));
        Map<PerformanceMember, Part> firstPart = new LinkedHashMap<>();
        sessions.forEach(s -> firstPart.putIfAbsent(s.getPerformanceMember(), s.getPart()));
        firstPart.forEach((pm, part) -> teamMemberRepository.save(TeamMember.of(team, pm, part)));
        song.assignTeam(team);
        return team;
    }

    // T-07 기본값 "n팀": 공연의 모든 상태 팀 수 + 1부터 비어 있는 번호
    private String defaultName(Long performanceId) {
        long n = teamRepository.countByPerformanceId(performanceId) + 1;
        while (teamRepository.existsByPerformanceIdAndName(performanceId, n + "팀")) {
            n++;
        }
        return n + "팀";
    }

    // 제출: 정책상 허용(T-02) 확인
    @Transactional
    public TeamResponse submit(Long performanceId, Long teamId, Long userId, TeamVersionRequest req) {
        return transition(performanceId, teamId, userId, req, false, (access, team) -> {
            if (!access.isAdmin()) {
                var policy = accessService.getPolicy(performanceId);
                PerformanceAccess.requireOpen(
                        policy.getTeamSubmissionState(), policy.getTeamSubmissionDeadline(), "팀 신청");
            }
            team.submit();
        });
    }

    // API-070 → POST /teams/{id}/unsubmit
    @Transactional
    public TeamResponse unsubmit(Long performanceId, Long teamId, Long userId, TeamVersionRequest req) {
        return transition(performanceId, teamId, userId, req, false, (access, team) -> team.unsubmit());
    }

    // API-071
    @Transactional
    public TeamResponse approve(Long performanceId, Long teamId, Long userId, TeamVersionRequest req) {
        return transition(performanceId, teamId, userId, req, true, (access, team) -> team.approve());
    }

    @Transactional
    public TeamResponse reject(Long performanceId, Long teamId, Long userId, TeamVersionRequest req) {
        return transition(performanceId, teamId, userId, req, true, (access, team) -> team.reject(req.getReason()));
    }

    // API-074 → POST /teams/{id}/reopen
    @Transactional
    public TeamResponse reopen(Long performanceId, Long teamId, Long userId, TeamVersionRequest req) {
        return transition(performanceId, teamId, userId, req, false, (access, team) -> team.reopen());
    }

    // API-068 PUT /teams/{id}/leader. 팀장은 이 팀의 팀원이어야 함(fk_team_leader_is_member)
    @Transactional
    public TeamResponse changeLeader(Long performanceId, Long teamId, Long userId, TeamLeaderRequest req) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        access.requireEditable();
        Team team = getTeam(performanceId, teamId);
        requireOwnerOrAdmin(access, team);
        checkVersion(team, req.getVersion());
        PerformanceMember leader = accessService.getMember(performanceId, req.getPerformanceMemberId());
        if (!isMember(teamId, leader.getId())) {
            throw new BusinessException(ErrorCode.RULE_VIOLATION, "팀장은 팀원 중에서 지정해야 합니다.");
        }
        team.changeLeader(leader);
        return TeamResponse.from(team);
    }

    private TeamResponse transition(
            Long performanceId,
            Long teamId,
            Long userId,
            TeamVersionRequest req,
            boolean adminOnly,
            BiConsumer<PerformanceAccess, Team> action) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        access.requireEditable();
        Team team = getTeam(performanceId, teamId);
        if (adminOnly) {
            access.requireAdmin();
        } else {
            requireOwnerOrAdmin(access, team);
        }
        checkVersion(team, req.getVersion());
        action.accept(access, team);
        teamRepository.flush();
        return TeamResponse.from(team);
    }

    // 팀을 만든 사람·팀장·임원
    private void requireOwnerOrAdmin(PerformanceAccess access, Team team) {
        if (access.isAdmin()) {
            return;
        }
        Long userId = access.getMembership().getUser().getId();
        PerformanceMember pm = access.getPerformanceMember();
        boolean leader = pm != null && team.getLeader() != null && team.getLeader().getId().equals(pm.getId());
        if (!team.getCreatedBy().getId().equals(userId) && !leader) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "팀장이나 팀을 만든 사람만 할 수 있습니다.");
        }
    }

    private static void checkVersion(Team team, Long version) {
        if (!team.getVersion().equals(version)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "다른 사람이 먼저 수정했습니다. 새로고침 후 다시 시도해 주세요.");
        }
    }
}
