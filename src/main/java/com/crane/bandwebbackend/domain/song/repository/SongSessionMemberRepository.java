package com.crane.bandwebbackend.domain.song.repository;

import com.crane.bandwebbackend.domain.song.entity.SongSessionMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SongSessionMemberRepository extends JpaRepository<SongSessionMember, Long> {

    List<SongSessionMember> findBySongIdOrderById(Long songId);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from SongSessionMember m where m.song.id = :songId")
    void deleteBySongId(Long songId);
}
