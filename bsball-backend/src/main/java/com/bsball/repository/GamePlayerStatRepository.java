/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.GamePlayerStat
 *  com.bsball.repository.GamePlayerStatRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.GamePlayerStat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GamePlayerStatRepository
extends JpaRepository<GamePlayerStat, Long> {
    public List<GamePlayerStat> findByGameId(Long var1);

    public List<GamePlayerStat> findByPlayerId(Long var1);

    public void deleteByGameId(Long var1);

    /**
     * 统计某“球员+球队”的有效比赛场次（口径与 PlayerStatsMapper 一致）：
     * JOIN bs_game / bs_event 并排除软删（game.deleted_at / event.deleted_at 为 NULL）；
     * 结果 = 该球员身披该球队出场的去重比赛数（{@code COUNT(DISTINCT s.game_id)}）。
     */
    @Query(value = "SELECT COUNT(DISTINCT s.game_id) FROM bs_game_player_stat s "
            + "INNER JOIN bs_game g ON s.game_id = g.id "
            + "INNER JOIN bs_event e ON g.event_id = e.id "
            + "WHERE s.player_id = :playerId AND s.team_id = :teamId "
            + "AND g.deleted_at IS NULL AND e.deleted_at IS NULL", nativeQuery = true)
    public long countValidByPlayerIdAndTeamId(@Param(value = "playerId") Long playerId,
            @Param(value = "teamId") Long teamId);
}

