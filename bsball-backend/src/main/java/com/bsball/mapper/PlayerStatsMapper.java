/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.mapper.PlayerStatsMapper
 *  com.bsball.model.dto.EventStatRow
 *  com.bsball.model.dto.PlayerStatsDTO$Batting
 *  com.bsball.model.dto.PlayerStatsDTO$Fielding
 *  com.bsball.model.dto.PlayerStatsDTO$Pitching
 *  org.apache.ibatis.annotations.Mapper
 *  org.apache.ibatis.annotations.Param
 */
package com.bsball.mapper;

import com.bsball.model.dto.EventStatRow;
import com.bsball.model.dto.PlayerEventSummaryDTO;
import com.bsball.model.dto.PlayerStatsDTO;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlayerStatsMapper {
    public PlayerStatsDTO.Batting selectBattingStats(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public PlayerStatsDTO.Pitching selectPitchingStats(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public PlayerStatsDTO.Fielding selectFieldingStats(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public PlayerStatsDTO.Batting selectBattingStatsByTeamScope(@Param(value="playerId") Long playerId, @Param(value="tenantId") Long tenantId, @Param(value="teamId") Long teamId, @Param(value="eventId") Long eventId, @Param(value="gameMode") String gameMode);

    public PlayerStatsDTO.Pitching selectPitchingStatsByTeamScope(@Param(value="playerId") Long playerId, @Param(value="tenantId") Long tenantId, @Param(value="teamId") Long teamId, @Param(value="eventId") Long eventId, @Param(value="gameMode") String gameMode);

    public PlayerStatsDTO.Fielding selectFieldingStatsByTeamScope(@Param(value="playerId") Long playerId, @Param(value="tenantId") Long tenantId, @Param(value="teamId") Long teamId, @Param(value="eventId") Long eventId, @Param(value="gameMode") String gameMode);

    public List<EventStatRow> selectBattingStatsByEvent(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public List<EventStatRow> selectPitchingStatsByEvent(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public List<EventStatRow> selectFieldingStatsByEvent(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public List<Map<String, Object>> selectTeamNamesByPlayerEvents(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="gameMode") String var4);

    public List<PlayerEventSummaryDTO> selectPlayerEventSummaries(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="teamId") Long var3, @Param(value="gameMode") String var4);

    public List<Map<String, Object>> selectGameLog(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="limit") Integer var3, @Param(value="gameMode") String var4, @Param(value="rankingOnly") boolean var5);

    public List<Map<String, Object>> selectGameLogForScope(@Param(value="playerId") Long playerId, @Param(value="tenantId") Long tenantId, @Param(value="teamId") Long teamId, @Param(value="eventId") Long eventId, @Param(value="limit") Integer limit, @Param(value="gameMode") String gameMode, @Param(value="rankingOnly") boolean rankingOnly);

    public long countBattingDrillDown(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="gameMode") String var6);

    public List<Map<String, Object>> selectBattingDrillDownPage(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="valueSql") String var6, @Param(value="limit") int var7, @Param(value="offset") long var8, @Param(value="gameMode") String var10);

    public long countPitchingDrillDown(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="gameMode") String var6);

    public List<Map<String, Object>> selectPitchingDrillDownPage(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="valueSql") String var6, @Param(value="limit") int var7, @Param(value="offset") long var8, @Param(value="gameMode") String var10);

    public long countFieldingDrillDown(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="gameMode") String var6);

    public List<Map<String, Object>> selectFieldingDrillDownPage(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="valueSql") String var6, @Param(value="limit") int var7, @Param(value="offset") long var8, @Param(value="gameMode") String var10);
}

