/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.Game
 *  com.bsball.repository.GameRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository
extends JpaRepository<Game, Long>,
JpaSpecificationExecutor<Game> {

    /**
     * 统计该球队作为主/客队的“未开打/未完成”比赛数（解散守卫用）。
     * 口径：status 非 {@code final}（已结束）/ {@code cancelled}（取消）；空/未知 status 保守按未开打算。
     * 仅计未软删的比赛（{@code deletedAt is null}）。
     */
    @Query(value="select count(g) from Game g where g.deletedAt is null and (g.homeTeamId = :teamId or g.awayTeamId = :teamId) and coalesce(g.status, 'scheduled') not in ('final', 'cancelled')")
    public long countPendingGamesByTeamId(@Param(value="teamId") Long var1);
}

