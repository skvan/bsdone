package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.model.dto.PlayerStatsDTO;
import com.bsball.repository.TeamRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService：OBP/WHIP/UER/PENDING/守备展示公式")
class StatsServiceFormulaTest {

    @Mock
    private PlayerStatsMapper playerStatsMapper;
    @Mock
    private StatsLeadersMapper statsLeadersMapper;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private TeamRepository teamRepository;

    private StatsService service;

    @BeforeEach
    void setUp() {
        service = new StatsService(playerStatsMapper, statsLeadersMapper, tenantProperties, teamRepository);
        CurrentUserHolder.set(Long.valueOf(1L), Long.valueOf(1L));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("OBP 使用 AB+BB+HBP+SF 作分母，而不是 PA")
    void obpUsesStandardDenominator() {
        PlayerStatsDTO.Batting batting = new PlayerStatsDTO.Batting();
        batting.setAb(3);
        batting.setH(2);
        batting.setBb(1);
        batting.setPa(5);
        batting.setSh(1);

        givenPlayerStats(batting, new PlayerStatsDTO.Pitching(), new PlayerStatsDTO.Fielding());

        Map<String, Object> result = service.getPlayerStats(Long.valueOf(1L), "BASEBALL");
        Map<String, Object> out = battingMap(result);
        // 标准 OBP = (2+1)/(3+1+0+0) = 0.75；旧实现 (2+1)/PA(5) = 0.6
        assertEquals("0.75", out.get("obp"));
    }

    @Test
    @DisplayName("WHIP 使用 H+BB，不把 HBP 计入分子")
    void whipExcludesHitByPitch() {
        PlayerStatsDTO.Pitching pitching = new PlayerStatsDTO.Pitching();
        pitching.setIp(2.0);
        pitching.setEr(1);
        pitching.setPitchH(2);
        pitching.setPitchBb(1);
        pitching.setPitchHbp(1);
        pitching.setPitchBbHp(2);

        givenPlayerStats(new PlayerStatsDTO.Batting(), pitching, new PlayerStatsDTO.Fielding());

        Map<String, Object> result = service.getPlayerStats(Long.valueOf(1L), "BASEBALL");
        Map<String, Object> out = pitchingMap(result);
        // 标准 WHIP = (2+1)/2 = 1.5；旧实现 (2+2)/2 = 2
        assertEquals("1.5", out.get("whip"));
    }

    @Test
    @DisplayName("WHIP 旧数据仅有 BBHP 合计时回退到合计")
    void whipFallsBackToBbHpWhenSplitMissing() {
        PlayerStatsDTO.Pitching pitching = new PlayerStatsDTO.Pitching();
        pitching.setIp(3.0);
        pitching.setPitchH(3);
        pitching.setPitchBbHp(3);

        givenPlayerStats(new PlayerStatsDTO.Batting(), pitching, new PlayerStatsDTO.Fielding());

        Map<String, Object> result = service.getPlayerStats(Long.valueOf(1L), "BASEBALL");
        // 分项均为 0 时回退 BBHP 合计：(3+3)/3 = 2
        assertEquals("2", pitchingMap(result).get("whip"));
    }

    @Test
    @DisplayName("WHIP 分项存在时即使 BB=0 也不回退 BBHP")
    void whipDoesNotFallBackWhenSplitPresent() {
        PlayerStatsDTO.Pitching pitching = new PlayerStatsDTO.Pitching();
        pitching.setIp(3.0);
        pitching.setPitchH(3);
        pitching.setPitchHbp(1);
        pitching.setPitchBbHp(2);

        givenPlayerStats(new PlayerStatsDTO.Batting(), pitching, new PlayerStatsDTO.Fielding());

        Map<String, Object> result = service.getPlayerStats(Long.valueOf(1L), "BASEBALL");
        // BB+HBP 分项存在则 HBP 不计入：(3+0)/3 = 1
        assertEquals("1", pitchingMap(result).get("whip"));
    }

    @Test
    @DisplayName("UER/PENDING 与守备 DP/捕手 SB 进入球员统计响应")
    void exposesUnearnedPendingAndFieldingExtras() {
        PlayerStatsDTO.Pitching pitching = new PlayerStatsDTO.Pitching();
        pitching.setIp(1.0);
        pitching.setEr(1);
        pitching.setUnearnedR(2);
        pitching.setPendingR(3);
        PlayerStatsDTO.Fielding fielding = new PlayerStatsDTO.Fielding();
        fielding.setTc(5);
        fielding.setPo(2);
        fielding.setA(2);
        fielding.setE(1);
        fielding.setDp(2);
        fielding.setPb(1);
        fielding.setCatcherCs(1);
        fielding.setCatcherSb(4);

        givenPlayerStats(new PlayerStatsDTO.Batting(), pitching, fielding);

        Map<String, Object> result = service.getPlayerStats(Long.valueOf(1L), "BASEBALL");
        assertEquals(2, pitchingMap(result).get("unearnedR"));
        assertEquals(3, pitchingMap(result).get("pendingR"));
        assertEquals(2, fieldingMap(result).get("dp"));
        assertEquals(1, fieldingMap(result).get("catcherCs"));
        assertEquals(4, fieldingMap(result).get("catcherSb"));
    }

    @Test
    @DisplayName("团队投手 ERA=0 且 IP>0 时显示 0，而不是 -")
    void teamPitchingZeroEraDisplaysZero() {
        when(statsLeadersMapper.countTeamPitchingLeaders(
                any(), any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(statsLeadersMapper.selectTeamPitchingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), anyString(), anyInt(), anyLong()))
                .thenReturn(List.of(Map.of(
                        "teamId", 1L,
                        "teamName", "测试队",
                        "ip", 12.0,
                        "era", 0.0,
                        "whip", 0.0
                )));

        Map<String, Object> row = service.getTeamPitchingLeaders(
                null, null, null, null, null, null, 1, 20, null, null)
                .getList().get(0);

        assertEquals("0", row.get("era"));
        assertEquals("0", row.get("whip"));
    }

    private void givenPlayerStats(PlayerStatsDTO.Batting batting, PlayerStatsDTO.Pitching pitching,
            PlayerStatsDTO.Fielding fielding) {
        when(playerStatsMapper.selectBattingStats(any(), any(), any())).thenReturn(batting);
        when(playerStatsMapper.selectPitchingStats(any(), any(), any())).thenReturn(pitching);
        when(playerStatsMapper.selectFieldingStats(any(), any(), any())).thenReturn(fielding);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> battingMap(Map<String, Object> result) {
        return (Map<String, Object>) result.get("batting");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pitchingMap(Map<String, Object> result) {
        return (Map<String, Object>) result.get("pitching");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fieldingMap(Map<String, Object> result) {
        return (Map<String, Object>) result.get("fielding");
    }
}
