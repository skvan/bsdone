package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.repository.TeamRepository;
import java.util.List;
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
}
