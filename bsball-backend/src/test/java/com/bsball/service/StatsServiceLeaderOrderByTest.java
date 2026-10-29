package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.repository.TeamRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService：投手/守备榜必须使用各自排序器")
class StatsServiceLeaderOrderByTest {

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
    @DisplayName("投手榜默认按 ERA 排序，不得回退到打击 avg")
    void pitchingLeadersUsePitchingOrderBy() {
        givenEmptyPitchingPage();

        service.getPitchingLeaders(null, null, null, null, null, null, null, null, null, null,
                1, 20, null, null);

        String orderBy = capturedPitchingOrderBy();
        assertTrue(orderBy.contains("q.era"), orderBy);
        assertFalse(orderBy.contains("q.avg"), orderBy);
    }

    @Test
    @DisplayName("投手榜支持 UER/PENDING 新增排序列")
    void pitchingLeadersSortByUnearnedAndPendingRuns() {
        givenEmptyPitchingPage();

        service.getPitchingLeaders(null, null, null, null, null, null, null, null, null, null,
                1, 20, "pendingR", "desc");

        String orderBy = capturedPitchingOrderBy();
        assertTrue(orderBy.contains("q.pendingR DESC"), orderBy);
        assertFalse(orderBy.contains("q.era"), orderBy);
    }

    @Test
    @DisplayName("守备榜默认按 TC 排序，不得回退到打击 avg")
    void fieldingLeadersUseFieldingOrderBy() {
        givenEmptyFieldingPage();

        service.getFieldingLeaders(null, null, null, null, null, null, null, null, null, null,
                1, 20, null, null);

        String orderBy = capturedFieldingOrderBy();
        assertTrue(orderBy.contains("q.tc"), orderBy);
        assertFalse(orderBy.contains("q.avg"), orderBy);
    }

    @Test
    @DisplayName("球员守备榜支持捕手 SB/CS/PB 新增排序列")
    void fieldingLeadersSortByCatcherSb() {
        givenEmptyFieldingPage();

        service.getFieldingLeaders(null, null, null, null, null, null, null, null, null, null,
                1, 20, "catcherSb", "desc");

        String orderBy = capturedFieldingOrderBy();
        assertTrue(orderBy.contains("q.catcherSb DESC"), orderBy);
        assertFalse(orderBy.contains("q.tcPct"), orderBy);
    }

    @Test
    @DisplayName("球队守备榜支持 catcherSb 新增排序列")
    void teamFieldingLeadersSortByCatcherSb() {
        when(statsLeadersMapper.countTeamFieldingLeaders(
                any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(statsLeadersMapper.selectTeamFieldingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), anyString(), anyInt(), anyLong()))
                .thenReturn(List.of());

        service.getTeamFieldingLeaders(null, null, null, null, null, null, 1, 20, "catcherSb", "desc");

        ArgumentCaptor<String> orderBy = ArgumentCaptor.forClass(String.class);
        verify(statsLeadersMapper).selectTeamFieldingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), orderBy.capture(), anyInt(), anyLong());
        assertTrue(orderBy.getValue().contains("q.catcherSb DESC"), orderBy.getValue());
        assertFalse(orderBy.getValue().contains("q.tcPct"), orderBy.getValue());
    }

    private void givenEmptyPitchingPage() {
        when(statsLeadersMapper.countPitchingLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(statsLeadersMapper.selectPitchingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of());
        when(statsLeadersMapper.selectMaxTeamScheduledGames(
                any(), any(), any(), any(), any())).thenReturn(0);
    }

    private String capturedPitchingOrderBy() {
        ArgumentCaptor<String> orderBy = ArgumentCaptor.forClass(String.class);
        verify(statsLeadersMapper).selectPitchingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                orderBy.capture(), anyInt(), anyLong());
        return orderBy.getValue();
    }

    private void givenEmptyFieldingPage() {
        when(statsLeadersMapper.countFieldingLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);
        when(statsLeadersMapper.selectFieldingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of());
    }

    private String capturedFieldingOrderBy() {
        ArgumentCaptor<String> orderBy = ArgumentCaptor.forClass(String.class);
        verify(statsLeadersMapper).selectFieldingLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                orderBy.capture(), anyInt(), anyLong());
        return orderBy.getValue();
    }

    @Test
    @DisplayName("按位置守备榜默认按 INN（outs）降序，不回退球员守备/打击排序")
    void fieldingPositionsDefaultSortByInnings() {
        when(statsLeadersMapper.countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(statsLeadersMapper.selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of());

        service.getFieldingPositionLeaders(null, null, null, null, null, null, null, null, 9, 1, 20, null, null);

        ArgumentCaptor<String> orderBy = ArgumentCaptor.forClass(String.class);
        verify(statsLeadersMapper).selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                orderBy.capture(), anyInt(), anyLong());
        assertTrue(orderBy.getValue().contains("q.outs DESC"), orderBy.getValue());
        assertFalse(orderBy.getValue().contains("q.tc"), orderBy.getValue());
    }

    @Test
    @DisplayName("按位置守备榜：outs 换算棒球局数并保留位置字段")
    void fieldingPositionsMapOutsToInnings() {
        Map<String, Object> row = new HashMap<>();
        row.put("playerId", Long.valueOf(108L));
        row.put("playerName", "中外野");
        row.put("teamId", Long.valueOf(1L));
        row.put("teamName", "主队");
        row.put("position", "CF");
        row.put("gp", Integer.valueOf(5));
        row.put("gs", Integer.valueOf(4));
        row.put("outs", Integer.valueOf(10));
        when(statsLeadersMapper.countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(statsLeadersMapper.selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of(row));

        Map<String, Object> out = service.getFieldingPositionLeaders(null, null, null, null, "中外", "CF", null, null, null, 1, 20, null, null).getList().get(0);

        assertEquals("CF", out.get("position"));
        assertEquals(5, out.get("gp"));
        assertEquals(4, out.get("gs"));
        assertEquals(3.1D, ((Number)out.get("inn")).doubleValue(), 0.0001D);
    }

    @Test
    @DisplayName("按位置守备榜：minOuts 缺省 9，并回传 meta.minOuts")
    void fieldingPositionsDefaultMinOuts() {
        when(statsLeadersMapper.countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(statsLeadersMapper.selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of());

        PageResult<Map<String, Object>> page =
                service.getFieldingPositionLeaders(null, null, null, null, null, null, null, null, null, 1, 20, null, null);

        ArgumentCaptor<Integer> minOuts = ArgumentCaptor.forClass(Integer.class);
        verify(statsLeadersMapper).countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), minOuts.capture());
        assertEquals(9, minOuts.getValue());
        assertEquals(9, page.getMeta().get("minOuts"));
    }

    @Test
    @DisplayName("按位置守备榜：minOuts 越界钳制到 0..999，非法排序回落局数降序")
    void fieldingPositionsClampMinOutsAndRejectUnknownSort() {
        when(statsLeadersMapper.countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(statsLeadersMapper.selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of());

        service.getFieldingPositionLeaders(null, null, null, null, null, null, null, null, -5, 1, 20, "avg", "asc");
        service.getFieldingPositionLeaders(null, null, null, null, null, null, null, null, 5000, 1, 20, null, null);

        ArgumentCaptor<Integer> minOuts = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> orderBy = ArgumentCaptor.forClass(String.class);
        verify(statsLeadersMapper, times(2)).countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), minOuts.capture());
        verify(statsLeadersMapper, times(2)).selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                orderBy.capture(), anyInt(), anyLong());
        assertEquals(List.of(0, 999), minOuts.getAllValues());
        assertTrue(orderBy.getAllValues().get(0).contains("q.outs DESC"), orderBy.getAllValues().get(0));
        assertTrue(orderBy.getAllValues().get(1).contains("q.outs DESC"), orderBy.getAllValues().get(1));
    }

    @Test
    @DisplayName("按位置守备榜：homeAway 归一化后传给 Mapper")
    void fieldingPositionsNormalizeHomeAway() {
        when(statsLeadersMapper.countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(statsLeadersMapper.selectFieldingPositionLeadersPage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyString(), anyInt(), anyLong())).thenReturn(List.of());

        service.getFieldingPositionLeaders(null, null, null, null, null, "CF", "AWAY", null, null, 1, 20, null, null);

        ArgumentCaptor<String> homeAway = ArgumentCaptor.forClass(String.class);
        verify(statsLeadersMapper).countFieldingPositionLeaders(
                any(), any(), any(), any(), any(), any(), any(), homeAway.capture(), any(), any());
        assertEquals("away", homeAway.getValue());
    }
}
