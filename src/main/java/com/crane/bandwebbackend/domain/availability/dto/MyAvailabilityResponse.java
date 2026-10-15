package com.crane.bandwebbackend.domain.availability.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyAvailabilityResponse {

    private final LocalDate weekStart;
    private final List<OffsetDateTime> slots;
}
