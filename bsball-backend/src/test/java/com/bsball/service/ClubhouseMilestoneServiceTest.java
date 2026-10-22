package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.model.entity.Team;
import com.bsball.repository.TeamRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubhouseMilestoneServiceTest {
    @Mock
    private StatsService statsService;
    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ClubhouseMilestoneService service;

    @Test
    @DisplayName("累计、下一目标与最近时间线使用同一批日志")
    void buildsCareerMilestonesAndTimeline() {
        Team home = team(1L, "广州猎豹");
        Team away = team(2L, "珠江飞鹰");
        when(teamRepository.findAllById(any())).thenReturn(List.of(home, away));
        when(statsService.getPlayerGameLogForMilestones(10L, "BASEBALL"))
                .thenReturn(List.of(
                        log(3L, "2026-05-01", 0, 0, 0, 1.0, 3, 0),
                        log(2L, "2026-04-01", 2, 0, 1, 4.0, 4, 0),
                        log(1L, "2026-03-01", 1, 1, 2, 5.0, 8, 1)));

        Map<String, Object> result = service.build(10L, "BASEBALL", config(
                "batting.h", "batting.hr", "batting.rbi", "pitching.ip", "pitching.so", "pitching.w"));

        assertEquals(true, result.get("available"));
        assertEquals(6, result.get("reachedCount"));
        Map<String, Object> hits = item(result, "batting.h");
        assertEquals("3", hits.get("value"));
        assertEquals(25, hits.get("nextTarget"));
        assertEquals("22", hits.get("remaining"));
        assertEquals(12, hits.get("progressPct"));
        Map<String, Object> innings = item(result, "pitching.ip");
        assertEquals("10", innings.get("value"));
        assertEquals(25, innings.get("nextTarget"));
        assertEquals("15", innings.get("remaining"));
        assertEquals(40, innings.get("progressPct"));
        Map<String, Object> wins = item(result, "pitching.w");
        assertEquals(5, wins.get("nextTarget"));
        assertEquals(20, wins.get("progressPct"));

        List<Map<String, Object>> timeline = timeline(result);
        assertEquals(5, timeline.size());
        assertEquals("2026-05-01", timeline.get(0).get("gameday"));
        assertEquals("珠江飞鹰", timeline.get(0).get("opponentName"));
    }

    @Test
    @DisplayName("投球局数按棒球记法展示，剩余进度按出局数换算")
    void formatsInningsInBaseballNotation() {
        when(teamRepository.findAllById(any())).thenReturn(List.of(team(1L, "广州猎豹"), team(2L, "珠江飞鹰")));
        when(statsService.getPlayerGameLogForMilestones(10L, "BASEBALL"))
                .thenReturn(List.of(log(1L, "2026-05-01", 0, 0, 0, 5.2, 0, 0)));

        Map<String, Object> result = service.build(10L, "BASEBALL", config("pitching.ip"));
        Map<String, Object> innings = item(result, "pitching.ip");

        assertEquals("5.2", innings.get("value"));
        assertEquals(10, innings.get("nextTarget"));
        assertEquals("4.1", innings.get("remaining"));
        assertEquals(57, innings.get("progressPct"));
    }

    @Test
    @DisplayName("首安使用专属里程碑标题")
    void firstHitUsesSpecialTitle() {
        when(teamRepository.findAllById(any())).thenReturn(List.of(team(1L, "广州猎豹"), team(2L, "珠江飞鹰")));
        when(statsService.getPlayerGameLogForMilestones(10L, "BASEBALL"))
                .thenReturn(List.of(log(1L, "2026-05-01", 1, 0, 0, 0, 0, 0)));

        Map<String, Object> result = service.build(10L, "BASEBALL", config("batting.h"));

        assertEquals("生涯首安", timeline(result).get(0).get("title"));
        assertEquals(1, result.get("reachedCount"));
    }

    @Test
    @DisplayName("投球局数按出局数累计，精确跨过整数阈值")
    void inningsThresholdUsesOuts() {
        when(teamRepository.findAllById(any())).thenReturn(List.of(team(1L, "广州猎豹"), team(2L, "珠江飞鹰")));
        when(statsService.getPlayerGameLogForMilestones(10L, null))
                .thenReturn(List.of(
                        log(1L, "2026-04-01", 0, 0, 0, 1.2, 0, 0),
                        log(2L, "2026-04-02", 0, 0, 0, 1.2, 0, 0),
                        log(3L, "2026-04-03", 0, 0, 0, 1.2, 0, 0),
                        log(4L, "2026-04-04", 0, 0, 0, 1.2, 0, 0),
                        log(5L, "2026-04-05", 0, 0, 0, 1.2, 0, 0),
                        log(6L, "2026-04-06", 0, 0, 0, 1.2, 0, 0)));

        Map<String, Object> result = service.build(10L, null, config("pitching.ip"));
        Map<String, Object> innings = item(result, "pitching.ip");

        assertEquals("10", innings.get("value"));
        assertEquals("15", innings.get("remaining"));
        assertEquals(40, innings.get("progressPct"));
        assertEquals(1, result.get("reachedCount"));
        assertEquals("达成 10 投球局", timeline(result).get(0).get("title"));
    }

    @Test
    @DisplayName("同一场比赛的多条日志只计一次出赛")
    void gamesCountDistinctGameIds() {
        when(teamRepository.findAllById(any())).thenReturn(List.of(team(1L, "广州猎豹"), team(2L, "珠江飞鹰")));
        when(statsService.getPlayerGameLogForMilestones(10L, null))
                .thenReturn(List.of(
                        log(7L, "2026-05-01", 1, 0, 1, 0, 0, 0),
                        log(7L, "2026-05-01", 1, 0, 1, 0, 0, 0)));

        Map<String, Object> result = service.build(10L, null, config("batting.gp"));
        Map<String, Object> games = item(result, "batting.gp");

        assertEquals("1", games.get("value"));
        assertEquals(10, games.get("progressPct"));
    }

    @Test
    @DisplayName("没有比赛日志时返回不可用空态")
    void emptyLogsReturnsUnavailable() {
        when(statsService.getPlayerGameLogForMilestones(10L, "BASEBALL")).thenReturn(List.of());

        Map<String, Object> result = service.build(10L, "BASEBALL", config("batting.h"));

        assertFalse((Boolean) result.get("available"));
        assertEquals(0, result.get("reachedCount"));
        assertTrue(((List<?>) result.get("items")).isEmpty());
        assertTrue(((List<?>) result.get("timeline")).isEmpty());
    }

    private PlayerGameLogEntryDTO log(Long gameId, String gameday, int hits, int homeRuns, int rbi,
            double innings, int strikeouts, int wins) {
        PlayerGameLogEntryDTO dto = new PlayerGameLogEntryDTO();
        dto.setGameId(gameId);
        dto.setGameday(gameday);
        dto.setEventName("花城联盟联赛");
        dto.setHomeTeamId(1L);
        dto.setAwayTeamId(2L);
        dto.setTeamId(1L);
        dto.setTeamName("广州猎豹");
        Map<String, Object> batting = new LinkedHashMap<>();
        batting.put("h", hits);
        batting.put("hr", homeRuns);
        batting.put("insideParkHr", 0);
        batting.put("rbi", rbi);
        batting.put("r", 0);
        batting.put("sb", 0);
        Map<String, Object> pitching = new LinkedHashMap<>();
        pitching.put("ip", innings);
        pitching.put("pitchSo", strikeouts);
        pitching.put("w", wins);
        pitching.put("sv", 0);
        pitching.put("np", 0);
        dto.setBatting(batting);
        dto.setPitching(pitching);
        return dto;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private Map<String, Object> config(String... keys) {
        return Map.of("milestones", Map.of("metricKeys", List.of(keys)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> item(Map<String, Object> result, String key) {
        return ((List<Map<String, Object>>) result.get("items")).stream()
                .filter(item -> key.equals(item.get("metricKey")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> timeline(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("timeline");
    }
}
