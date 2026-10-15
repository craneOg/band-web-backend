package com.crane.bandwebbackend.domain.setlist.dto;

import com.crane.bandwebbackend.domain.song.entity.Song;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SetlistItemResponse {

    private final Long songId;
    private final String title;
    private final String artist;
    private final Long assignedTeamId;
    // NULL = 미배정
    private final Integer order;
    private final Long version;

    public static SetlistItemResponse from(Song song) {
        return SetlistItemResponse.builder()
                .songId(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .assignedTeamId(song.getAssignedTeam() == null ? null : song.getAssignedTeam().getId())
                .order(song.getSetlistOrder())
                .version(song.getVersion())
                .build();
    }
}
