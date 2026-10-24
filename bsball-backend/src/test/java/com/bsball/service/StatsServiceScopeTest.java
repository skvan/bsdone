package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.model.dto.PlayerEventSummaryDTO;
import com.bsball.repository.TeamRepository;
import java.util.List;
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
    @DisplayName("最后一场为空的赛事排在最后")
    void eventSummariesPlaceMissingLastGamedayLast() {
        when(playerStatsMapper.selectPlayerEventSummaries(10L, 1L, null, "BASEBALL"))
                .thenReturn(List.of(
                        eventRow(21L, "未来杯", null, "1", "猎豹"),
                        eventRow(22L, "城市杯", "2026-09-18", "1", "猎豹")));

        List<PlayerEventSummaryDTO> rows = statsService.getPlayerEventSummaries(10L, null, "BASEBALL");

        assertEquals(List.of(22L, 21L), rows.stream().map(PlayerEventSummaryDTO::getEventId).toList());
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
