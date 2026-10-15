package com.crane.bandwebbackend.domain.participant.service;

import com.crane.bandwebbackend.domain.membership.entity.MemberType;
import com.crane.bandwebbackend.domain.membership.entity.Membership;
import com.crane.bandwebbackend.domain.membership.entity.MembershipStatus;
import com.crane.bandwebbackend.domain.part.entity.Part;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMemberStatus;
import com.crane.bandwebbackend.domain.performance.entity.Performance;
import com.crane.bandwebbackend.domain.performance.entity.PerformancePolicy;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * song·team·setlist·availability·playability가 공유하는 공연 접근·조회.
 * ponytail: membership·participant·part 도메인 service가 생기면 그쪽 호출로 교체(지금은 JPQL 직접 조회)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceAccessService {

    private static final List<MembershipStatus> ACCESSIBLE =
            List.of(MembershipStatus.ACTIVE, MembershipStatus.ON_LEAVE, MembershipStatus.GRADUATED);

    private final EntityManager em;

    public PerformanceAccess get(Long performanceId, Long userId) {
        return load(em.find(Performance.class, performanceId), userId);
    }

    // 여러 행에 걸친 조건(한도, 순서)은 공연 행을 잠근 뒤 한 트랜잭션에서 검사(0.4)
    public PerformanceAccess getForUpdate(Long performanceId, Long userId) {
        return load(em.find(Performance.class, performanceId, LockModeType.PESSIMISTIC_WRITE), userId);
    }

    private PerformanceAccess load(Performance performance, Long userId) {
        if (performance == null) {
            throw notFound();
        }
        // 소속·참여자를 한 번에(모든 요청이 지나가는 경로). 다른 동아리 공연은 존재를 숨김(G-04)
        Object[] row = em
                .createQuery(
                        "select m, pm from Membership m"
                                + " left join PerformanceMember pm on pm.membership = m and pm.performance.id = :pid"
                                + " where m.club.id = :clubId and m.user.id = :userId and m.status in :statuses",
                        Object[].class)
                .setParameter("pid", performance.getId())
                .setParameter("clubId", performance.getClub().getId())
                .setParameter("userId", userId)
                .setParameter("statuses", ACCESSIBLE)
                .getResultStream()
                .findFirst()
                .orElseThrow(PerformanceAccessService::notFound);
        Membership membership = (Membership) row[0];
        PerformanceMember pm = (PerformanceMember) row[1];
        // 외부 객원은 자신이 참여한 공연만 열람
        if (membership.getMemberType() == MemberType.GUEST && pm == null) {
            throw notFound();
        }
        return new PerformanceAccess(performance, membership, pm);
    }

    public PerformancePolicy getPolicy(Long performanceId) {
        PerformancePolicy policy = em.find(PerformancePolicy.class, performanceId);
        if (policy == null) {
            throw notFound();
        }
        return policy;
    }

    public PerformanceMember getMember(Long performanceId, Long performanceMemberId) {
        PerformanceMember pm = em.find(PerformanceMember.class, performanceMemberId);
        if (pm == null || !pm.getPerformance().getId().equals(performanceId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공연 참여자를 찾을 수 없습니다.");
        }
        return pm;
    }

    // 여러 명을 한 쿼리로. 하나라도 이 공연 참여자가 아니면 404
    public Map<Long, PerformanceMember> getMembers(Long performanceId, Collection<Long> ids) {
        Map<Long, PerformanceMember> found = em
                .createQuery(
                        "select pm from PerformanceMember pm where pm.performance.id = :pid and pm.id in :ids",
                        PerformanceMember.class)
                .setParameter("pid", performanceId)
                .setParameter("ids", ids)
                .getResultStream()
                .collect(Collectors.toMap(PerformanceMember::getId, Function.identity()));
        if (found.size() != Set.copyOf(ids).size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공연 참여자를 찾을 수 없습니다.");
        }
        return found;
    }

    public Part getActivePart(Long clubId, Long partId) {
        return getActiveParts(clubId, List.of(partId)).get(partId);
    }

    // 같은 동아리 파트인지는 서비스 검증(DB 미강제). 사용 중지 파트는 새로 선택 불가
    public Map<Long, Part> getActiveParts(Long clubId, Collection<Long> ids) {
        Map<Long, Part> found = em
                .createQuery("select p from Part p where p.club.id = :clubId and p.id in :ids", Part.class)
                .setParameter("clubId", clubId)
                .setParameter("ids", ids)
                .getResultStream()
                .collect(Collectors.toMap(Part::getId, Function.identity()));
        if (found.size() != Set.copyOf(ids).size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "파트를 찾을 수 없습니다.");
        }
        if (found.values().stream().anyMatch(p -> !p.isActive())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "사용 중지된 파트입니다.");
        }
        return found;
    }

    // 소속·사용자까지 한 번에 가져옴(N+1 방지)
    public List<PerformanceMember> getMembers(Long performanceId, List<PerformanceMemberStatus> statuses) {
        return em.createQuery(
                        "select pm from PerformanceMember pm join fetch pm.membership m join fetch m.user"
                                + " where pm.performance.id = :pid and pm.status in :statuses order by pm.id",
                        PerformanceMember.class)
                .setParameter("pid", performanceId)
                .setParameter("statuses", statuses)
                .getResultList();
    }

    public static String displayName(PerformanceMember pm) {
        Membership m = pm.getMembership();
        return m.getDisplayName() != null ? m.getDisplayName() : m.getUser().getName();
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, "공연을 찾을 수 없습니다.");
    }
}
