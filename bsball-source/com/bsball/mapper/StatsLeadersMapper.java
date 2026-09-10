/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.mapper.StatsLeadersMapper
 *  com.bsball.model.dto.BattingLeaderDTO
 *  com.bsball.model.dto.FieldingLeaderDTO
 *  com.bsball.model.dto.PitchingLeaderDTO
 *  com.bsball.model.dto.StandingGameRowDTO
 *  org.apache.ibatis.annotations.Mapper
 *  org.apache.ibatis.annotations.Param
 */
package com.bsball.mapper;

import com.bsball.model.dto.BattingLeaderDTO;
import com.bsball.model.dto.FieldingLeaderDTO;
import com.bsball.model.dto.PitchingLeaderDTO;
import com.bsball.model.dto.StandingGameRowDTO;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatsLeadersMapper {
    public long countBattingLeaders(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="playerKeyword") String var6, @Param(value="positionCode") String var7, @Param(value="homeAway") String var8, @Param(value="batterHand") String var9, @Param(value="pitcherHand") String var10, @Param(value="gameMode") String var11, @Param(value="applyQualification") Boolean var12);

    public List<BattingLeaderDTO> selectBattingLeadersPage(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="playerKeyword") String var6, @Param(value="positionCode") String var7, @Param(value="homeAway") String var8, @Param(value="batterHand") String var9, @Param(value="pitcherHand") String var10, @Param(value="gameMode") String var11, @Param(value="applyQualification") Boolean var12, @Param(value="orderBySql") String var13, @Param(value="limit") int var14, @Param(value="offset") long var15);

    public long countPitchingLeaders(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="playerKeyword") String var6, @Param(value="positionCode") String var7, @Param(value="homeAway") String var8, @Param(value="batterHand") String var9, @Param(value="pitcherHand") String var10, @Param(value="gameMode") String var11, @Param(value="applyQualification") Boolean var12);

    public List<PitchingLeaderDTO> selectPitchingLeadersPage(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="playerKeyword") String var6, @Param(value="positionCode") String var7, @Param(value="homeAway") String var8, @Param(value="batterHand") String var9, @Param(value="pitcherHand") String var10, @Param(value="gameMode") String var11, @Param(value="applyQualification") Boolean var12, @Param(value="orderBySql") String var13, @Param(value="limit") int var14, @Param(value="offset") long var15);

    public long countFieldingLeaders(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="playerKeyword") String var6, @Param(value="positionCode") String var7, @Param(value="homeAway") String var8, @Param(value="batterHand") String var9, @Param(value="pitcherHand") String var10, @Param(value="gameMode") String var11);

    public List<FieldingLeaderDTO> selectFieldingLeadersPage(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="playerKeyword") String var6, @Param(value="positionCode") String var7, @Param(value="homeAway") String var8, @Param(value="batterHand") String var9, @Param(value="pitcherHand") String var10, @Param(value="gameMode") String var11, @Param(value="orderBySql") String var12, @Param(value="limit") int var13, @Param(value="offset") long var14);

    public List<StandingGameRowDTO> selectStandingGames(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="gameMode") String var5);

    public long countTeamBattingLeaders(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="homeAway") String var6, @Param(value="gameMode") String var7);

    public List<Map<String, Object>> selectTeamBattingLeadersPage(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="homeAway") String var6, @Param(value="gameMode") String var7, @Param(value="orderBySql") String var8, @Param(value="limit") int var9, @Param(value="offset") long var10);

    public long countTeamPitchingLeaders(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="homeAway") String var6, @Param(value="gameMode") String var7);

    public List<Map<String, Object>> selectTeamPitchingLeadersPage(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="homeAway") String var6, @Param(value="gameMode") String var7, @Param(value="orderBySql") String var8, @Param(value="limit") int var9, @Param(value="offset") long var10);

    public long countTeamFieldingLeaders(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="homeAway") String var6, @Param(value="gameMode") String var7);

    public List<Map<String, Object>> selectTeamFieldingLeadersPage(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="teamIds") List<Long> var5, @Param(value="homeAway") String var6, @Param(value="gameMode") String var7, @Param(value="orderBySql") String var8, @Param(value="limit") int var9, @Param(value="offset") long var10);

    public int selectMaxTeamScheduledGames(@Param(value="tenantId") Long var1, @Param(value="eventId") Long var2, @Param(value="eventIds") List<Long> var3, @Param(value="years") List<String> var4, @Param(value="gameMode") String var5);
}

