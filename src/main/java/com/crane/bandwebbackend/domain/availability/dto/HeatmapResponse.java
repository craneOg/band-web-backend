package com.crane.bandwebbackend.domain.availability.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

// API-103 대체: 칸별 가능 인원 n/m(AVAIL-002). 가능 인원이 0인 칸은 생략
@Getter
@Builder
public class HeatmapResponse {

    private final LocalDate weekStart;
    // m: 승인된 참여자 수
    private final int totalMembers;
    // 아직 한 칸도 입력하지 않은 참여자 수(누락자가 있어도 히트맵은 그림)
    private final int notRespondedCount;
    private final List<Cell> cells;

    @Getter
    @Builder
    public static class Cell {

        private final OffsetDateTime slotStart;
        // n
        private final int availableCount;
        // 임원에게만 이름 목록, 부원에게는 NULL
        private final List<String> names;
    }
}
