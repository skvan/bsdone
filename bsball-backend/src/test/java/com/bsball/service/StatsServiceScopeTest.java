package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.model.dto.PlayerEventSummaryDTO;
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
class StatsServiceScopeTest {
    @Mock
    private PlayerStatsMapper playerStatsMapper;
    @Mock
    private StatsLeadersMapper statsLeadersMapper;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private TeamRepository teamRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(playerStatsMapper, statsLeadersMapper, tenantProperties, teamRepository);
        CurrentUserHolder.set(7L, 1L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("赛事摘要按球员最后一场倒序，多队赛事合并")
    void eventSummariesSortByLastGameday() {
        when(playerStatsMapper.selectPlayerEventSummaries(10L, 1L, 1L, "BASEBALL"))
                .thenReturn(List.of(
                        eventRow(12L, "城市杯", "2026-09-18", "1", "猎豹"),
                        eventRow(11L, "花城联赛", "2026-10-20", "1,2", "猎豹,蓝鸟")));

        List<PlayerEventSummaryDTO> rows = statsService.getPlayerEventSummaries(10L, 1L, "BASEBALL");

        assertEquals(List.of(11L, 12L), rows.stream().map(PlayerEventSummaryDTO::getEventId).toList());
        assertEquals("1,2", rows.get(0).getTeamIds());
        assertEquals(List.of(1L, 2L), rows.get(0).getTeamIdList());
        assertEquals(List.of("猎豹", "蓝鸟"), rows.get(0).getTeamNameList());
    }

    @Test
    @DisplayName("赛事球队 ID 与名称按同一球队顺序配对，不按文本或名称各自排序")
    void eventTeamIdsAndNamesPairByTeamIdOrder() {
        when(playerStatsMapper.selectPlayerEventSummaries(10L, 1L, null, "BASEBALL"))
                .thenReturn(List.of(
                        eventRow(31L, "城市杯", "2026-09-18", "2,10", "猎豹,斑马")));

        List<PlayerEventSummaryDTO> rows = statsService.getPlayerEventSummaries(10L, null, "BASEBALL");

        List<Long> ids = rows.get(0).getTeamIdList();
        List<String> names = rows.get(0).getTeamNameList();
        assertEquals(List.of(2L, 10L), ids);
        assertEquals("猎豹", names.get(ids.indexOf(2L)));
        assertEquals("斑马", names.get(ids.indexOf(10L)));
    }

    @Test
    @DisplayName("最后一场为空的赛事排在最后")
    void eventSummariesPlaceMissingLastGamedayLast() {
        when(playerStatsMapper.selectPlayerEventSummaries(10L, 1L, null, "BASEBALL"))
                .thenReturn(List.of(
                        eventRow(21L, "未来杯", null, "1", "猎豹"),
                        eventRow(22L, "城市杯", "2026-09-18", "1", "猎豹")));

        List<PlayerEventSummaryDTO> rows = statsService.getPlayerEventSummaries(10L, null, "BASEBALL");

        assertEquals(List.of(22L, 21L), rows.stream().map(PlayerEventSummaryDTO::getEventId).toList());
    }

    @Test
    @DisplayName("球队+赛事统计使用球队过滤，不拿赛事合计代替")
    void teamEventStatsUseTeamFilter() {
        when(playerStatsMapper.selectBattingStatsByTeamScope(10L, 1L, 2L, 3L, "BASEBALL"))
                .thenReturn(batting());
        when(playerStatsMapper.selectPitchingStatsByTeamScope(10L, 1L, 2L, 3L, "BASEBALL"))
                .thenReturn(pitching());
        when(playerStatsMapper.selectFieldingStatsByTeamScope(10L, 1L, 2L, 3L, "BASEBALL"))
                .thenReturn(fielding());

        Map<String, Object> stats = statsService.getPlayerStatsByTeam(10L, 2L, 3L, "BASEBALL");

        verify(playerStatsMapper).selectBattingStatsByTeamScope(10L, 1L, 2L, 3L, "BASEBALL");
        verify(playerStatsMapper).selectPitchingStatsByTeamScope(10L, 1L, 2L, 3L, "BASEBALL");
        verify(playerStatsMapper).selectFieldingStatsByTeamScope(10L, 1L, 2L, 3L, "BASEBALL");
        assertTeamStatsFormatted(stats);
    }

    @Test
    @DisplayName("球队生涯统计 eventId 传空，只按球队过滤")
    void teamCareerStatsPassNullEventId() {
        when(playerStatsMapper.selectBattingStatsByTeamScope(10L, 1L, 2L, null, "BASEBALL"))
                .thenReturn(batting());
        when(playerStatsMapper.selectPitchingStatsByTeamScope(10L, 1L, 2L, null, "BASEBALL"))
                .thenReturn(pitching());
        when(playerStatsMapper.selectFieldingStatsByTeamScope(10L, 1L, 2L, null, "BASEBALL"))
                .thenReturn(fielding());

        Map<String, Object> stats = statsService.getPlayerStatsByTeam(10L, 2L, null, "BASEBALL");

        verify(playerStatsMapper).selectBattingStatsByTeamScope(10L, 1L, 2L, null, "BASEBALL");
        verify(playerStatsMapper).selectPitchingStatsByTeamScope(10L, 1L, 2L, null, "BASEBALL");
        verify(playerStatsMapper).selectFieldingStatsByTeamScope(10L, 1L, 2L, null, "BASEBALL");
        assertTeamStatsFormatted(stats);
    }

    @SuppressWarnings("unchecked")
    private void assertTeamStatsFormatted(Map<String, Object> stats) {
        Map<String, Object> batting = (Map<String, Object>) stats.get("batting");
        Map<String, Object> pitching = (Map<String, Object>) stats.get("pitching");
        Map<String, Object> fielding = (Map<String, Object>) stats.get("fielding");

        assertEquals(5, batting.get("gp"));
        assertEquals("0.4", batting.get("avg"));
        assertEquals(2, pitching.get("gp"));
        assertEquals(4.0, pitching.get("ip"));
        assertEquals("6.75", pitching.get("era"));
        assertEquals(4, fielding.get("gp"));
        assertEquals(9.0, fielding.get("inn"));
        assertEquals("90.0%", fielding.get("tcPct"));
    }

    private PlayerStatsDTO.Batting batting() {
        PlayerStatsDTO.Batting b = new PlayerStatsDTO.Batting();
        b.setGp(5);
        b.setPa(12);
        b.setAb(10);
        b.setH(4);
        return b;
    }

    private PlayerStatsDTO.Pitching pitching() {
        PlayerStatsDTO.Pitching p = new PlayerStatsDTO.Pitching();
        p.setGp(2);
        p.setIp(4.0);
        p.setEr(3);
        p.setPitchH(4);
        p.setPitchBbHp(5);
        p.setPitchSo(3);
        return p;
    }

    private PlayerStatsDTO.Fielding fielding() {
        PlayerStatsDTO.Fielding f = new PlayerStatsDTO.Fielding();
        f.setGp(4);
        f.setInn(9.0);
        f.setTc(10);
        f.setPo(8);
        f.setA(1);
        f.setE(1);
        return f;
    }

    private PlayerEventSummaryDTO eventRow(Long id, String name, String lastGameday,
            String teamIds, String teamNames) {
        PlayerEventSummaryDTO row = new PlayerEventSummaryDTO();
        row.setEventId(id);
        row.setEventName(name);
        row.setLastGameday(lastGameday);
        row.setTeamIds(teamIds);
        row.setTeamNames(teamNames);
        row.setGameCount(1);
        return row;
    }
}
