package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerEventSummaryDTO;
import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.model.dto.PlayerStatsByEventDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventRepository;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ClubhouseService.Scope;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubhouseServiceTest {
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerTeamRepository playerTeamRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventTeamRepository eventTeamRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private StatsService statsService;
    @Mock
    private ClubhouseEvaluationService evaluationService;
    @Mock
    private ClubhouseMilestoneService milestoneService;
    @Mock
    private UserClubhouseConfigService configService;

    private ClubhouseService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new ClubhouseService(
                playerRepository,
                playerTeamRepository,
                teamRepository,
                eventRepository,
                eventTeamRepository,
                gameRepository,
                statsService,
                evaluationService,
                milestoneService,
                configService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("里程碑按需加载，使用当前用户配置")
    void milestonesLoadOnDemand() {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        Map<String, Object> config = ClubhouseMetricCatalog.defaultConfig();
        when(configService.getOrCreate(7L, 1L)).thenReturn(config);
        Map<String, Object> milestones = Map.of("available", true);
        when(milestoneService.build(10L, null, config)).thenReturn(milestones);

        assertEquals(milestones, service.getMilestones(7L));
    }

    @Test
    @DisplayName("未认领球员的账号访问驾驶舱返回 404")
    void unclaimedUserGets404() {
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getDashboard(99L, null, null));
    }

    @Test
    @DisplayName("已认领但无代表球队时返回默认配置与空数据")
    void claimedPlayerWithoutTeamsReturnsDefaults() {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        player.setName("林嘉豪");
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(10L))
                .thenReturn(List.of());
        when(teamRepository.findAllById(any())).thenReturn(List.of());
        when(configService.getOrCreate(7L, 1L)).thenReturn(ClubhouseMetricCatalog.defaultConfig());
        Map<String, Object> emptyStats = new LinkedHashMap<>();
        emptyStats.put("batting", new LinkedHashMap<>());
        emptyStats.put("pitching", new LinkedHashMap<>());
        emptyStats.put("fielding", new LinkedHashMap<>());
        when(statsService.getPlayerStats(10L, "BASEBALL")).thenReturn(emptyStats);
        when(statsService.getPlayerGameLogForScope(10L, null, null, 3, "BASEBALL", false)).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> playerView = (Map<String, Object>) result.get("player");
        assertEquals("林嘉豪", playerView.get("name"));
        assertTrue(((List<?>) result.get("teams")).isEmpty());
        assertNotNull(result.get("catalog"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> moduleCatalog = (List<Map<String, Object>>) result.get("moduleCatalog");
        assertEquals(9, moduleCatalog.size());
        assertEquals("ranks", moduleCatalog.get(0).get("key"));
        assertEquals("evaluation", moduleCatalog.get(1).get("key"));
        assertEquals("milestones", moduleCatalog.get(2).get("key"));
        assertEquals(12, moduleCatalog.get(2).get("span"));
        assertEquals(11, ((List<?>) result.get("milestoneCatalog")).size());
        assertTrue(result.containsKey("evaluation"));
        assertTrue(!result.containsKey("milestones"));
        assertTrue(((List<?>) result.get("ranks")).isEmpty());
        @SuppressWarnings("unchecked")
        Map<String, Object> performance = (Map<String, Object>) result.get("performance");
        assertEquals(false, performance.get("available"));
        assertTrue(((List<?>) result.get("schedule")).isEmpty());
    }

    @Test
    @DisplayName("默认范围为全部球队与生涯")
    void defaultScopeIsCareer() {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(10L))
                .thenReturn(List.of());
        when(teamRepository.findAllById(any())).thenReturn(List.of());
        when(configService.getOrCreate(7L, 1L)).thenReturn(ClubhouseMetricCatalog.defaultConfig());
        when(statsService.getPlayerStats(10L, "BASEBALL")).thenReturn(emptyStats());

        Map<String, Object> result = service.getDashboard(7L, null, null, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertNull(scope.get("teamId"));
        assertEquals("全部球队", scope.get("teamName"));
        assertNull(scope.get("eventId"));
        assertEquals("生涯", scope.get("eventName"));
        assertEquals("BASEBALL", scope.get("gameMode"));
        assertEquals(true, scope.get("teamAll"));
        assertEquals(true, scope.get("career"));
    }

    @Test
    @DisplayName("非法球队与赛事回落到全部球队与生涯")
    void invalidTeamAndEventFallBackToDefaults() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStats(10L, "BASEBALL")).thenReturn(emptyStats());

        Map<String, Object> result = service.getDashboard(7L, 999L, 999L, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertNull(scope.get("teamId"));
        assertEquals("全部球队", scope.get("teamName"));
        assertNull(scope.get("eventId"));
        assertEquals("生涯", scope.get("eventName"));
        assertEquals(true, scope.get("teamAll"));
        assertEquals(true, scope.get("career"));
    }

    @Test
    @DisplayName("只选赛事时球队仍为全部球队")
    void eventScopeKeepsAllTeams() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, 11L, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertNull(scope.get("teamId"));
        assertEquals("全部球队", scope.get("teamName"));
        assertEquals(11L, scope.get("eventId"));
        assertEquals("花城联赛", scope.get("eventName"));
        assertEquals(true, scope.get("teamAll"));
        assertEquals(false, scope.get("career"));
    }

    @Test
    @DisplayName("请求的运动类型优先于所选赛事")
    void requestGameModeOverridesEventGameMode() {
        stubSingleTeam("SOFTBALL");
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertEquals(11L, scope.get("eventId"));
        assertEquals("BASEBALL", scope.get("gameMode"));
    }

    @Test
    @DisplayName("未传运动类型时沿用所选赛事类型")
    void gameModeFallsBackToSelectedEvent() {
        stubSingleTeam("SOFTBALL");
        when(statsService.getPlayerStatsBySeason(10L, "SOFTBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, 11L);

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertEquals(11L, scope.get("eventId"));
        assertEquals("SOFTBALL", scope.get("gameMode"));
    }

    @Test
    @DisplayName("同时选择球队与赛事时统计按球队+赛事查询")
    void teamAndEventResolveTogether() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsByTeam(10L, 1L, 11L, "BASEBALL")).thenReturn(emptyStats());

        Map<String, Object> result = service.getDashboard(7L, 1L, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertEquals(1L, scope.get("teamId"));
        assertEquals("广州猎豹", scope.get("teamName"));
        assertEquals(11L, scope.get("eventId"));
        assertEquals("花城联赛", scope.get("eventName"));
        assertEquals("BASEBALL", scope.get("gameMode"));
        assertEquals(false, scope.get("teamAll"));
        assertEquals(false, scope.get("career"));
        verify(statsService).getPlayerStatsByTeam(10L, 1L, 11L, "BASEBALL");
    }

    @Test
    @DisplayName("所选赛事不属于所选球队时赛事回落到生涯，球队统计仍按球队")
    void eventNotOwnedByTeamFallsBackToCareer() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsByTeam(10L, 1L, null, "BASEBALL")).thenReturn(emptyStats());

        Map<String, Object> result = service.getDashboard(7L, 1L, 12L, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertEquals(1L, scope.get("teamId"));
        assertEquals("广州猎豹", scope.get("teamName"));
        assertNull(scope.get("eventId"));
        assertEquals("生涯", scope.get("eventName"));
        assertEquals(false, scope.get("teamAll"));
        assertEquals(true, scope.get("career"));
        verify(statsService).getPlayerStatsByTeam(10L, 1L, null, "BASEBALL");
    }

    @Test
    @DisplayName("非法球队回落到全部球队时赛事仍然生效")
    void invalidTeamKeepsValidEvent() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, 999L, 11L, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertNull(scope.get("teamId"));
        assertEquals("全部球队", scope.get("teamName"));
        assertEquals(11L, scope.get("eventId"));
        assertEquals("花城联赛", scope.get("eventName"));
        assertEquals(true, scope.get("teamAll"));
        assertEquals(false, scope.get("career"));
    }

    @Test
    @DisplayName("未知运动类型回落到所选赛事类型并用于统计查询")
    void unknownGameModeFallsBackToSelectedEvent() {
        stubSingleTeam("SOFTBALL");
        when(statsService.getPlayerStatsBySeason(10L, "SOFTBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "foo");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertEquals("SOFTBALL", scope.get("gameMode"));
        verify(statsService).getPlayerStatsBySeason(10L, "SOFTBALL");
    }

    @Test
    @DisplayName("小写带空白的运动类型归一为 SOFTBALL 并用于统计查询")
    void lowercaseGameModeIsNormalized() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsBySeason(10L, "SOFTBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "  softball ");

        @SuppressWarnings("unchecked")
        Map<String, Object> scope = (Map<String, Object>) result.get("scope");
        assertEquals("SOFTBALL", scope.get("gameMode"));
        verify(statsService).getPlayerStatsBySeason(10L, "SOFTBALL");
    }

    @Test
    @DisplayName("驾驶舱返回赛事下拉数据，球队聚合为数组且最后一场为日期")
    void dashboardIncludesEventSummaries() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL")).thenReturn(List.of());
        PlayerEventSummaryDTO summary = new PlayerEventSummaryDTO();
        summary.setEventId(11L);
        summary.setEventName("花城联赛");
        summary.setStatus("ongoing");
        summary.setStartDate("2026-03-01");
        summary.setEndDate("2026-11-30");
        summary.setGameMode("BASEBALL");
        summary.setTeamIds("1,2");
        summary.setTeamNames("猎豹,蓝鸟");
        summary.setTeamIdList(List.of(1L, 2L));
        summary.setTeamNameList(List.of("猎豹", "蓝鸟"));
        summary.setLastGameday("2026-10-20 00:00:00");
        summary.setGameCount(18);
        when(statsService.getPlayerEventSummaries(10L, null, "BASEBALL")).thenReturn(List.of(summary));

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
        assertEquals(1, events.size());
        assertEquals(11L, events.get(0).get("id"));
        assertEquals("花城联赛", events.get(0).get("name"));
        assertEquals(List.of(1L, 2L), events.get(0).get("teamIds"));
        assertEquals(List.of("猎豹", "蓝鸟"), events.get(0).get("teamNames"));
        assertEquals("2026-10-20", events.get(0).get("lastGameday"));
        assertEquals(18, events.get(0).get("gameCount"));
    }

    @Test
    @DisplayName("赛事下拉只保留当前代表球队已确认的赛事")
    void dashboardExcludesEventsOutsideConfirmedTeams() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStats(10L, "BASEBALL")).thenReturn(emptyStats());
        when(statsService.getPlayerEventSummaries(10L, null, "BASEBALL"))
                .thenReturn(List.of(eventSummary(11L, "花城联赛"), eventSummary(99L, "旧赛事")));

        Map<String, Object> result = service.getDashboard(7L, null, null, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
        assertEquals(1, events.size());
        assertEquals(11L, events.get(0).get("id"));
    }

    @Test
    @DisplayName("选择球队时赛事下拉只保留该队已确认的赛事")
    void dashboardExcludesEventsOutsideSelectedTeam() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsByTeam(10L, 1L, null, "BASEBALL")).thenReturn(emptyStats());
        when(statsService.getPlayerEventSummaries(10L, 1L, "BASEBALL"))
                .thenReturn(List.of(eventSummary(11L, "花城联赛"), eventSummary(99L, "旧赛事")));

        Map<String, Object> result = service.getDashboard(7L, 1L, null, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
        assertEquals(1, events.size());
        assertEquals(11L, events.get(0).get("id"));
    }

    @Test
    @DisplayName("球队+生涯使用跨赛事排行榜，且不因 eventId 为空丢弃排名")
    void careerTeamRankUsesEventlessLeaderboard() {
        Scope scope = new Scope(1L, "广州猎豹", null, null, "BASEBALL");
        ClubhouseMetricCatalog.MetricDef def = ClubhouseMetricCatalog.find("batting.avg");
        List<Map<String, Object>> rows = List.of(
                Map.of("playerId", 11L, "avg", ".400"),
                Map.of("playerId", 10L, "avg", ".300"));
        when(statsService.getBattingLeaders(isNull(), any(), any(), eq(List.of(1L)), any(), any(),
                any(), any(), any(), eq("BASEBALL"), anyInt(), anyInt(), any(), any(), eq(false)))
                .thenReturn(PageResult.of(rows, rows.size()));

        Map<String, Object> rank = service.rankOne(10L, scope, def, "BASEBALL", true);

        assertEquals(2, rank.get("rank"));
        assertEquals(2, rank.get("total"));
        assertEquals(".300", rank.get("value"));
    }

    @Test
    @DisplayName("球队+赛事仍使用该赛事的同队排行榜")
    void teamEventRankStillUsesEventLeaderboard() {
        Scope scope = new Scope(1L, "广州猎豹", 11L, "花城联赛", "BASEBALL");
        ClubhouseMetricCatalog.MetricDef def = ClubhouseMetricCatalog.find("batting.avg");
        List<Map<String, Object>> rows = List.of(Map.of("playerId", 10L, "avg", ".300"));
        when(statsService.getBattingLeaders(eq(11L), any(), any(), eq(List.of(1L)), any(), any(),
                any(), any(), any(), eq("BASEBALL"), anyInt(), anyInt(), any(), any(), eq(false)))
                .thenReturn(PageResult.of(rows, rows.size()));

        Map<String, Object> rank = service.rankOne(10L, scope, def, "BASEBALL", true);

        assertEquals(1, rank.get("rank"));
        assertEquals(1, rank.get("total"));
        assertEquals(".300", rank.get("value"));
    }

    @Test
    @DisplayName("最近战报按球队与赛事过滤后取 3 场")
    void recentGamesFollowScope() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsByTeam(10L, 1L, 11L, "BASEBALL")).thenReturn(emptyStats());
        PlayerGameLogEntryDTO log = gameLog(101L, 11L, 1L, "2026-10-20");
        when(statsService.getPlayerGameLogForScope(10L, 1L, 11L, 3, "BASEBALL", false))
                .thenReturn(List.of(log));

        Map<String, Object> result = service.getDashboard(7L, 1L, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = (List<Map<String, Object>>) result.get("recentGames");
        assertEquals(1, recent.size());
        assertEquals(101L, recent.get(0).get("gameId"));
        assertEquals(1L, recent.get(0).get("teamId"));
        assertEquals("广州猎豹", recent.get(0).get("teamName"));
        assertEquals("W", recent.get(0).get("result"));
        verify(statsService).getPlayerGameLogForScope(10L, 1L, 11L, 3, "BASEBALL", false);
    }

    @Test
    @DisplayName("表现走势按范围取排行比赛，保留最近 12 场")
    void performanceFollowsScope() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL"))
                .thenReturn(List.of(eventStats(11L)));
        when(statsService.getPlayerGameLogForScope(10L, null, 11L, 30, "BASEBALL", true))
                .thenReturn(battingLogs(13));

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> performance = (Map<String, Object>) result.get("performance");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) performance.get("points");
        assertEquals(12, points.size());
        assertEquals("10-02", points.get(0).get("label"));
        assertEquals("10-13", points.get(11).get("label"));
        verify(statsService).getPlayerGameLogForScope(10L, null, 11L, 30, "BASEBALL", true);
    }

    @Test
    @DisplayName("表现走势跳过无打击出场，不占用最近 12 场名额")
    void performanceSkipsGamesWithoutParticipation() {
        stubSingleTeam("BASEBALL");
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL"))
                .thenReturn(List.of(eventStats(11L)));
        List<PlayerGameLogEntryDTO> logs = new ArrayList<>();
        logs.add(emptyBattingLog(113L, "2026-10-13"));
        logs.addAll(battingLogs(12));
        when(statsService.getPlayerGameLogForScope(10L, null, 11L, 30, "BASEBALL", true))
                .thenReturn(logs);

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        Map<String, Object> performance = (Map<String, Object>) result.get("performance");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) performance.get("points");
        // 最新一场未打击被跳过，仍应有 12 个有效点（若先按 12 场截断则只剩 11 个）
        assertEquals(12, points.size());
        assertEquals("10-02", points.get(0).get("label"));
        assertEquals("10-13", points.get(11).get("label"));
    }

    @Test
    @DisplayName("全部球队选赛事时赛程包含该赛事所有代表球队")
    void scheduleIncludesAllTeamsOfSelectedEvent() {
        stubTwoTeamsSameEvent();
        when(statsService.getPlayerStatsBySeason(10L, "BASEBALL")).thenReturn(List.of());
        when(gameRepository.findForTeamsAndStatuses(1L, List.of(1L, 2L), List.of("scheduled", "live")))
                .thenReturn(List.of(
                        upcomingGame(201L, 11L, 1L, 2L, "2999-05-01"),
                        upcomingGame(202L, 11L, 2L, 3L, "2999-05-02"),
                        upcomingGame(203L, 12L, 1L, 2L, "2999-05-03"),
                        upcomingGame(204L, 11L, 1L, 2L, "2000-05-03")));

        Map<String, Object> result = service.getDashboard(7L, null, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedule = (List<Map<String, Object>>) result.get("schedule");
        assertEquals(List.of(201L, 202L),
                schedule.stream().map(row -> row.get("gameId")).toList());
        assertEquals(2L, schedule.get(1).get("teamId"));
        assertEquals("海珠蓝鸟", schedule.get(1).get("teamName"));
        assertEquals(201L, ((Map<?, ?>) result.get("nextGame")).get("gameId"));
    }

    @Test
    @DisplayName("赛程只保留与当前运动类型一致的赛事")
    void scheduleExcludesOtherGameMode() {
        stubTeamsWithMixedGameModes();
        when(statsService.getPlayerStats(10L, "BASEBALL")).thenReturn(emptyStats());
        when(gameRepository.findForTeamsAndStatuses(1L, List.of(1L, 2L), List.of("scheduled", "live")))
                .thenReturn(List.of(
                        upcomingGame(201L, 11L, 1L, 2L, "2999-05-01"),
                        upcomingGame(202L, 12L, 1L, 4L, "2999-05-02")));

        Map<String, Object> result = service.getDashboard(7L, null, null, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedule = (List<Map<String, Object>>) result.get("schedule");
        assertEquals(List.of(201L), schedule.stream().map(row -> row.get("gameId")).toList());
        assertEquals(201L, ((Map<?, ?>) result.get("nextGame")).get("gameId"));
    }

    @Test
    @DisplayName("选择球队时赛程只查该队比赛")
    void scheduleFiltersSelectedTeam() {
        stubTwoTeamsSameEvent();
        when(statsService.getPlayerStatsByTeam(10L, 1L, 11L, "BASEBALL")).thenReturn(emptyStats());
        when(gameRepository.findForTeamsAndStatuses(1L, List.of(1L), List.of("scheduled", "live")))
                .thenReturn(List.of(upcomingGame(201L, 11L, 1L, 2L, "2999-05-01")));

        Map<String, Object> result = service.getDashboard(7L, 1L, 11L, "BASEBALL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedule = (List<Map<String, Object>>) result.get("schedule");
        assertEquals(1, schedule.size());
        assertEquals(1L, schedule.get(0).get("teamId"));
        assertEquals("广州猎豹", schedule.get(0).get("teamName"));
        verify(gameRepository).findForTeamsAndStatuses(1L, List.of(1L), List.of("scheduled", "live"));
    }

    private PlayerGameLogEntryDTO gameLog(Long gameId, Long eventId, Long teamId, String gameday) {
        PlayerGameLogEntryDTO log = new PlayerGameLogEntryDTO();
        log.setGameId(gameId);
        log.setEventId(eventId);
        log.setGameday(gameday);
        log.setEventName("花城联赛");
        log.setHomeTeamId(1L);
        log.setAwayTeamId(2L);
        log.setHomeScore(5);
        log.setAwayScore(3);
        log.setTeamId(teamId);
        log.setTeamName("广州猎豹");
        log.setIsHome(true);
        log.setBatting(Map.of("ab", 3, "pa", 4, "avg", "0.333"));
        log.setPitching(new LinkedHashMap<>());
        log.setFielding(new LinkedHashMap<>());
        return log;
    }

    /** 按 SQL 返回顺序（新→旧）构造 count 场有打击数据的比赛，日期从 10-13 递减。 */
    private List<PlayerGameLogEntryDTO> battingLogs(int count) {
        List<PlayerGameLogEntryDTO> logs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int day = 13 - i;
            logs.add(gameLog(100L + day, 11L, 1L, String.format("2026-10-%02d", day)));
        }
        return logs;
    }

    /** 只上场未打击的比赛：ab/pa 均为 0，应被表现走势的参与口径过滤。 */
    private PlayerGameLogEntryDTO emptyBattingLog(Long gameId, String gameday) {
        PlayerGameLogEntryDTO log = gameLog(gameId, 11L, 1L, gameday);
        log.setBatting(Map.of("ab", 0, "pa", 0, "avg", "-"));
        return log;
    }

    private PlayerStatsByEventDTO eventStats(Long eventId) {
        PlayerStatsByEventDTO row = new PlayerStatsByEventDTO();
        row.setEventId(eventId);
        row.setEventName("花城联赛");
        row.setBatting(Map.of("ab", 3, "pa", 4, "avg", "0.333"));
        row.setPitching(new LinkedHashMap<>());
        row.setFielding(new LinkedHashMap<>());
        return row;
    }

    private Game upcomingGame(Long gameId, Long eventId, Long homeTeamId, Long awayTeamId, String gameday) {
        Game game = new Game();
        game.setId(gameId);
        game.setEventId(eventId);
        game.setTenantId(1L);
        game.setHomeTeamId(homeTeamId);
        game.setAwayTeamId(awayTeamId);
        game.setGameday(gameday);
        game.setStatus("scheduled");
        return game;
    }

    /** 两支代表球队都报名同一赛事，用于多球队赛事口径。 */
    private void stubTwoTeamsSameEvent() {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        PlayerTeam first = new PlayerTeam();
        first.setTeamId(1L);
        first.setCurrent(true);
        PlayerTeam second = new PlayerTeam();
        second.setTeamId(2L);
        second.setCurrent(true);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(10L))
                .thenReturn(List.of(first, second));
        Team team1 = new Team();
        team1.setId(1L);
        team1.setName("广州猎豹");
        Team team2 = new Team();
        team2.setId(2L);
        team2.setName("海珠蓝鸟");
        when(teamRepository.findAllById(any())).thenReturn(List.of(team1, team2));
        EventTeam firstRegistration = new EventTeam();
        firstRegistration.setEventId(11L);
        firstRegistration.setStatus(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(List.of(firstRegistration));
        EventTeam secondRegistration = new EventTeam();
        secondRegistration.setEventId(11L);
        secondRegistration.setStatus(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(2L))
                .thenReturn(List.of(secondRegistration));
        Event event = new Event();
        event.setId(11L);
        event.setTenantId(1L);
        event.setName("花城联赛");
        event.setStatus("ongoing");
        event.setStartDate("2026-03-01");
        event.setGameMode("BASEBALL");
        when(eventRepository.findAllById(any())).thenReturn(List.of(event));
        when(statsService.getStandings(11L, null, null, "BASEBALL", 1, 100))
                .thenReturn(PageResult.of(List.of(), 0L));
        when(configService.getOrCreate(7L, 1L)).thenReturn(ClubhouseMetricCatalog.defaultConfig());
    }

    /** 同一球员两支代表球队：赛事 11 为棒球，赛事 12 只有垒球，用于赛程运动类型过滤。 */
    private void stubTeamsWithMixedGameModes() {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        PlayerTeam first = new PlayerTeam();
        first.setTeamId(1L);
        first.setCurrent(true);
        PlayerTeam second = new PlayerTeam();
        second.setTeamId(2L);
        second.setCurrent(true);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(10L))
                .thenReturn(List.of(first, second));
        Team team1 = new Team();
        team1.setId(1L);
        team1.setName("广州猎豹");
        Team team2 = new Team();
        team2.setId(2L);
        team2.setName("海珠蓝鸟");
        when(teamRepository.findAllById(any())).thenReturn(List.of(team1, team2));
        EventTeam baseballRegistration = new EventTeam();
        baseballRegistration.setEventId(11L);
        baseballRegistration.setStatus(EventTeam.STATUS_CONFIRMED);
        EventTeam softballRegistration = new EventTeam();
        softballRegistration.setEventId(12L);
        softballRegistration.setStatus(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(List.of(softballRegistration, baseballRegistration));
        EventTeam blueBirdsRegistration = new EventTeam();
        blueBirdsRegistration.setEventId(11L);
        blueBirdsRegistration.setStatus(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(2L))
                .thenReturn(List.of(blueBirdsRegistration));
        Event baseball = new Event();
        baseball.setId(11L);
        baseball.setTenantId(1L);
        baseball.setName("花城联赛");
        baseball.setStatus("ongoing");
        baseball.setStartDate("2026-03-01");
        baseball.setGameMode("BASEBALL");
        Event softball = new Event();
        softball.setId(12L);
        softball.setTenantId(1L);
        softball.setName("垒球联赛");
        softball.setStatus("ended");
        softball.setStartDate("2026-04-01");
        softball.setGameMode("SOFTBALL");
        when(eventRepository.findAllById(any())).thenReturn(List.of(baseball, softball));
        when(statsService.getStandings(11L, null, null, "BASEBALL", 1, 100))
                .thenReturn(PageResult.of(List.of(), 0L));
        when(configService.getOrCreate(7L, 1L)).thenReturn(ClubhouseMetricCatalog.defaultConfig());
    }

    private PlayerEventSummaryDTO eventSummary(Long eventId, String eventName) {
        PlayerEventSummaryDTO summary = new PlayerEventSummaryDTO();
        summary.setEventId(eventId);
        summary.setEventName(eventName);
        summary.setGameMode("BASEBALL");
        summary.setTeamIds("1");
        summary.setTeamNames("广州猎豹");
        summary.setTeamIdList(List.of(1L));
        summary.setTeamNameList(List.of("广州猎豹"));
        summary.setLastGameday("2026-10-20");
        summary.setGameCount(1);
        return summary;
    }

    private void stubSingleTeam(String eventGameMode) {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        PlayerTeam entry = new PlayerTeam();
        entry.setTeamId(1L);
        entry.setCurrent(true);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(10L))
                .thenReturn(List.of(entry));
        Team team = new Team();
        team.setId(1L);
        team.setName("广州猎豹");
        when(teamRepository.findAllById(any())).thenReturn(List.of(team));
        EventTeam registration = new EventTeam();
        registration.setEventId(11L);
        registration.setStatus(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(List.of(registration));
        Event event = new Event();
        event.setId(11L);
        event.setTenantId(1L);
        event.setName("花城联赛");
        event.setStatus("ongoing");
        event.setStartDate("2026-03-01");
        event.setGameMode(eventGameMode);
        when(eventRepository.findAllById(any())).thenReturn(List.of(event));
        when(statsService.getStandings(11L, null, null, eventGameMode, 1, 100))
                .thenReturn(PageResult.of(List.of(), 0L));
        when(configService.getOrCreate(7L, 1L)).thenReturn(ClubhouseMetricCatalog.defaultConfig());
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("batting", new LinkedHashMap<>());
        out.put("pitching", new LinkedHashMap<>());
        out.put("fielding", new LinkedHashMap<>());
        return out;
    }
}
