package com.crane.bandwebbackend.global.error;

import lombok.Builder;
import lombok.Getter;

// ponytail: 공통 오류 본문 모양은 API 명세 공통 계약이 정해지면 그에 맞춰 교체
@Getter
@Builder
public class ErrorResponse {

    private final String code;
    private final String message;
}
