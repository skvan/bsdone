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
import java.util.Collection;
import java.util.List;
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

    /** 超管资产视图（spec §6.10）：平台已归还（platform_owned=true 且已软删）的比赛计数。 */
    public long countByPlatformOwnedTrueAndDeletedAtIsNotNull();

    /**
     * 赛事结束校验（Issue #318 决策 7）：该赛事下“未结束”的未删比赛计数。
     * 口径与 {@link #countPendingGamesByTeamId} 一致：终态 = {@code final}（已结束）/ {@code cancelled}（取消）；
     * 空/未知 status 保守按未结束计。
     */
    @Query(value="select count(g) from Game g where g.eventId = :eventId and g.deletedAt is null and coalesce(g.status, 'scheduled') not in ('final', 'cancelled')")
    public long countUnfinishedGamesByEvent(@Param(value="eventId") Long eventId);

    /**
     * 本队“已开赛”比赛计数（撤销报名守卫，Issue #310 延伸）：
     * 口径 = status ∈ {live（进行中）, final（已结束）}；仅计未软删比赛。
     */
    @Query(value="select count(g) from Game g where g.deletedAt is null and g.eventId = :eventId and (g.homeTeamId = :teamId or g.awayTeamId = :teamId) and g.status in ('live', 'final')")
    public long countStartedGamesByEventAndTeam(@Param(value="eventId") Long eventId, @Param(value="teamId") Long teamId);

    /**
     * 个人驾驶舱跨赛事赛程：本人代表球队的未结束比赛（scheduled/live），按日期时间升序。
     * 只按租户 + 球队集合取数，避免逐赛事查询。
     */
    @Query(value="select g from Game g where g.deletedAt is null and g.tenantId = :tenantId and (g.homeTeamId in :teamIds or g.awayTeamId in :teamIds) and coalesce(g.status, 'scheduled') in :statuses order by g.gameday asc, g.gameTime asc, g.id asc")
    public List<Game> findForTeamsAndStatuses(@Param(value="tenantId") Long tenantId, @Param(value="teamIds") Collection<Long> teamIds, @Param(value="statuses") Collection<String> statuses);

    /** 批量口径（球队端列表 canWithdraw 标记）：本队在给定赛事中已开赛的赛事 ID 集（防逐行查询）。 */
    @Query(value="select distinct g.eventId from Game g where g.deletedAt is null and g.eventId in :eventIds and (g.homeTeamId = :teamId or g.awayTeamId = :teamId) and g.status in ('live', 'final')")
    public List<Long> findStartedEventIdsByEventIdsAndTeamId(@Param(value="eventIds") Collection<Long> eventIds, @Param(value="teamId") Long teamId);
}

