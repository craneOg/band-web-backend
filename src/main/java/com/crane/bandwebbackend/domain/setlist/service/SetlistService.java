package com.crane.bandwebbackend.domain.setlist.service;

import com.crane.bandwebbackend.domain.participant.service.PerformanceAccess;
import com.crane.bandwebbackend.domain.participant.service.PerformanceAccessService;
import com.crane.bandwebbackend.domain.setlist.dto.SetlistItemResponse;
import com.crane.bandwebbackend.domain.setlist.dto.SetlistRequest;
import com.crane.bandwebbackend.domain.song.entity.Song;
import com.crane.bandwebbackend.domain.song.service.SongService;
import com.crane.bandwebbackend.global.error.BusinessException;
import com.crane.bandwebbackend.global.error.ErrorCode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetlistService {

    // 배정 순서대로, 미배정은 뒤에
    private static final Comparator<Song> ORDER = Comparator.comparing(
                    Song::getSetlistOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Song::getId);

    private final PerformanceAccessService accessService;
    private final SongService songService;

    public List<SetlistItemResponse> get(Long performanceId, Long userId) {
        accessService.get(performanceId, userId);
        return toResponse(songService.getConfirmedSongs(performanceId));
    }

    @Transactional
    public List<SetlistItemResponse> save(Long performanceId, Long userId, SetlistRequest req) {
        PerformanceAccess access = accessService.getForUpdate(performanceId, userId);
        access.requireAdmin();
        access.requireEditable();

        Map<Long, Song> confirmed = songService.getConfirmedSongs(performanceId).stream()
                .collect(Collectors.toMap(Song::getId, Function.identity()));
        // 낡은 화면에서 저장해 곡이 빠지거나 끼는 일을 막기 위해 확정 곡 전체를 받음
        Map<Song, Integer> orders = new HashMap<>();
        for (SetlistRequest.Item item : req.getItems()) {
            Song song = confirmed.get(item.getSongId());
            if (song == null || orders.containsKey(song)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "확정 곡 목록이 바뀌었습니다. 새로고침 후 다시 저장해 주세요.");
            }
            orders.put(song, item.getOrder());
        }
        if (orders.size() != confirmed.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "확정 곡 목록이 바뀌었습니다. 새로고침 후 다시 저장해 주세요.");
        }

        List<Integer> assigned = orders.values().stream().filter(Objects::nonNull).sorted().toList();
        for (int i = 0; i < assigned.size(); i++) {
            if (assigned.get(i) != i + 1) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "순서는 1부터 빠짐없이 지정해야 합니다.");
            }
        }
        // LOCKED 진입 시 1부터 재부여된 상태를 유지 → 미배정 불가
        if (access.isLocked() && assigned.size() != orders.size()) {
            throw new BusinessException(ErrorCode.RULE_VIOLATION, "확정된 공연에서는 모든 곡의 순서를 정해야 합니다.");
        }

        songService.applySetlistOrders(orders);
        return toResponse(List.copyOf(orders.keySet()));
    }

    private static List<SetlistItemResponse> toResponse(List<Song> songs) {
        return songs.stream().sorted(ORDER).map(SetlistItemResponse::from).toList();
    }
}
