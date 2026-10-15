package com.crane.bandwebbackend.global.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// 저장은 UTC LocalDateTime, 요청·응답은 +09:00
public final class Kst {

    public static final ZoneOffset OFFSET = ZoneOffset.ofHours(9);

    private Kst() {}

    public static OffsetDateTime fromUtc(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).withOffsetSameInstant(OFFSET);
    }

    public static LocalDateTime toUtc(OffsetDateTime time) {
        return time.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime toUtc(LocalDateTime kst) {
        return kst.atOffset(OFFSET).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
