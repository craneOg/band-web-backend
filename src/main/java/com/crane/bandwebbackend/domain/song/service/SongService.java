package com.crane.bandwebbackend.domain.song.service;

import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccess;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccessService;
import com.crane.bandwebbackend.domain.performance.entity.CandidateScope;
import com.crane.bandwebbackend.domain.performance.entity.PerformancePolicy;
import com.crane.bandwebbackend.domain.song.dto.SongConfirmRequest;
import com.crane.bandwebbackend.domain.song.dto.SongCreateRequest;
import com.crane.bandwebbackend.domain.song.dto.SongDecisionRequest;
import com.crane.bandwebbackend.domain.song.dto.SongResponse;
import com.crane.bandwebbackend.domain.song.dto.SongTeamRequest;
import com.crane.bandwebbackend.domain.song.entity.Song;
import com.crane.bandwebbackend.domain.song.entity.SongSessionMember;
import com.crane.bandwebbackend.domain.song.entity.SongStatus;
import com.crane.bandwebbackend.domain.song.entity.WithdrawalType;
import com.crane.bandwebbackend.domain.song.repository.SongRepository;
import com.crane.bandwebbackend.domain.song.repository.SongSessionMemberRepository;
import com.crane.bandwebbackend.domain.team.dto.TeamCreateRequest;
import com.crane.bandwebbackend.domain.team.dto.TeamResponse;
import com.crane.bandwebbackend.domain.team.entity.Team;
import com.crane.bandwebbackend.domain.team.entity.TeamStatus;
import com.crane.bandwebbackend.domain.team.service.TeamService;
import com.crane.bandwebbackend.global.audit.AuditLog;
import com.crane.bandwebbackend.global.audit.AuditLogRepository;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;
    private final SongSessionMemberRepository sessionMemberRepository;
    private final PerformanceAccessService accessService;
    private final TeamService teamService;
    private final AuditLogRepository auditLogRepository;

    public List<SongResponse> list(Long performanceId, Long userId) {
        accessService.get(performanceId, userId);
        return songRepository.findByPerformanceIdOrderById(performanceId).stream()
                .map(SongResponse::from)
                .toList();
    }

    public Song getSong(Long performanceId, Long songId) {
        return songRepository
                .findByIdAndPerformanceId(songId, performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "곡을 찾을 수 없습니다."));
    }

    // API-076 후보곡 등록
    @Transactional
    public SongResponse create(Long performanceId, Long userId, SongCreateRequest req) {
        // 1인 한도(S-03)를 세기 전에 공연 행 잠금(0.4)
        PerformanceAccess access = accessService.getForUpdate(performanceId, userId);
        access.requireEditable();
        if (access.isLocked()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "확정된 공연에는 후보곡을 등록할 수 없습니다.");
        }
        PerformancePolicy policy = accessService.getPolicy(performanceId);
        PerformanceMember pm = null;
        if (!access.isAdmin()) {
            // 후보곡 신청이 DISABLED면 임원만 등록
            pm = access.requireParticipant();
            PerformanceAccess.requireOpen(policy.getCandidateState(), policy.getCandidateDeadline(), "후보곡 신청");
        }

        Team candidateTeam = null;
        if (req.getCandidateTeamId() != null) {
            candidateTeam = teamService.getTeam(performanceId, req.getCandidateTeamId());
            if (candidateTeam.getStatus() == TeamStatus.REJECTED || candidateTeam.getStatus() == TeamStatus.WITHDRAWN) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "반려되었거나 철회된 팀입니다.");
            }
            if (pm != null && !teamService.isMember(candidateTeam.getId(), pm.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "팀원만 팀 후보곡을 등록할 수 있습니다.");
            }
        } else if (policy.getCandidateScope() == CandidateScope.TEAM_MEMBERS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "팀 범위 후보곡만 등록할 수 있습니다. 팀을 선택해 주세요.");
        }

        Long membershipId = access.getMembership().getId();
        Integer limit = policy.getCandidateLimitPerMember();
        if (pm != null
                && limit != null
                && songRepository.countByPerformanceIdAndProposedByIdAndStatusNot(
                                performanceId, membershipId, SongStatus.WITHDRAWN)
                        >= limit) {
            throw new BusinessException(ErrorCode.RULE_VIOLATION, "1인 후보곡 신청 수(" + limit + "곡)를 넘었습니다.");
        }

        Song song = songRepository.save(Song.propose(
                access.getPerformance(),
                candidateTeam,
                req.getTitle().strip(),
                req.getArtist(),
                req.getReferenceUrl(),
                req.getMemo(),
                access.getMembership()));
        return SongResponse.from(song);
    }

    // API-080 확정. LOCKED 공연에서도 담당 팀·순서·연주자·사유를 한 번에 받아 허용(X-05)
    @Transactional
    public SongResponse confirm(Long performanceId, Long songId, Long userId, SongConfirmRequest req) {
        PerformanceAccess access = adminForUpdate(performanceId, userId);
        Song song = getSong(performanceId, songId);
        checkVersion(song, req.getVersion());

        Team team = req.getAssignedTeamId() == null
                ? null
                : teamService.getApprovedTeam(performanceId, req.getAssignedTeamId());
        if (access.isLocked() && team == null && song.getCandidateTeam() == null) {
            throw new BusinessException(ErrorCode.RULE_VIOLATION, "확정된 공연에서는 담당 팀을 지정해야 합니다.");
        }
        Integer order = req.getSetlistOrder();
        if (order != null && songRepository.existsByPerformanceIdAndSetlistOrder(performanceId, order)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 세트리스트 순서입니다.");
        }
        if (order == null && access.isLocked()) {
            order = songRepository.findMaxSetlistOrder(performanceId) + 1;
        }

        song.confirm(team, order, req.getDecisionNote());
        replaceSessionMembers(access, song, req.getSessionMembers());
        audit(access, song, SongStatus.PROPOSED, req.getDecisionNote());
        return respond(song, List.of());
    }

    // 확정 취소 CONFIRMED → PROPOSED
    @Transactional
    public SongResponse unconfirm(Long performanceId, Long songId, Long userId, SongDecisionRequest req) {
        return decide(performanceId, songId, userId, req, song -> song.unconfirm(req.getDecisionNote()));
    }

    // API-082 탈락. CONFIRMED → REJECTED도 한 번에(경고)
    @Transactional
    public SongResponse reject(Long performanceId, Long songId, Long userId, SongDecisionRequest req) {
        PerformanceAccess access = adminForUpdate(performanceId, userId);
        Song song = getSong(performanceId, songId);
        checkVersion(song, req.getVersion());
        SongStatus before = song.getStatus();
        List<String> warnings = before == SongStatus.CONFIRMED
                ? List.of("선정곡을 탈락 처리했습니다. 담당 팀과 세트리스트 순서가 해제되었습니다.")
                : List.of();
        song.reject(req.getDecisionNote());
        audit(access, song, before, req.getDecisionNote());
        return respond(song, warnings);
    }

    // 재검토 REJECTED → PROPOSED(SONG-003 9-5)
    @Transactional
    public SongResponse reopen(Long performanceId, Long songId, Long userId, SongDecisionRequest req) {
        return decide(performanceId, songId, userId, req, song -> song.reopen(req.getDecisionNote()));
    }

    /**
     * 후보곡 삭제 = WITHDRAWN(물리 삭제 아님). PROPOSED에서만.
     * 제안자 본인 → PROPOSER, 임원 → ADMIN(S-11, 사유는 감사 기록). WITHDRAWN은 1인 한도 제외(S-03)
     */
    @Transactional
    public void withdraw(Long performanceId, Long songId, Long userId, SongDecisionRequest req) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        access.requireEditable();
        Song song = getSong(performanceId, songId);
        checkVersion(song, req.getVersion());
        boolean proposer = song.getProposedBy().getId().equals(access.getMembership().getId());
        if (access.isAdmin()) {
            song.withdraw(proposer ? WithdrawalType.PROPOSER : WithdrawalType.ADMIN);
            if (!proposer) {
                audit(access, song, SongStatus.PROPOSED, req.getDecisionNote());
            }
        } else if (proposer) {
            song.withdraw(WithdrawalType.PROPOSER);
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "제안자나 임원만 삭제할 수 있습니다.");
        }
    }

    // API-084 담당 팀 지정(PUT 하위 리소스)
    @Transactional
    public SongResponse assignTeam(Long performanceId, Long songId, Long userId, SongTeamRequest req) {
        PerformanceAccess access = adminForUpdate(performanceId, userId);
        Song song = getSong(performanceId, songId);
        checkVersion(song, req.getVersion());
        if (req.getTeamId() == null && access.isLocked()) {
            throw new BusinessException(ErrorCode.RULE_VIOLATION, "확정된 공연에서는 담당 팀을 해제할 수 없습니다.");
        }
        song.assignTeam(req.getTeamId() == null ? null : teamService.getApprovedTeam(performanceId, req.getTeamId()));
        return respond(song, List.of());
    }

    // API-086 → POST /teams + sourceSongId
    @Transactional
    public TeamResponse createTeamFromSong(Long performanceId, Long userId, TeamCreateRequest req) {
        PerformanceAccess access = adminForUpdate(performanceId, userId);
        Song song = getSong(performanceId, req.getSourceSongId());
        List<SongSessionMember> sessions = sessionMemberRepository.findBySongIdOrderById(song.getId());
        Team team = teamService.createFromSessions(access, song, sessions, req.getName());
        songRepository.flush();
        return TeamResponse.from(team);
    }

    public List<Song> getSongs(Long performanceId, List<SongStatus> statuses) {
        return songRepository.findByPerformanceIdAndStatusInOrderById(performanceId, statuses);
    }

    public List<Song> getConfirmedSongs(Long performanceId) {
        return songRepository.findByPerformanceIdAndStatus(performanceId, SongStatus.CONFIRMED);
    }

    // 순서 일괄 변경: uk_song_setlist_order 때문에 한 트랜잭션에서 NULL로 비운 뒤 재부여
    @Transactional
    public void applySetlistOrders(Map<Song, Integer> orders) {
        orders.keySet().forEach(song -> song.changeSetlistOrder(null));
        songRepository.flush();
        orders.forEach(Song::changeSetlistOrder);
        songRepository.flush();
    }

    private SongResponse decide(
            Long performanceId, Long songId, Long userId, SongDecisionRequest req, Consumer<Song> action) {
        PerformanceAccess access = adminForUpdate(performanceId, userId);
        Song song = getSong(performanceId, songId);
        checkVersion(song, req.getVersion());
        SongStatus before = song.getStatus();
        action.accept(song);
        audit(access, song, before, req.getDecisionNote());
        return respond(song, List.of());
    }

    private PerformanceAccess adminForUpdate(Long performanceId, Long userId) {
        PerformanceAccess access = accessService.getForUpdate(performanceId, userId);
        access.requireAdmin();
        access.requireEditable();
        return access;
    }

    private void replaceSessionMembers(
            PerformanceAccess access, Song song, List<SongConfirmRequest.SessionMemberRequest> members) {
        Long performanceId = access.getPerformance().getId();
        Set<String> seen = new HashSet<>();
        sessionMemberRepository.deleteBySongId(song.getId());
        for (SongConfirmRequest.SessionMemberRequest m : members) {
            if (!seen.add(m.getPerformanceMemberId() + ":" + m.getPartId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "같은 연주자·파트가 중복되었습니다.");
            }
            sessionMemberRepository.save(SongSessionMember.of(
                    song,
                    accessService.getMember(performanceId, m.getPerformanceMemberId()),
                    accessService.getActivePart(access.clubId(), m.getPartId())));
        }
    }

    // 버전 증가를 응답에 반영
    private SongResponse respond(Song song, List<String> warnings) {
        songRepository.flush();
        return SongResponse.from(song, warnings);
    }

    private void audit(PerformanceAccess access, Song song, SongStatus before, String reason) {
        auditLogRepository.save(AuditLog.of(
                access.clubId(),
                access.getMembership().getUser().getId(),
                "SONG_DECIDED",
                "SONG",
                song.getId(),
                reason,
                "{\"from\":\"" + before + "\",\"to\":\"" + song.getStatus() + "\"}"));
    }

    private static void checkVersion(Song song, Long version) {
        if (!song.getVersion().equals(version)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "다른 사람이 먼저 수정했습니다. 새로고침 후 다시 시도해 주세요.");
        }
    }
}
