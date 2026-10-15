package com.crane.bandwebbackend.domain.setlist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// API-088 세트리스트 저장: 확정 곡 전체를 {songId, order|null}로. order NULL = 미배정(SET-001 "빼기")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SetlistRequest {

    @NotNull
    @Valid
    private List<Item> items;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Item {

        @NotNull
        private Long songId;

        @Positive
        private Integer order;
    }
}
