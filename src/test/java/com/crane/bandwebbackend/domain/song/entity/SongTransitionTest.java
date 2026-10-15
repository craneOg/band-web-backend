package com.crane.bandwebbackend.domain.song.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crane.bandwebbackend.domain.team.entity.Team;
import com.crane.bandwebbackend.domain.team.entity.TeamSource;
import com.crane.bandwebbackend.global.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SongTransitionTest {

    private final Team team = Team.createApproved(null, "1팀", TeamSource.ADMIN, null);

    private Song proposed() {
        return Song.propose(null, null, "곡", null, null, null, null);
    }

    @Test
    @DisplayName("X-05 확정은 담당 팀·순서·사유를 한 번에 기록")
    void confirm() {
        Song song = proposed();
        song.confirm(team, 3, "투표 1위");

        assertThat(song.getStatus()).isEqualTo(SongStatus.CONFIRMED);
        assertThat(song.getAssignedTeam()).isSameAs(team);
        assertThat(song.getSetlistOrder()).isEqualTo(3);
        assertThat(song.getDecisionNote()).isEqualTo("투표 1위");
        assertThat(song.getDecidedAt()).isNotNull();
    }

    @Test
    @DisplayName("팀 범위 후보 확정 시 담당 팀 기본값 = 후보 팀")
    void confirmDefaultsToCandidateTeam() {
        Song song = Song.propose(null, team, "곡", null, null, null, null);
        song.confirm(null, null, null);
        assertThat(song.getAssignedTeam()).isSameAs(team);
    }

    @Test
    @DisplayName("SONG-003 9-4 CONFIRMED → REJECTED 한 번에, 담당 팀·순서 해제")
    void rejectConfirmed() {
        Song song = proposed();
        song.confirm(team, 1, null);
        song.reject("편성 불가");

        assertThat(song.getStatus()).isEqualTo(SongStatus.REJECTED);
        assertThat(song.getAssignedTeam()).isNull();
        assertThat(song.getSetlistOrder()).isNull();
    }

    @Test
    @DisplayName("SONG-003 9-5 재검토 REJECTED → PROPOSED")
    void reopen() {
        Song song = proposed();
        song.reject(null);
        song.reopen("다시 보기");
        assertThat(song.getStatus()).isEqualTo(SongStatus.PROPOSED);
    }

    @Test
    @DisplayName("재검토는 REJECTED에서만 409")
    void reopenOnlyFromRejected() {
        assertThatThrownBy(() -> proposed().reopen(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("S-11 철회·임원 삭제는 PROPOSED에서만, withdrawalType 기록")
    void withdraw() {
        Song song = proposed();
        song.withdraw(WithdrawalType.ADMIN);
        assertThat(song.getStatus()).isEqualTo(SongStatus.WITHDRAWN);
        assertThat(song.getWithdrawalType()).isEqualTo(WithdrawalType.ADMIN);

        Song confirmed = proposed();
        confirmed.confirm(team, null, null);
        assertThatThrownBy(() -> confirmed.withdraw(WithdrawalType.PROPOSER)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("세트리스트 순서는 CONFIRMED만, 미배정(NULL)은 언제나 허용")
    void setlistOrderOnlyConfirmed() {
        Song song = proposed();
        song.changeSetlistOrder(null);
        assertThatThrownBy(() -> song.changeSetlistOrder(1)).isInstanceOf(BusinessException.class);
    }
}
