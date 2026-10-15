package com.crane.bandwebbackend.domain.playability.dto;

import com.crane.bandwebbackend.domain.playability.entity.PlayabilityAnswer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayabilityRequest {

    @NotNull
    private Long partId;

    @NotNull
    private PlayabilityAnswer answer;

    // 불가능·보류 사유
    @Size(max = 500)
    private String reason;
}
