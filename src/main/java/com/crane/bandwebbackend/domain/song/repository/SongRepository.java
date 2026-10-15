package com.crane.bandwebbackend.domain.song.repository;

import com.crane.bandwebbackend.domain.song.entity.Song;
import com.crane.bandwebbackend.domain.song.entity.SongStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findByIdAndPerformanceId(Long id, Long performanceId);

    List<Song> findByPerformanceIdOrderById(Long performanceId);

    List<Song> findByPerformanceIdAndStatusInOrderById(Long performanceId, Collection<SongStatus> statuses);

    List<Song> findByPerformanceIdAndStatus(Long performanceId, SongStatus status);

    // S-03: WITHDRAWN은 1인 한도에서 제외
    long countByPerformanceIdAndProposedByIdAndStatusNot(Long performanceId, Long membershipId, SongStatus status);

    boolean existsByPerformanceIdAndSetlistOrder(Long performanceId, Integer setlistOrder);

    @Query("select coalesce(max(s.setlistOrder), 0) from Song s where s.performance.id = :performanceId")
    int findMaxSetlistOrder(Long performanceId);
}
