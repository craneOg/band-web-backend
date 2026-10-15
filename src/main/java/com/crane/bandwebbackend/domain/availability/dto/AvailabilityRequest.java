package com.crane.bandwebbackend.domain.availability.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// API-101 가능 시간 저장: 한 주(월요일 시작, KST) 단위 교체(AV-03). slots = 30분 슬롯 시작 시각(AV-02)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityRequest {

    @NotNull
    private LocalDate weekStart;

    // 빈 목록 = 그 주 전부 비우기
    @NotNull
    private List<@NotNull OffsetDateTime> slots;
}
