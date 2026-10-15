package com.crane.bandwebbackend.domain.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crane.bandwebbackend.global.error.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvailabilitySlotRulesTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Test
    @DisplayName("KST 슬롯을 UTC로 바꾸고 중복 제거·정렬")
    void convertsToUtc() {
        List<LocalDateTime> slots = AvailabilityService.toUtcSlots(
                MONDAY,
                List.of(
                        OffsetDateTime.parse("2026-10-12T19:30:00+09:00"),
                        OffsetDateTime.parse("2026-10-12T19:00:00+09:00"),
                        OffsetDateTime.parse("2026-10-12T10:00:00Z")),
                null,
                null);
        assertThat(slots)
                .containsExactly(LocalDateTime.parse("2026-10-12T10:00"), LocalDateTime.parse("2026-10-12T10:30"));
    }

    @Test
    @DisplayName("AV-02 30분 단위가 아니면 400")
    void rejectsUnaligned() {
        assertThatThrownBy(() -> AvailabilityService.toUtcSlots(
                        MONDAY, List.of(OffsetDateTime.parse("2026-10-12T19:15:00+09:00")), null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("AV-03 다른 주 슬롯이면 400 (KST 기준 주 경계)")
    void rejectsOutsideWeek() {
        // KST 다음 주 월요일 00:00 = UTC 일요일 15:00
        assertThatThrownBy(() -> AvailabilityService.toUtcSlots(
                        MONDAY, List.of(OffsetDateTime.parse("2026-10-18T15:00:00Z")), null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("합주 시즌 밖이면 422")
    void rejectsOutsideSeason() {
        assertThatThrownBy(() -> AvailabilityService.toUtcSlots(
                        MONDAY,
                        List.of(OffsetDateTime.parse("2026-10-12T19:00:00+09:00")),
                        LocalDate.of(2026, 10, 13),
                        null))
                .isInstanceOf(BusinessException.class);
    }
}
