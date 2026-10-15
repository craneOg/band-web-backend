package com.crane.bandwebbackend.domain.playability.repository;

import com.crane.bandwebbackend.domain.playability.entity.SongPlayability;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongPlayabilityRepository extends JpaRepository<SongPlayability, Long> {

    Optional<SongPlayability> findBySongIdAndPerformanceMemberIdAndPartId(
            Long songId, Long performanceMemberId, Long partId);

    List<SongPlayability> findByPerformanceId(Long performanceId);
}
