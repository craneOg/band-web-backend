package com.crane.bandwebbackend.domain.team.repository;

import com.crane.bandwebbackend.domain.team.entity.TeamMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByTeamIdAndPerformanceMemberId(Long teamId, Long performanceMemberId);

    List<TeamMember> findByPerformanceId(Long performanceId);
}
