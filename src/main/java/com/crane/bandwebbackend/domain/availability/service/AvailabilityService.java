package com.crane.bandwebbackend.domain.availability.service;

import com.crane.bandwebbackend.domain.availability.dto.AvailabilityRequest;
import com.crane.bandwebbackend.domain.availability.dto.HeatmapResponse;
import com.crane.bandwebbackend.domain.availability.dto.MyAvailabilityResponse;
import com.crane.bandwebbackend.domain.availability.entity.AvailableSlot;
import com.crane.bandwebbackend.domain.availability.repository.AvailableSlotRepository;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMember;
import com.crane.bandwebbackend.domain.participant.entity.PerformanceMemberStatus;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccess;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccessService;
import com.crane.bandwebbackend.domain.performance.entity.PerformancePolicy;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import com.crane.bandwebbackend.global.time.Kst;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AvailableSlotRepository slotRepository;
    private final PerformanceAccessService accessService;

    // 승인된 참여자 본인만, 대리 입력 불가
    @Transactional
    public MyAvailabilityResponse replaceMine(Long performanceId, Long userId, AvailabilityRequest req) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        access.requireEditable();
        PerformanceMember pm = access.requireApprovedParticipant();
        PerformancePolicy policy = accessService.getPolicy(performanceId);
        PerformanceAccess.requireOpen(policy.getAvailabilityState(), policy.getAvailabilityDeadline(), "가능 시간 입력");

        List<LocalDateTime> slots = toUtcSlots(
                req.getWeekStart(), req.getSlots(), policy.getPracticeSeasonStart(), policy.getPracticeSeasonEnd());
        LocalDateTime from = weekFromUtc(req.getWeekStart());
        slotRepository.deleteInRange(pm.getId(), from, from.plusDays(7));
        slotRepository.saveAll(slots.stream().map(s -> AvailableSlot.of(pm, s)).toList());
        return mine(req.getWeekStart(), slots);
    }

    public MyAvailabilityResponse getMine(Long performanceId, Long userId, LocalDate weekStart) {
        PerformanceMember pm = accessService.get(performanceId, userId).requireApprovedParticipant();
        requireMonday(weekStart);
        LocalDateTime from = weekFromUtc(weekStart);
        return mine(weekStart, slotRepository.findStartsInRange(pm.getId(), from, from.plusDays(7)));
    }

    // 칸별 n/m. 임원에게는 칸별 이름도
    public HeatmapResponse heatmap(Long performanceId, Long userId, LocalDate weekStart) {
        PerformanceAccess access = accessService.get(performanceId, userId);
        requireMonday(weekStart);
        List<PerformanceMember> members =
                accessService.getMembers(performanceId, List.of(PerformanceMemberStatus.APPROVED));
        Map<Long, String> names = members.stream()
                .collect(Collectors.toMap(PerformanceMember::getId, PerformanceAccessService::displayName));
        LocalDateTime from = weekFromUtc(weekStart);
        List<AvailableSlot> slots = members.isEmpty()
                ? List.of()
                : slotRepository.findByMembersInRange(List.copyOf(names.keySet()), from, from.plusDays(7));

        Map<LocalDateTime, List<Long>> bySlot = new TreeMap<>();
        slots.forEach(s -> bySlot.computeIfAbsent(s.getSlotStart(), k -> new ArrayList<>())
                .add(s.getPerformanceMember().getId()));
        long responded = slots.stream().map(s -> s.getPerformanceMember().getId()).distinct().count();

        List<HeatmapResponse.Cell> cells = bySlot.entrySet().stream()
                .map(e -> HeatmapResponse.Cell.builder()
                        .slotStart(Kst.fromUtc(e.getKey()))
                        .availableCount(e.getValue().size())
                        .names(access.isAdmin()
                                ? e.getValue().stream().map(names::get).sorted().toList()
                                : null)
                        .build())
                .toList();
        return HeatmapResponse.builder()
                .weekStart(weekStart)
                .totalMembers(members.size())
                .notRespondedCount(members.size() - (int) responded)
                .cells(cells)
                .build();
    }

    /**
     * 요청 슬롯 검증 후 UTC로 변환(중복 제거·정렬).
     * AV-02 KST 기준 30분 정렬, AV-03 해당 주 안, 합주 시즌(정책)이 있으면 그 날짜 안.
     */
    static List<LocalDateTime> toUtcSlots(
            LocalDate weekStart, List<OffsetDateTime> slots, LocalDate seasonStart, LocalDate seasonEnd) {
        requireMonday(weekStart);
        TreeSet<LocalDateTime> result = new TreeSet<>();
        for (OffsetDateTime slot : slots) {
            LocalDateTime kst = slot.atZoneSameInstant(Kst.OFFSET).toLocalDateTime();
            if (kst.getMinute() % 30 != 0 || kst.getSecond() != 0 || kst.getNano() != 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "가능 시간은 30분 단위로만 입력할 수 있습니다.");
            }
            LocalDate date = kst.toLocalDate();
            if (date.isBefore(weekStart) || !date.isBefore(weekStart.plusDays(7))) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 주에 속한 시간만 저장할 수 있습니다.");
            }
            if ((seasonStart != null && date.isBefore(seasonStart)) || (seasonEnd != null && date.isAfter(seasonEnd))) {
                throw new BusinessException(ErrorCode.RULE_VIOLATION, "합주 기간 밖의 시간은 입력할 수 없습니다.");
            }
            result.add(Kst.toUtc(kst));
        }
        return List.copyOf(result);
    }

    private static MyAvailabilityResponse mine(LocalDate weekStart, List<LocalDateTime> utcSlots) {
        return MyAvailabilityResponse.builder()
                .weekStart(weekStart)
                .slots(utcSlots.stream().map(Kst::fromUtc).filter(Objects::nonNull).toList())
                .build();
    }

    private static LocalDateTime weekFromUtc(LocalDate weekStart) {
        return Kst.toUtc(weekStart.atStartOfDay());
    }

    // ponytail: 주 시작 요일은 월요일로 가정. 화면 명세가 일요일이면 이 한 곳만 수정
    private static void requireMonday(LocalDate weekStart) {
        if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "weekStart는 월요일이어야 합니다.");
        }
    }
}
