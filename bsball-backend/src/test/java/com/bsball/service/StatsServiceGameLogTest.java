package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.repository.TeamRepository;
import java.util.LinkedHashMap;
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
class StatsServiceGameLogTest {
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
        CurrentUserHolder.set(1L, 1L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("里程碑日志读取不传 LIMIT，保留全部比赛")
    void milestoneLogReadsWithoutLimit() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("gameId", 88L);
        row.put("eventId", 9L);
        row.put("gameday", "2026-05-01");
        row.put("eventName", "花城联盟联赛");
        row.put("homeTeamId", 1L);
        row.put("awayTeamId", 2L);
        row.put("teamId", 1L);
        row.put("teamName", "广州猎豹");
        when(playerStatsMapper.selectGameLog(1L, 1L, null, "BASEBALL", true))
                .thenReturn(List.of(row));

        List<PlayerGameLogEntryDTO> logs = service.getPlayerGameLogForMilestones(1L, "BASEBALL");

        assertEquals(1, logs.size());
        assertEquals(88L, logs.get(0).getGameId());
        verify(playerStatsMapper).selectGameLog(eq(1L), eq(1L), isNull(), eq("BASEBALL"), eq(true));
    }
}
