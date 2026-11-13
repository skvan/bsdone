package com.bsball.service;

import com.bsball.model.dto.TournamentViewDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 赛事积分与淘汰赛对阵计算（Issue #665）。
 * <p>纯计算：不访问数据库、不依赖当前时间，便于单元测试；数据加载由 {@link TournamentService} 负责。
 */
@Service
public class TournamentStandingsCalculator {
    public static final String FORMAT_SINGLE_ROUND_ROBIN = "SINGLE_ROUND_ROBIN";
    public static final String FORMAT_DOUBLE_ROUND_ROBIN = "DOUBLE_ROUND_ROBIN";
    public static final String FORMAT_KNOCKOUT = "KNOCKOUT";
    public static final String FORMAT_GROUP_KNOCKOUT = "GROUP_KNOCKOUT";
    public static final String STAGE_GROUP = "GROUP";

    private static final List<String> BRACKET_STAGE_SEQUENCE = List.of("R16", "QF", "SF", "FINAL");
    private static final Set<String> BRACKET_STAGES = Set.of("R16", "QF", "SF", "FINAL", "THIRD_PLACE");
    private static final Map<String, String> BRACKET_STAGE_NAMES = Map.of(
            "R16", "1/8 决赛",
            "QF", "1/4 决赛",
            "SF", "半决赛",
            "FINAL", "决赛");
    private static final String UNASSIGNED_GROUP_NAME = "未分组";

    /** 计算积分榜；单败淘汰返回空列表。 */
    public List<TournamentViewDTO.StandingGroup> calculateStandings(
            Event event,
            List<Game> games,
            List<EventTeam> registrations,
            Map<Long, Team> teams,
            List<String> warnings) {
        String format = format(event);
        if (FORMAT_KNOCKOUT.equals(format)) {
            return List.of();
        }
        TournamentViewDTO.PointsRule pointsRule = pointsRule(event);
        addStandingWarnings(format, games, registrations, warnings);
        List<TableSpec> specs = buildTableSpecs(event, format, games, registrations);
        List<TournamentViewDTO.StandingGroup> groups = new ArrayList<>(specs.size());
        for (TableSpec spec : specs) {
            List<StandingAccumulator> rows = computeRows(spec, pointsRule);
            sortRows(rows, spec, pointsRule);
            groups.add(toGroup(spec, rows, teams));
        }
        return groups;
    }

    /** 计算淘汰赛对阵；非淘汰赛赛制返回 null。 */
    public TournamentViewDTO.Bracket calculateBracket(
            Event event,
            List<Game> games,
            Map<Long, Team> teams,
            List<String> warnings) {
        String format = format(event);
        if (!FORMAT_KNOCKOUT.equals(format) && !FORMAT_GROUP_KNOCKOUT.equals(format)) {
            return null;
        }
        List<Game> bracketGames = games.stream()
                .filter(game -> BRACKET_STAGES.contains(normalizeCode(game.getStageCode())))
                .toList();
        if (bracketGames.isEmpty()) {
            return new TournamentViewDTO.Bracket(List.of(), null, null);
        }
        addBracketWarnings(bracketGames, warnings);
        Map<String, List<Game>> byStage = new LinkedHashMap<>();
        for (String stage : BRACKET_STAGE_SEQUENCE) {
            List<Game> stageGames = bracketGames.stream()
                    .filter(game -> stage.equals(normalizeCode(game.getStageCode())))
                    .sorted(GAME_ORDER)
                    .toList();
            if (!stageGames.isEmpty()) {
                byStage.put(stage, stageGames);
            }
        }
        List<TournamentViewDTO.BracketRound> rounds = new ArrayList<>(byStage.size());
        for (Map.Entry<String, List<Game>> entry : byStage.entrySet()) {
            List<TournamentViewDTO.BracketMatch> matches = new ArrayList<>(entry.getValue().size());
            for (int i = 0; i < entry.getValue().size(); i++) {
                matches.add(toBracketMatch(entry.getValue().get(i), i + 1, teams));
            }
            rounds.add(new TournamentViewDTO.BracketRound(
                    entry.getKey(), BRACKET_STAGE_NAMES.get(entry.getKey()), matches));
        }
        Game finalGame = bracketGames.stream()
                .filter(game -> "FINAL".equals(normalizeCode(game.getStageCode())))
                .sorted(GAME_ORDER)
                .findFirst()
                .orElse(null);
        Long championTeamId = finalGame == null ? null : winnerTeamId(finalGame);
        Game thirdPlaceGame = bracketGames.stream()
                .filter(game -> "THIRD_PLACE".equals(normalizeCode(game.getStageCode())))
                .sorted(GAME_ORDER)
                .findFirst()
                .orElse(null);
        TournamentViewDTO.BracketMatch thirdPlace = thirdPlaceGame == null
                ? null
                : toBracketMatch(thirdPlaceGame, 1, teams);
        return new TournamentViewDTO.Bracket(rounds, championTeamId, thirdPlace);
    }

    /** 积分规则（缺省 2-1-0）。 */
    public TournamentViewDTO.PointsRule pointsRule(Event event) {
        return new TournamentViewDTO.PointsRule(
                intValue(event.getPointsWin(), 2),
                intValue(event.getPointsDraw(), 1),
                intValue(event.getPointsLoss(), 0));
    }

    private List<TableSpec> buildTableSpecs(
            Event event,
            String format,
            List<Game> games,
            List<EventTeam> registrations) {
        if (FORMAT_GROUP_KNOCKOUT.equals(format)) {
            return buildGroupSpecs(event, games, registrations);
        }
        TableSpec spec = new TableSpec("ROUND_ROBIN", null, null, 0);
        addRegistrationTeams(spec, registrations, null);
        for (Game game : games) {
            addGameTeams(spec, game);
        }
        spec.tableGames = games;
        return List.of(spec);
    }

    private List<TableSpec> buildGroupSpecs(
            Event event,
            List<Game> games,
            List<EventTeam> registrations) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (EventTeam registration : registrations) {
            if (EventTeam.STATUS_CONFIRMED.equals(registration.getStatus())) {
                codes.add(normalizeCode(registration.getGroupCode()));
            }
        }
        for (Game game : games) {
            if (isGroupStage(game)) {
                codes.add(normalizeCode(game.getGroupCode()));
            }
        }
        List<String> ordered = new ArrayList<>(codes);
        ordered.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        int advance = intValue(event.getAdvancePerGroup(), 2);
        List<TableSpec> specs = new ArrayList<>(ordered.size());
        for (String code : ordered) {
            String groupName = code == null ? UNASSIGNED_GROUP_NAME : code + " 组";
            TableSpec spec = new TableSpec(STAGE_GROUP, code, groupName, code == null ? 0 : advance);
            addRegistrationTeams(spec, registrations, code);
            for (Game game : games) {
                if (isGroupStage(game) && Objects.equals(code, normalizeCode(game.getGroupCode()))) {
                    addGameTeams(spec, game);
                    spec.tableGames.add(game);
                }
            }
            specs.add(spec);
        }
        return specs;
    }

    private void addRegistrationTeams(TableSpec spec, List<EventTeam> registrations, String groupCode) {
        for (EventTeam registration : registrations) {
            if (EventTeam.STATUS_CONFIRMED.equals(registration.getStatus())
                    && Objects.equals(groupCode, normalizeCode(registration.getGroupCode()))) {
                spec.teamIds.add(registration.getTeamId());
            }
        }
    }

    private void addGameTeams(TableSpec spec, Game game) {
        if (game.getHomeTeamId() != null) {
            spec.teamIds.add(game.getHomeTeamId());
        }
        if (game.getAwayTeamId() != null) {
            spec.teamIds.add(game.getAwayTeamId());
        }
    }

    private List<StandingAccumulator> computeRows(
            TableSpec spec,
            TournamentViewDTO.PointsRule pointsRule) {
        Map<Long, StandingAccumulator> rows = new LinkedHashMap<>();
        for (Long teamId : spec.teamIds) {
            rows.put(teamId, new StandingAccumulator(teamId));
        }
        for (Game game : spec.tableGames) {
            if (!isCountable(game)) {
                continue;
            }
            StandingAccumulator home = rows.computeIfAbsent(game.getHomeTeamId(), StandingAccumulator::new);
            StandingAccumulator away = rows.computeIfAbsent(game.getAwayTeamId(), StandingAccumulator::new);
            home.add(game, true, pointsRule);
            away.add(game, false, pointsRule);
        }
        return new ArrayList<>(rows.values());
    }

    private void sortRows(
            List<StandingAccumulator> rows,
            TableSpec spec,
            TournamentViewDTO.PointsRule pointsRule) {
        rows.sort(Comparator.comparingInt(StandingAccumulator::getPoints).reversed());
        int start = 0;
        while (start < rows.size()) {
            int end = start + 1;
            while (end < rows.size() && rows.get(end).getPoints() == rows.get(start).getPoints()) {
                end++;
            }
            if (end - start > 1) {
                applyHeadToHead(rows.subList(start, end), spec, pointsRule);
            }
            start = end;
        }
    }

    private void applyHeadToHead(
            List<StandingAccumulator> tied,
            TableSpec spec,
            TournamentViewDTO.PointsRule pointsRule) {
        Set<Long> tiedTeamIds = new HashSet<>();
        for (StandingAccumulator row : tied) {
            tiedTeamIds.add(row.teamId);
        }
        Map<Long, Integer> h2hPoints = new HashMap<>();
        Map<Long, Integer> h2hRunDiff = new HashMap<>();
        for (Game game : spec.tableGames) {
            if (!isCountable(game)
                    || !tiedTeamIds.contains(game.getHomeTeamId())
                    || !tiedTeamIds.contains(game.getAwayTeamId())) {
                continue;
            }
            h2hPoints.merge(game.getHomeTeamId(), pointsFor(game, game.getHomeTeamId(), pointsRule), Integer::sum);
            h2hPoints.merge(game.getAwayTeamId(), pointsFor(game, game.getAwayTeamId(), pointsRule), Integer::sum);
            h2hRunDiff.merge(game.getHomeTeamId(), runDiffFor(game, game.getHomeTeamId()), Integer::sum);
            h2hRunDiff.merge(game.getAwayTeamId(), runDiffFor(game, game.getAwayTeamId()), Integer::sum);
        }
        tied.sort(Comparator
                .comparingInt((StandingAccumulator row) -> h2hPoints.getOrDefault(row.teamId, 0)).reversed()
                .thenComparing(Comparator.comparingInt(
                        (StandingAccumulator row) -> h2hRunDiff.getOrDefault(row.teamId, 0)).reversed())
                .thenComparing(Comparator.comparingInt(StandingAccumulator::getRunDiff).reversed())
                .thenComparing(Comparator.comparingInt(StandingAccumulator::getRunsFor).reversed())
                .thenComparingInt(StandingAccumulator::getRunsAgainst)
                .thenComparingLong(StandingAccumulator::getTeamId));
    }

    private TournamentViewDTO.StandingGroup toGroup(
            TableSpec spec,
            List<StandingAccumulator> rows,
            Map<Long, Team> teams) {
        List<TournamentViewDTO.StandingRow> out = new ArrayList<>(rows.size());
        StandingAccumulator leader = rows.isEmpty() ? null : rows.get(0);
        for (int i = 0; i < rows.size(); i++) {
            StandingAccumulator row = rows.get(i);
            int rank = i + 1;
            Team team = teams.get(row.teamId);
            out.add(new TournamentViewDTO.StandingRow(
                    rank,
                    row.teamId,
                    team != null && team.getName() != null ? team.getName() : "球队#" + row.teamId,
                    team != null ? team.getLogo() : null,
                    row.played,
                    row.wins,
                    row.losses,
                    row.draws,
                    row.points,
                    row.winPct(),
                    leader == null ? 0.0 : gamesBehind(leader, row),
                    row.runsFor,
                    row.runsAgainst,
                    row.getRunDiff(),
                    spec.advanceCount > 0 && rank <= spec.advanceCount));
        }
        List<TournamentViewDTO.StandingMatch> matches = spec.tableGames.stream()
                .filter(this::isCountable)
                .map(game -> new TournamentViewDTO.StandingMatch(
                        game.getId(),
                        game.getHomeTeamId(),
                        game.getHomeScore(),
                        game.getAwayTeamId(),
                        game.getAwayScore()))
                .toList();
        return new TournamentViewDTO.StandingGroup(
                spec.stage, spec.groupCode, spec.groupName, spec.advanceCount, out, matches);
    }

    private TournamentViewDTO.BracketMatch toBracketMatch(
            Game game,
            int fallbackSlot,
            Map<Long, Team> teams) {
        int slot = game.getBracketSlot() == null ? fallbackSlot : game.getBracketSlot();
        return new TournamentViewDTO.BracketMatch(
                slot,
                game.getId(),
                game.getGameNumber(),
                game.getHomeTeamId(),
                teamName(teams, game.getHomeTeamId()),
                game.getHomeScore(),
                game.getAwayTeamId(),
                teamName(teams, game.getAwayTeamId()),
                game.getAwayScore(),
                game.getStatus(),
                winnerTeamId(game));
    }

    private void addStandingWarnings(
            String format,
            List<Game> games,
            List<EventTeam> registrations,
            List<String> warnings) {
        long missing = games.stream()
                .filter(this::isFinal)
                .filter(game -> !isCountable(game))
                .filter(game -> !FORMAT_GROUP_KNOCKOUT.equals(format) || isGroupStage(game))
                .count();
        if (missing > 0) {
            warnings.add("有 " + missing + " 场已结束比赛缺少比分，未计入积分榜");
        }
        if (FORMAT_GROUP_KNOCKOUT.equals(format)) {
            long unassigned = registrations.stream()
                    .filter(registration -> EventTeam.STATUS_CONFIRMED.equals(registration.getStatus()))
                    .filter(registration -> normalizeCode(registration.getGroupCode()) == null)
                    .count();
            if (unassigned > 0) {
                warnings.add("有 " + unassigned + " 支参赛球队未分组，已放入「未分组」积分表");
            }
        }
    }

    private void addBracketWarnings(List<Game> bracketGames, List<String> warnings) {
        long undecided = bracketGames.stream()
                .filter(this::isFinal)
                .filter(game -> winnerTeamId(game) == null)
                .count();
        if (undecided > 0) {
            warnings.add("有 " + undecided + " 场淘汰赛已结束但未判定胜者（缺比分或平局）");
        }
    }

    private boolean isCountable(Game game) {
        return isFinal(game)
                && game.getHomeTeamId() != null
                && game.getAwayTeamId() != null
                && !Objects.equals(game.getHomeTeamId(), game.getAwayTeamId())
                && game.getHomeScore() != null
                && game.getAwayScore() != null;
    }

    private boolean isFinal(Game game) {
        return game.getStatus() != null && "final".equalsIgnoreCase(game.getStatus().trim());
    }

    private boolean isGroupStage(Game game) {
        String stage = normalizeCode(game.getStageCode());
        return stage == null || STAGE_GROUP.equals(stage);
    }

    private int pointsFor(Game game, Long teamId, TournamentViewDTO.PointsRule rule) {
        if (isDraw(game)) {
            return intValue(rule.draw(), 1);
        }
        return Objects.equals(teamId, winnerTeamId(game))
                ? intValue(rule.win(), 2)
                : intValue(rule.loss(), 0);
    }

    private int runDiffFor(Game game, Long teamId) {
        if (Objects.equals(teamId, game.getHomeTeamId())) {
            return game.getHomeScore() - game.getAwayScore();
        }
        return game.getAwayScore() - game.getHomeScore();
    }

    private boolean isDraw(Game game) {
        return game.getHomeScore() != null
                && game.getAwayScore() != null
                && game.getHomeScore().intValue() == game.getAwayScore().intValue();
    }

    private Long winnerTeamId(Game game) {
        if (!isFinal(game)
                || game.getHomeTeamId() == null
                || game.getAwayTeamId() == null
                || game.getHomeScore() == null
                || game.getAwayScore() == null
                || game.getHomeScore().intValue() == game.getAwayScore().intValue()) {
            return null;
        }
        return game.getHomeScore() > game.getAwayScore()
                ? game.getHomeTeamId()
                : game.getAwayTeamId();
    }

    private String teamName(Map<Long, Team> teams, Long teamId) {
        if (teamId == null) {
            return null;
        }
        Team team = teams.get(teamId);
        return team != null && team.getName() != null ? team.getName() : "球队#" + teamId;
    }

    private double gamesBehind(StandingAccumulator leader, StandingAccumulator row) {
        return ((leader.wins - row.wins) + (row.losses - leader.losses)) / 2.0;
    }

    private String format(Event event) {
        String value = event.getFormatType();
        if (value == null || value.isBlank()) {
            return FORMAT_SINGLE_ROUND_ROBIN;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private int intValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static final Comparator<Game> GAME_ORDER = Comparator
            .comparing((Game game) -> game.getBracketSlot() == null ? Integer.MAX_VALUE : game.getBracketSlot())
            .thenComparing(game -> game.getGameNumber() == null ? Integer.MAX_VALUE : game.getGameNumber())
            .thenComparing(game -> game.getId() == null ? Long.MAX_VALUE : game.getId());

    private static final class TableSpec {
        private final String stage;
        private final String groupCode;
        private final String groupName;
        private final int advanceCount;
        private final Set<Long> teamIds = new LinkedHashSet<>();
        private List<Game> tableGames = new ArrayList<>();

        private TableSpec(String stage, String groupCode, String groupName, int advanceCount) {
            this.stage = stage;
            this.groupCode = groupCode;
            this.groupName = groupName;
            this.advanceCount = advanceCount;
        }
    }

    private static final class StandingAccumulator {
        private final Long teamId;
        private int played;
        private int wins;
        private int losses;
        private int draws;
        private int points;
        private int runsFor;
        private int runsAgainst;

        private StandingAccumulator(Long teamId) {
            this.teamId = teamId;
        }

        private void add(Game game, boolean home, TournamentViewDTO.PointsRule rule) {
            played++;
            int own = home ? game.getHomeScore() : game.getAwayScore();
            int other = home ? game.getAwayScore() : game.getHomeScore();
            runsFor += own;
            runsAgainst += other;
            if (own == other) {
                draws++;
                points += rule.draw() == null ? 1 : rule.draw();
            } else if (own > other) {
                wins++;
                points += rule.win() == null ? 2 : rule.win();
            } else {
                losses++;
                points += rule.loss() == null ? 0 : rule.loss();
            }
        }

        private double winPct() {
            int decisions = wins + losses;
            return decisions == 0 ? 0.0 : (double) wins / decisions;
        }

        private int getRunDiff() {
            return runsFor - runsAgainst;
        }

        private int getPoints() {
            return points;
        }

        private int getRunsFor() {
            return runsFor;
        }

        private int getRunsAgainst() {
            return runsAgainst;
        }

        private Long getTeamId() {
            return teamId;
        }
    }
}
