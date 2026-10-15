package com.crane.bandwebbackend.domain.playability.service;

import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMemberStatus;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccess;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccessService;
import com.crane.bandwebbackend.domain.part.entity.Part;
import com.crane.bandwebbackend.domain.performance.entity.PerformancePolicy;
import com.crane.bandwebbackend.domain.playability.dto.PlayabilityMatrixResponse;
import com.crane.bandwebbackend.domain.playability.dto.PlayabilityRequest;
import com.crane.bandwebbackend.domain.playability.entity.PlayabilityAnswer;
import com.crane.bandwebbackend.domain.playability.entity.SongPlayability;
import com.crane.bandwebbackend.domain.playability.repository.SongPlayabilityRepository;
import com.crane.bandwebbackend.domain.song.entity.Song;
import com.crane.bandwebbackend.domain.song.entity.SongStatus;
import com.crane.bandwebbackend.domain.song.service.SongService;
import com.crane.bandwebbackend.domain.team.service.TeamService;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayabilityService {

    private static final List<SongStatus> TARGET_SONGS = List.of(SongStatus.PROPOSED, SongStatus.CONFIRMED);
    private static final List<PerformanceMemberStatus> TARGET_MEMBERS =
            List.of(PerformanceMemberStatus.APPLIED, PerformanceMemberStatus.APPROVED);

    private final SongPlayabilityRepository playabilityRepository;
    private final PerformanceAccessService accessService;
    private final SongService songService;
    private final TeamService teamService;

    // 내 가불 응답(파트 단위 upsert). 팀 범위 후보는 그 팀원만
    @Transactional
    public void answer(Long performanceId, Long songId, Long userId, PlayabilityRequest req) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        access.requireEditable();
        PerformanceMember pm = access.requireParticipant();
        PerformancePolicy policy = accessService.getPolicy(performanceId);
        PerformanceAccess.requireOpen(policy.getPlayabilityState(), policy.getPlayabilityDeadline(), "가불 응답");

        Song song = songService.getSong(performanceId, songId);
        if (!TARGET_SONGS.contains(song.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "응답할 수 없는 곡입니다.");
        }
        if (song.getCandidateTeam() != null && !teamService.isMember(song.getCandidateTeam().getId(), pm.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "팀 후보곡은 팀원만 응답할 수 있습니다.");
        }
        Part part = accessService.getActivePart(access.clubId(), req.getPartId());
        var user = access.getMembership().getUser();
        playabilityRepository
                .findBySongIdAndPerformanceMemberIdAndPartId(songId, pm.getId(), part.getId())
                .ifPresentOrElse(
                        p -> p.update(req.getAnswer(), req.getReason(), user),
                        () -> playabilityRepository.save(
                                SongPlayability.of(song, pm, part, req.getAnswer(), req.getReason(), user)));
    }

    // SONG-005: 곡마다 사람 수만큼 부르던 것을 한 번에
    public PlayabilityMatrixResponse matrix(Long performanceId, Long userId) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        boolean namesVisible =
                access.isAdmin() || accessService.getPolicy(performanceId).isPlayabilityResultPublic();

        List<PerformanceMember> members = accessService.getMembers(performanceId, TARGET_MEMBERS);
        Set<Long> memberIds = members.stream().map(PerformanceMember::getId).collect(Collectors.toSet());
        Map<Long, Set<Long>> teamMembers = teamService.memberIdsByTeam(performanceId);
        Map<Long, List<SongPlayability>> bySong = playabilityRepository.findByPerformanceId(performanceId).stream()
                .collect(Collectors.groupingBy(p -> p.getSong().getId()));

        int targetPairs = 0;
        int respondedPairs = 0;
        Set<Long> nonResponderIds = new HashSet<>();
        List<PlayabilityMatrixResponse.SongRow> rows = new ArrayList<>();
        for (Song song : songService.getSongs(performanceId, TARGET_SONGS)) {
            // 팀 범위 후보 = 그 팀원, 공연 범위 후보 = 참여자 전체
            Set<Long> targets = new HashSet<>(memberIds);
            if (song.getCandidateTeam() != null) {
                targets.retainAll(teamMembers.getOrDefault(song.getCandidateTeam().getId(), Set.of()));
            }
            List<SongPlayability> answers = bySong.getOrDefault(song.getId(), List.of()).stream()
                    .filter(p -> targets.contains(p.getPerformanceMember().getId()))
                    .toList();
            Set<Long> responded = answers.stream()
                    .map(p -> p.getPerformanceMember().getId())
                    .collect(Collectors.toSet());
            targets.stream().filter(id -> !responded.contains(id)).forEach(nonResponderIds::add);
            targetPairs += targets.size();
            respondedPairs += responded.size();

            Map<PlayabilityAnswer, Long> counts = new EnumMap<>(PlayabilityAnswer.class);
            for (PlayabilityAnswer a : PlayabilityAnswer.values()) {
                counts.put(a, 0L);
            }
            answers.forEach(p -> counts.merge(p.getAnswer(), 1L, Long::sum));
            rows.add(PlayabilityMatrixResponse.SongRow.builder()
                    .songId(song.getId())
                    .title(song.getTitle())
                    .status(song.getStatus())
                    .targetCount(targets.size())
                    .respondedCount(responded.size())
                    .answerCounts(counts)
                    .answers(namesVisible ? answers.stream().map(PlayabilityService::toAnswer).toList() : null)
                    .build());
        }

        return PlayabilityMatrixResponse.builder()
                .targetPairs(targetPairs)
                .respondedPairs(respondedPairs)
                .namesVisible(namesVisible)
                .songs(rows)
                .members(namesVisible ? members.stream().map(PlayabilityService::toMember).toList() : null)
                .nonResponders(
                        namesVisible
                                ? members.stream()
                                        .filter(m -> nonResponderIds.contains(m.getId()))
                                        .map(PlayabilityService::toMember)
                                        .toList()
                                : null)
                .build();
    }

    private static PlayabilityMatrixResponse.Answer toAnswer(SongPlayability p) {
        return PlayabilityMatrixResponse.Answer.builder()
                .performanceMemberId(p.getPerformanceMember().getId())
                .partId(p.getPart().getId())
                .answer(p.getAnswer())
                .reason(p.getReason())
                .build();
    }

    private static PlayabilityMatrixResponse.Member toMember(PerformanceMember m) {
        return PlayabilityMatrixResponse.Member.builder()
                .performanceMemberId(m.getId())
                .name(PerformanceAccessService.displayName(m))
                .build();
    }
}
