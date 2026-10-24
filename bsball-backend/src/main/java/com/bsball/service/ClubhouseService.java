package com.bsball.service;

import com.bsball.common.json.PositionsJsonUtil;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 个人驾驶舱数据聚合（模块化布局 v3-modular）。
 * <p>数据范围：顶部数据按所选赛事（未选则全部赛事）聚合；队内排名必须落到
 * 「代表球队 + 赛事」；赛程跨全部代表球队与赛事。服务端只接受白名单指标与模块。</p>
 */
@Service
@RequiredArgsConstructor
public class ClubhouseService {
    private static final String DEFAULT_GAME_MODE = "BASEBALL";
    private static final int RANK_PAGE_SIZE = 100;
    private static final int MAX_PERFORMANCE_POINTS = 12;
    private static final int MAX_SCHEDULE = 6;
    private static final int MAX_RECENT = 3;
    private static final List<String> UPCOMING_STATUSES = List.of("scheduled", "live");

    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamRepository teamRepository;
    private final EventRepository eventRepository;
    private final EventTeamRepository eventTeamRepository;
    private final GameRepository gameRepository;
    private final StatsService statsService;
    private final ClubhouseEvaluationService evaluationService;
    private final ClubhouseMilestoneService milestoneService;
    private final UserClubhouseConfigService configService;

    /** 配置保存与读取统一使用球员所属租户，避免跨租户浏览时的租户口径分叉。 */
    public Long playerTenantId(Long userId) {
        return findPlayer(userId).getTenantId();
    }

    public Map<String, Object> getDashboard(Long userId, Long teamId, Long eventId) {
        return getDashboard(userId, teamId, eventId, null);
    }

    /**
     * 驾驶舱首屏聚合。
     *
     * @param userId      当前登录用户
     * @param teamId      代表球队；为空表示全部球队
     * @param eventId     赛事；为空表示生涯
     * @param requestedGameMode 运动类型；为空时取所选赛事，再回落到棒球
     */
    public Map<String, Object> getDashboard(Long userId, Long teamId, Long eventId, String requestedGameMode) {
        Player player = findPlayer(userId);
        Long tenantId = player.getTenantId();
        List<PlayerTeam> entries = currentEntries(player.getId());
        List<Map<String, Object>> teams = buildTeams(entries, tenantId);
        Scope scope = resolveScope(teams, teamId, eventId, requestedGameMode);
        Map<String, Object> config = configService.getOrCreate(userId, tenantId);
        String gameMode = scope.gameMode();
        List<PlayerEventSummaryDTO> eventSummaries =
                statsService.getPlayerEventSummaries(player.getId(), scope.teamId(), gameMode);
        Map<String, Object> stats = loadStats(player.getId(), scope.eventId(), gameMode);
        boolean battingAvailable = hasBatting(stats);
        boolean pitchingAvailable = hasPitching(stats);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("player", playerView(player, entries));
        out.put("teams", teams);
        out.put("scope", scopeView(scope));
        out.put("events", eventViews(eventSummaries));
        out.put("config", config);
        out.put("stats", stats);
        out.put("catalog", ClubhouseMetricCatalog.catalog(battingAvailable, pitchingAvailable));
        out.put("moduleCatalog", ClubhouseModuleCatalog.catalog());
        out.put("milestoneCatalog", ClubhouseMilestoneCatalog.catalog());
        out.put("performance", performance(player.getId(), scope, config, stats, gameMode));
        out.put("ranks", ranks(player.getId(), scope, config, battingAvailable, pitchingAvailable, gameMode));
        out.put("evaluation", evaluationService.build(
                player.getId(), scope.teamId(), scope.eventId(), gameMode, stats));
        List<Map<String, Object>> schedule = schedule(tenantId, teams);
        out.put("nextGame", schedule.isEmpty() ? null : schedule.get(0));
        out.put("schedule", schedule);
        out.put("recentGames", recentGames(player, gameMode));
        return out;
    }

    /** 生涯里程碑按需加载；与顶部赛事/球队范围无关，也不参与驾驶舱首屏聚合。 */
    public Map<String, Object> getMilestones(Long userId) {
        Player player = findPlayer(userId);
        Map<String, Object> config = configService.getOrCreate(userId, player.getTenantId());
        return milestoneService.build(player.getId(), null, config);
    }

    private Player findPlayer(Long userId) {
        Long tenantId = CurrentUserHolder.getTenantId();
        Player player = tenantId == null
                ? playerRepository.findFirstByUserIdAndDeletedAtIsNull(userId).orElse(null)
                : playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(userId, tenantId).orElse(null);
        if (player == null) {
            throw new BusinessException(404, "当前账号未关联球员档案");
        }
        return player;
    }

    private List<PlayerTeam> currentEntries(Long playerId) {
        return playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(playerId)
                .stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getCurrent()))
                .toList();
    }

    private List<Map<String, Object>> buildTeams(List<PlayerTeam> entries, Long tenantId) {
        if (entries.isEmpty()) {
            return List.of();
        }
        List<Long> teamIds = entries.stream().map(PlayerTeam::getTeamId).toList();
        Map<Long, Team> teams = teamRepository.findAllById(teamIds).stream()
                .collect(java.util.stream.Collectors.toMap(Team::getId, team -> team, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>();
        for (PlayerTeam entry : entries) {
            Team team = teams.get(entry.getTeamId());
            if (team == null || team.getDeletedAt() != null) {
                continue;
            }
            List<Event> events = confirmedEvents(team.getId(), tenantId);
            Event primary = events.isEmpty() ? null : events.get(0);
            Map<String, Object> standing = primary == null
                    ? Map.of()
                    : standing(primary.getId(), team.getId(), gameMode(primary));

            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", team.getId());
            row.put("name", team.getName());
            row.put("shortName", team.getShortName());
            row.put("logo", team.getLogo());
            row.put("leagueId", team.getLeagueId());
            row.put("number", entry.getNumber());
            row.put("positions", PositionsJsonUtil.parseList(entry.getPositions()));
            row.put("events", events.stream().map(this::eventView).toList());
            row.put("primaryEventId", primary == null ? null : primary.getId());
            row.put("primaryEventName", primary == null ? null : primary.getName());
            row.put("gameMode", primary == null ? DEFAULT_GAME_MODE : gameMode(primary));
            row.put("standing", standing);
            out.add(row);
        }
        return out;
    }

    private List<Event> confirmedEvents(Long teamId, Long tenantId) {
        List<Long> eventIds = eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(teamId)
                .stream()
                .filter(reg -> EventTeam.STATUS_CONFIRMED.equals(reg.getStatus()))
                .map(EventTeam::getEventId)
                .toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return eventRepository.findAllById(eventIds).stream()
                .filter(event -> event.getDeletedAt() == null && Objects.equals(tenantId, event.getTenantId()))
                .sorted(this::compareEventPriority)
                .toList();
    }

    private int compareEventPriority(Event left, Event right) {
        int byStatus = Integer.compare(eventPriority(left), eventPriority(right));
        if (byStatus != 0) {
            return byStatus;
        }
        String leftDate = left.getStartDate() == null ? "" : left.getStartDate();
        String rightDate = right.getStartDate() == null ? "" : right.getStartDate();
        return rightDate.compareTo(leftDate);
    }

    private int eventPriority(Event event) {
        if ("ongoing".equals(event.getStatus())) {
            return 0;
        }
        if ("ended".equals(event.getStatus())) {
            return 2;
        }
        return 1;
    }

    private Map<String, Object> eventView(Event event) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("name", event.getName());
        row.put("status", event.getStatus());
        row.put("startDate", event.getStartDate());
        row.put("endDate", event.getEndDate());
        row.put("gameMode", gameMode(event));
        return row;
    }

    /** 赛事下拉数据源：球队聚合输出为数组，最后一场统一为 yyyy-MM-dd。 */
    private List<Map<String, Object>> eventViews(List<PlayerEventSummaryDTO> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlayerEventSummaryDTO summary : summaries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("id", summary.getEventId());
            row.put("name", summary.getEventName());
            row.put("status", summary.getStatus());
            row.put("startDate", summary.getStartDate());
            row.put("endDate", summary.getEndDate());
            row.put("gameMode", blank(summary.getGameMode()) ? DEFAULT_GAME_MODE : summary.getGameMode());
            row.put("teamIds", summary.getTeamIdList());
            row.put("teamNames", summary.getTeamNameList());
            row.put("lastGameday", gamedayDate(summary.getLastGameday()));
            row.put("gameCount", summary.getGameCount());
            rows.add(row);
        }
        return rows;
    }

    /** 数据库日期若带时间部分，仅保留日期，保证前端按 yyyy-MM-dd 解析。 */
    private String gamedayDate(String gameday) {
        if (blank(gameday)) {
            return null;
        }
        String value = gameday.trim();
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private String gameMode(Event event) {
        return blank(event.getGameMode()) ? DEFAULT_GAME_MODE : event.getGameMode();
    }

    private Map<String, Object> standing(Long eventId, Long teamId, String gameMode) {
        List<Map<String, Object>> rows = statsService.getStandings(eventId, null, null, gameMode, 1, 100).getList();
        if (rows == null) {
            return Map.of();
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            if (teamId.equals(asLong(row.get("teamId")))) {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>(row);
                out.put("rank", i + 1);
                out.put("totalTeams", rows.size());
                return out;
            }
        }
        return Map.of();
    }

    /**
     * 解析筛选范围：null 表示全部球队/生涯；非法或不匹配的入参回落到全部/生涯，不自动选中第一项。
     * 球队与赛事相互独立：只选赛事时球队仍为全部球队。gameMode 优先级为请求参数 → 所选赛事 → 棒球。
     */
    private Scope resolveScope(List<Map<String, Object>> teams, Long requestedTeamId,
            Long requestedEventId, String requestedGameMode) {
        Map<String, Object> team = requestedTeamId == null ? null : teams.stream()
                .filter(row -> requestedTeamId.equals(asLong(row.get("id"))))
                .findFirst()
                .orElse(null);
        Map<String, Object> event = requestedEventId == null ? null : candidateEvents(teams, team).stream()
                .map(this::asMap)
                .filter(row -> requestedEventId.equals(asLong(row.get("id"))))
                .findFirst()
                .orElse(null);
        String eventGameMode = normalizeGameMode(event == null ? null : asString(event.get("gameMode")));
        String gameMode = resolveGameMode(normalizeGameMode(requestedGameMode), eventGameMode);
        return new Scope(
                team == null ? null : asLong(team.get("id")),
                team == null ? null : asString(team.get("name")),
                event == null ? null : asLong(event.get("id")),
                event == null ? null : asString(event.get("name")),
                gameMode);
    }

    /** 规范化运动类型：仅接受 BASEBALL/SOFTBALL（忽略大小写与首尾空白），其余返回 null。 */
    private static String normalizeGameMode(String raw) {
        if (blank(raw)) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return "BASEBALL".equals(normalized) || "SOFTBALL".equals(normalized) ? normalized : null;
    }

    /** 请求类型优先（已过白名单），其次所选赛事类型，最后回落到棒球。 */
    private static String resolveGameMode(String requested, String eventGameMode) {
        if (requested != null) {
            return requested;
        }
        return eventGameMode == null ? DEFAULT_GAME_MODE : eventGameMode;
    }

    /** 已选球队时赛事必须属于该球队的已确认赛事；全部球队时取所有代表球队的赛事。 */
    private List<?> candidateEvents(List<Map<String, Object>> teams, Map<String, Object> team) {
        if (team != null) {
            return team.get("events") instanceof List<?> list ? list : List.of();
        }
        List<Object> events = new ArrayList<>();
        for (Map<String, Object> row : teams) {
            if (row.get("events") instanceof List<?> list) {
                events.addAll(list);
            }
        }
        return events;
    }

    private Map<String, Object> scopeView(Scope scope) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("teamId", scope.teamId());
        row.put("teamName", scope.teamId() == null ? "全部球队" : scope.teamName());
        row.put("eventId", scope.eventId());
        row.put("eventName", scope.eventId() == null ? "生涯" : scope.eventName());
        row.put("gameMode", scope.gameMode());
        row.put("teamAll", scope.teamId() == null);
        row.put("career", scope.eventId() == null);
        return row;
    }

    private Map<String, Object> loadStats(Long playerId, Long eventId, String gameMode) {
        if (eventId != null) {
            List<PlayerStatsByEventDTO> seasons = statsService.getPlayerStatsBySeason(playerId, gameMode);
            for (PlayerStatsByEventDTO season : seasons) {
                if (eventId.equals(season.getEventId())) {
                    return statsMap(season.getBatting(), season.getPitching(), season.getFielding());
                }
            }
            // 所选赛事没有个人统计时返回空，而不是拿全部赛事合计冒充所选赛事
            return statsMap(Map.of(), Map.of(), Map.of());
        }
        return statsService.getPlayerStats(playerId, gameMode);
    }

    private Map<String, Object> statsMap(Map<String, Object> batting, Map<String, Object> pitching,
            Map<String, Object> fielding) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("batting", batting == null ? Map.of() : batting);
        out.put("pitching", pitching == null ? Map.of() : pitching);
        out.put("fielding", fielding == null ? Map.of() : fielding);
        return out;
    }

    private boolean hasBatting(Map<String, Object> stats) {
        Map<String, Object> batting = asMap(stats.get("batting"));
        return toInt(batting.get("ab")) > 0 || toInt(batting.get("pa")) > 0;
    }

    private boolean hasPitching(Map<String, Object> stats) {
        Double innings = toDouble(asMap(stats.get("pitching")).get("ip"));
        return innings != null && innings > 0.0;
    }

    private Map<String, Object> performance(Long playerId, Scope scope, Map<String, Object> config,
            Map<String, Object> stats, String gameMode) {
        Map<String, Object> perf = asMap(config.get("performance"));
        ClubhouseMetricCatalog.MetricDef def = ClubhouseMetricCatalog.find(asString(perf.get("metricKey")));
        if (def == null) {
            def = ClubhouseMetricCatalog.find(ClubhouseMetricCatalog.DEFAULT_PERFORMANCE_KEY);
        }
        String viz = asString(perf.get("viz"));
        if (viz == null || !ClubhouseMetricCatalog.supportsViz(def.key(), viz)) {
            viz = def.viz().get(0);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("metricKey", def.key());
        out.put("label", def.label());
        out.put("viz", viz);
        out.put("domain", def.domain());
        boolean domainAvailable = "pitching".equals(def.domain()) ? hasPitching(stats) : hasBatting(stats);
        if (!domainAvailable) {
            out.put("items", List.of());
            out.put("points", List.of());
            out.put("summaryValue", null);
            out.put("available", false);
            return out;
        }
        if ("batting.hitTypes".equals(def.key())) {
            List<Map<String, Object>> items = hitTypes(stats);
            out.put("items", items);
            out.put("points", List.of());
            out.put("summaryValue", null);
            out.put("available", !items.isEmpty());
            return out;
        }
        List<Map<String, Object>> points = performancePoints(playerId, scope.eventId(), gameMode, def);
        Object summary = metricValue(stats, def);
        out.put("points", points);
        out.put("summaryValue", summary);
        out.put("available", !points.isEmpty() || !emptyMetric(summary));
        return out;
    }

    private List<Map<String, Object>> performancePoints(Long playerId, Long eventId, String gameMode,
            ClubhouseMetricCatalog.MetricDef def) {
        List<PlayerGameLogEntryDTO> logs = statsService.getPlayerGameLogForRanking(playerId, 30, gameMode);
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<PlayerGameLogEntryDTO> filtered = logs.stream()
                .filter(log -> eventId == null || eventId.equals(log.getEventId()))
                .filter(log -> participated(log, def.domain()))
                .toList();
        List<PlayerGameLogEntryDTO> chronological = new ArrayList<>(filtered);
        chronological.sort(Comparator.comparing(PlayerGameLogEntryDTO::getGameday,
                Comparator.nullsLast(Comparator.naturalOrder())));
        int from = Math.max(0, chronological.size() - MAX_PERFORMANCE_POINTS);
        List<Map<String, Object>> points = new ArrayList<>();
        for (PlayerGameLogEntryDTO log : chronological.subList(from, chronological.size())) {
            Map<String, Object> domain = "pitching".equals(def.domain()) ? log.getPitching() : log.getBatting();
            Object raw = domain == null ? null : domain.get(def.field());
            Double value = toDouble(raw);
            if (value == null) {
                continue;
            }
            LinkedHashMap<String, Object> point = new LinkedHashMap<>();
            point.put("label", shortGameday(log.getGameday()));
            point.put("value", value);
            point.put("gameday", log.getGameday());
            point.put("eventId", log.getEventId());
            points.add(point);
        }
        return points;
    }

    private boolean participated(PlayerGameLogEntryDTO log, String domain) {
        if ("pitching".equals(domain)) {
            Map<String, Object> pitching = log.getPitching();
            Double innings = pitching == null ? null : toDouble(pitching.get("ip"));
            return innings != null && innings > 0;
        }
        Map<String, Object> batting = log.getBatting();
        return batting != null && (toInt(batting.get("ab")) > 0 || toInt(batting.get("pa")) > 0);
    }

    private List<Map<String, Object>> hitTypes(Map<String, Object> stats) {
        Map<String, Object> batting = asMap(stats.get("batting"));
        int singles = toInt(batting.get("singles"));
        int doubles = toInt(batting.get("doubles"));
        int triples = toInt(batting.get("triples"));
        int homeRuns = toInt(batting.get("hr")) + toInt(batting.get("insideParkHr"));
        int total = singles + doubles + triples + homeRuns;
        if (total <= 0) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        addHitType(items, "一垒安打", singles);
        addHitType(items, "二垒安打", doubles);
        addHitType(items, "三垒安打", triples);
        addHitType(items, "全垒打", homeRuns);
        return items;
    }

    private void addHitType(List<Map<String, Object>> items, String label, int value) {
        if (value <= 0) {
            return;
        }
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        items.add(row);
    }

    private Object metricValue(Map<String, Object> stats, ClubhouseMetricCatalog.MetricDef def) {
        if (stats == null || def == null || def.field() == null) {
            return null;
        }
        Map<String, Object> domain = asMap(stats.get("batting"));
        if ("pitching".equals(def.domain())) {
            domain = asMap(stats.get("pitching"));
        } else if ("fielding".equals(def.domain())) {
            domain = asMap(stats.get("fielding"));
        }
        return domain.get(def.field());
    }

    private List<Map<String, Object>> ranks(Long playerId, Scope scope, Map<String, Object> config,
            boolean battingAvailable, boolean pitchingAvailable, String gameMode) {
        List<Map<String, Object>> configured = configRanks(config);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : configured) {
            ClubhouseMetricCatalog.MetricDef def = ClubhouseMetricCatalog.find(asString(row.get("metricKey")));
            boolean available = def != null && (("batting".equals(def.domain()) && battingAvailable)
                    || ("pitching".equals(def.domain()) && pitchingAvailable));
            out.add(rankOne(playerId, scope, def, gameMode, available));
        }
        return out;
    }

    private List<Map<String, Object>> configRanks(Map<String, Object> config) {
        List<?> raw = config.get("ranks") instanceof List<?> list ? list : List.of();
        List<Map<String, Object>> ranks = raw.stream()
                .map(this::asMap)
                .filter(row -> ClubhouseMetricCatalog.isRankable(asString(row.get("metricKey"))))
                .limit(4)
                .toList();
        if (ranks.size() == 4) {
            return ranks;
        }
        return ClubhouseMetricCatalog.defaultRankEntries();
    }

    private Map<String, Object> rankOne(Long playerId, Scope scope, ClubhouseMetricCatalog.MetricDef def,
            String gameMode, boolean available) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("metricKey", def == null ? null : def.key());
        out.put("label", def == null ? "" : def.label());
        out.put("domain", def == null ? null : def.domain());
        out.put("available", available);
        out.put("value", "-");
        out.put("rank", null);
        out.put("total", 0);
        out.put("barPct", 0);
        if (!available || def == null || scope.teamId() == null || scope.eventId() == null) {
            return out;
        }
        List<Map<String, Object>> rows = leaderRows(scope, def, gameMode);
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            if (!playerId.equals(asLong(row.get("playerId")))) {
                continue;
            }
            int rank = i + 1;
            int total = rows.size();
            out.put("value", row.get(def.field()));
            out.put("rank", rank);
            out.put("total", total);
            out.put("barPct", total == 0 ? 0 : Math.max(0, Math.min(100,
                    Math.round(100f * (total - rank + 1) / total))));
            return out;
        }
        return out;
    }

    private List<Map<String, Object>> leaderRows(Scope scope, ClubhouseMetricCatalog.MetricDef def,
            String gameMode) {
        List<Long> teamIds = List.of(scope.teamId());
        List<Map<String, Object>> rows = "pitching".equals(def.domain())
                ? statsService.getPitchingLeaders(scope.eventId(), null, null, teamIds, null, null, null,
                        null, null, gameMode, 1, RANK_PAGE_SIZE, def.sortProp(), def.sortOrder(), false).getList()
                : statsService.getBattingLeaders(scope.eventId(), null, null, teamIds, null, null, null,
                        null, null, gameMode, 1, RANK_PAGE_SIZE, def.sortProp(), def.sortOrder(), false).getList();
        return rows == null ? List.of() : rows;
    }

    private List<Map<String, Object>> schedule(Long tenantId, List<Map<String, Object>> teams) {
        List<Long> teamIds = teams.stream()
                .map(row -> asLong(row.get("id")))
                .filter(Objects::nonNull)
                .toList();
        Set<Long> eventIds = new LinkedHashSet<>();
        for (Map<String, Object> team : teams) {
            for (Object event : team.get("events") instanceof List<?> list ? list : List.of()) {
                Long id = asLong(asMap(event).get("id"));
                if (id != null) {
                    eventIds.add(id);
                }
            }
        }
        if (teamIds.isEmpty() || eventIds.isEmpty()) {
            return List.of();
        }
        List<Game> games = gameRepository.findForTeamsAndStatuses(tenantId, teamIds, UPCOMING_STATUSES);
        String today = LocalDate.now().toString();
        List<Game> upcoming = games.stream()
                .filter(game -> game.getEventId() != null && eventIds.contains(game.getEventId()))
                .filter(game -> game.getGameday() != null && game.getGameday().compareTo(today) >= 0)
                .limit(MAX_SCHEDULE)
                .toList();
        if (upcoming.isEmpty()) {
            return List.of();
        }
        Map<Long, String> teamNames = teamNames(upcoming);
        Map<Long, String> eventNames = eventNames(upcoming);
        return upcoming.stream()
                .map(game -> scheduleRow(game, teamIds, teamNames, eventNames))
                .toList();
    }

    private Map<Long, String> teamNames(List<Game> games) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Game game : games) {
            if (game.getHomeTeamId() != null) {
                ids.add(game.getHomeTeamId());
            }
            if (game.getAwayTeamId() != null) {
                ids.add(game.getAwayTeamId());
            }
        }
        return teamNamesByIds(ids);
    }

    private Map<Long, String> teamNamesForRecent(List<PlayerGameLogEntryDTO> logs) {
        Set<Long> ids = new LinkedHashSet<>();
        for (PlayerGameLogEntryDTO log : logs) {
            if (log.getHomeTeamId() != null) {
                ids.add(log.getHomeTeamId());
            }
            if (log.getAwayTeamId() != null) {
                ids.add(log.getAwayTeamId());
            }
        }
        return teamNamesByIds(ids);
    }

    private Map<Long, String> teamNamesByIds(Collection<Long> ids) {
        LinkedHashMap<Long, String> out = new LinkedHashMap<>();
        teamRepository.findAllById(ids).forEach(team -> out.put(team.getId(), team.getName()));
        return out;
    }

    private Map<Long, String> eventNames(List<Game> games) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Game game : games) {
            if (game.getEventId() != null) {
                ids.add(game.getEventId());
            }
        }
        LinkedHashMap<Long, String> out = new LinkedHashMap<>();
        eventRepository.findAllById(ids).forEach(event -> out.put(event.getId(), event.getName()));
        return out;
    }

    private Map<String, Object> scheduleRow(Game game, List<Long> teamIds, Map<Long, String> teamNames,
            Map<Long, String> eventNames) {
        boolean isHome = teamIds.contains(game.getHomeTeamId());
        Long teamId = isHome ? game.getHomeTeamId() : game.getAwayTeamId();
        Long opponentId = isHome ? game.getAwayTeamId() : game.getHomeTeamId();
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("gameId", game.getId());
        row.put("eventId", game.getEventId());
        row.put("eventName", eventNames.get(game.getEventId()));
        row.put("gameday", game.getGameday());
        row.put("gameTime", game.getGameTime());
        row.put("status", game.getStatus());
        row.put("venue", game.getVenue());
        row.put("homeTeamId", game.getHomeTeamId());
        row.put("homeTeamName", teamNames.getOrDefault(game.getHomeTeamId(), "#" + game.getHomeTeamId()));
        row.put("awayTeamId", game.getAwayTeamId());
        row.put("awayTeamName", teamNames.getOrDefault(game.getAwayTeamId(), "#" + game.getAwayTeamId()));
        row.put("homeScore", game.getHomeScore());
        row.put("awayScore", game.getAwayScore());
        row.put("teamId", teamId);
        row.put("teamName", teamNames.getOrDefault(teamId, "#" + teamId));
        row.put("opponentName", teamNames.getOrDefault(opponentId, "#" + opponentId));
        row.put("isHome", isHome);
        return row;
    }

    private List<Map<String, Object>> recentGames(Player player, String gameMode) {
        List<PlayerGameLogEntryDTO> logs = statsService.getPlayerGameLog(player.getId(), MAX_RECENT, gameMode);
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        Map<Long, String> teamNames = teamNamesForRecent(logs);
        return logs.stream().map(log -> recentRow(log, teamNames)).toList();
    }

    private Map<String, Object> recentRow(PlayerGameLogEntryDTO log, Map<Long, String> teamNames) {
        boolean isHome = Boolean.TRUE.equals(log.getIsHome());
        Integer scoreFor = isHome ? log.getHomeScore() : log.getAwayScore();
        Integer scoreAgainst = isHome ? log.getAwayScore() : log.getHomeScore();
        Long opponentId = isHome ? log.getAwayTeamId() : log.getHomeTeamId();
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("gameId", log.getGameId());
        row.put("eventId", log.getEventId());
        row.put("eventName", log.getEventName());
        row.put("gameday", log.getGameday());
        row.put("teamId", log.getTeamId());
        row.put("teamName", log.getTeamName());
        row.put("opponentName", teamNames.getOrDefault(opponentId, "#" + opponentId));
        row.put("isHome", isHome);
        row.put("scoreFor", scoreFor);
        row.put("scoreAgainst", scoreAgainst);
        row.put("result", result(scoreFor, scoreAgainst));
        row.put("batting", log.getBatting());
        row.put("pitching", log.getPitching());
        return row;
    }

    private String result(Integer scoreFor, Integer scoreAgainst) {
        if (scoreFor == null || scoreAgainst == null) {
            return "—";
        }
        return scoreFor > scoreAgainst ? "W" : scoreFor < scoreAgainst ? "L" : "D";
    }

    private Map<String, Object> playerView(Player player, List<PlayerTeam> entries) {
        Map<Long, String> teamNames = teamNamesByIds(entries.stream().map(PlayerTeam::getTeamId).toList());
        List<Map<String, Object>> teamEntries = new ArrayList<>();
        for (PlayerTeam entry : entries) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("teamId", entry.getTeamId());
            row.put("teamName", teamNames.get(entry.getTeamId()));
            row.put("number", entry.getNumber());
            row.put("positions", PositionsJsonUtil.parseList(entry.getPositions()));
            row.put("current", entry.getCurrent());
            teamEntries.add(row);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", player.getId());
        out.put("name", player.getName());
        out.put("shortName", player.getShortName());
        out.put("nameEn", player.getNameEn());
        out.put("nickname", player.getNickname());
        out.put("number", player.getNumber());
        out.put("positions", PositionsJsonUtil.parseList(player.getPositions()));
        out.put("avatar", player.getAvatar());
        out.put("bgImage", player.getBgImage());
        out.put("height", player.getHeight());
        out.put("weight", player.getWeight());
        out.put("throwHand", player.getThrowHand());
        out.put("batHand", player.getBatHand());
        out.put("birthPlace", player.getBirthPlace());
        out.put("intro", player.getIntro());
        out.put("teamEntries", teamEntries);
        return out;
    }

    private String shortGameday(String gameday) {
        if (gameday == null) {
            return "";
        }
        return gameday.length() >= 10 ? gameday.substring(5) : gameday;
    }

    private static boolean emptyMetric(Object raw) {
        if (raw == null) {
            return true;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() || "-".equals(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static Long asLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asString(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private static Double toDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim().replace("%", "");
        if (value.isEmpty() || "-".equals(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int toInt(Object raw) {
        Double value = toDouble(raw);
        return value == null ? 0 : value.intValue();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record Scope(Long teamId, String teamName, Long eventId, String eventName, String gameMode) {
    }
}
