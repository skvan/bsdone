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

    public List<EventStatRow> selectBattingStatsByEvent(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public List<EventStatRow> selectPitchingStatsByEvent(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public List<EventStatRow> selectFieldingStatsByEvent(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="gameMode") String var3);

    public List<Map<String, Object>> selectTeamNamesByPlayerEvents(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="gameMode") String var4);

    public List<Map<String, Object>> selectGameLog(@Param(value="playerId") Long var1, @Param(value="tenantId") Long var2, @Param(value="limit") int var3, @Param(value="gameMode") String var4);

    public long countBattingDrillDown(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="gameMode") String var6);

    public List<Map<String, Object>> selectBattingDrillDownPage(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="valueSql") String var6, @Param(value="limit") int var7, @Param(value="offset") long var8, @Param(value="gameMode") String var10);

    public long countPitchingDrillDown(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="gameMode") String var6);

    public List<Map<String, Object>> selectPitchingDrillDownPage(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="valueSql") String var6, @Param(value="limit") int var7, @Param(value="offset") long var8, @Param(value="gameMode") String var10);

    public long countFieldingDrillDown(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="gameMode") String var6);

    public List<Map<String, Object>> selectFieldingDrillDownPage(@Param(value="playerId") Long var1, @Param(value="tenantId") long var2, @Param(value="eventId") Long var4, @Param(value="season") String var5, @Param(value="valueSql") String var6, @Param(value="limit") int var7, @Param(value="offset") long var8, @Param(value="gameMode") String var10);
}

