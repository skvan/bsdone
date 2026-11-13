package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.model.dto.TournamentViewDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Team;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TournamentStandingsCalculator：积分榜与淘汰赛对阵")
class TournamentStandingsCalculatorTest {

    private static final long EVENT_ID = 100L;
    private final TournamentStandingsCalculator calculator = new TournamentStandingsCalculator();

    @Test
    @DisplayName("单循环：按积分排名，同分依次比较相互战绩/净胜分")
    void singleRoundRobin_ranksByPointsThenHeadToHead() {
        Event event = event(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN);
        List<Game> games = List.of(
                finalGame(1, 1, 2, 10, 5, null, null),
                finalGame(2, 1, 3, 3, 2, null, null),
                finalGame(3, 2, 3, 4, 4, null, null));
        List<String> warnings = new ArrayList<>();

        List<TournamentViewDTO.StandingGroup> groups = calculator.calculateStandings(
                event, games, List.of(), teams(1, "甲队", 2, "乙队", 3, "丙队"), warnings);

        assertEquals(1, groups.size());
        List<TournamentViewDTO.StandingRow> rows = groups.get(0).rows();
        assertEquals(List.of(1L, 3L, 2L), rows.stream().map(TournamentViewDTO.StandingRow::teamId).toList());
        TournamentViewDTO.StandingRow leader = rows.get(0);
        assertEquals(2, leader.played());
        assertEquals(2, leader.wins());
        assertEquals(4, leader.points());
        assertEquals(1.0, leader.winPct(), 0.0001);
        assertEquals(0.0, leader.gamesBehind(), 0.0001);
        assertEquals(13, leader.runsFor());
        assertEquals(7, leader.runsAgainst());
        assertEquals(6, leader.runDiff());
        assertEquals(2, rows.get(1).rank());
        assertEquals(1.5, rows.get(1).gamesBehind(), 0.0001);
        assertEquals(2L, rows.get(2).teamId());
        assertFalse(leader.qualified());
        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("同分两队：相互战绩优先于总净胜分")
    void singleRoundRobin_headToHeadBeatsRunDifferential() {
        Event event = event(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN);
        List<Game> games = List.of(
                finalGame(1, 1, 2, 1, 0, null, null),
                finalGame(2, 3, 1, 10, 0, null, null),
                finalGame(3, 2, 4, 10, 0, null, null),
                finalGame(4, 3, 4, 10, 0, null, null),
                finalGame(5, 3, 2, 10, 0, null, null));

        List<TournamentViewDTO.StandingRow> rows = calculator.calculateStandings(
                event, games, List.of(),
                teams(1, "甲队", 2, "乙队", 3, "丙队", 4, "丁队"),
                new ArrayList<>()).get(0).rows();

        assertEquals(List.of(3L, 1L, 2L, 4L),
                rows.stream().map(TournamentViewDTO.StandingRow::teamId).toList());
        assertEquals(1L, rows.get(1).teamId());
        assertEquals(2, rows.get(1).rank());
        assertEquals(2L, rows.get(2).teamId());
        assertEquals(3, rows.get(2).rank());
    }

    @Test
    @DisplayName("双循环：两回合都计入积分，3-1-0 时胜场积 3 分")
    void doubleRoundRobin_countsBothLegsAnd310Points() {
        Event event = event(TournamentStandingsCalculator.FORMAT_DOUBLE_ROUND_ROBIN);
        event.setPointsWin(3);
        List<Game> games = List.of(
                finalGame(1, 1, 2, 5, 2, null, null),
                finalGame(2, 2, 1, 3, 1, null, null));

        List<TournamentViewDTO.StandingRow> rows = calculator.calculateStandings(
                event, games, List.of(), teams(1, "甲队", 2, "乙队"), new ArrayList<>()).get(0).rows();

        assertEquals(2, rows.get(0).played());
        assertEquals(3, rows.get(0).points());
        assertEquals(3, rows.get(1).points());
        assertEquals(1L, rows.get(0).teamId());
        assertEquals(2L, rows.get(1).teamId());
    }

    @Test
    @DisplayName("平局：双方各得平局积分，胜率不计平局")
    void draw_givesBothTeamsDrawPoints() {
        Event event = event(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN);
        List<Game> games = List.of(finalGame(1, 1, 2, 2, 2, null, null));

        List<TournamentViewDTO.StandingRow> rows = calculator.calculateStandings(
                event, games, List.of(), teams(1, "甲队", 2, "乙队"), new ArrayList<>()).get(0).rows();

        assertEquals(1, rows.get(0).draws());
        assertEquals(1, rows.get(0).points());
        assertEquals(0.0, rows.get(0).winPct(), 0.0001);
    }

    @Test
    @DisplayName("取消/未结束/缺比分的比赛不计入，并返回缺比分提示")
    void incompleteGames_areExcludedAndWarned() {
        Event event = event(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN);
        List<Game> games = List.of(
                game(1, 1, 2, null, null, "final", null, null, null),
                game(2, 1, 2, 4, 1, "cancelled", null, null, null),
                game(3, 1, 2, 3, 1, "scheduled", null, null, null));
        List<String> warnings = new ArrayList<>();

        List<TournamentViewDTO.StandingRow> rows = calculator.calculateStandings(
                event, games, List.of(), teams(1, "甲队", 2, "乙队"), warnings).get(0).rows();

        assertEquals(0, rows.get(0).played());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("缺少比分"));
    }

    @Test
    @DisplayName("小组赛：按小组分别计算，前两名标记晋级")
    void groupStage_marksTopTwoQualified() {
        Event event = event(TournamentStandingsCalculator.FORMAT_GROUP_KNOCKOUT);
        List<EventTeam> registrations = List.of(
                registration(1, "A"), registration(2, "A"), registration(3, "A"),
                registration(4, "B"), registration(5, "B"), registration(6, "B"));
        List<Game> games = List.of(
                groupGame(1, 1, 2, 10, 2, "A"),
                groupGame(2, 1, 3, 5, 1, "A"),
                groupGame(3, 2, 3, 3, 2, "A"),
                groupGame(4, 4, 5, 4, 1, "B"),
                groupGame(5, 4, 6, 3, 2, "B"),
                groupGame(6, 5, 6, 5, 4, "B"));

        List<TournamentViewDTO.StandingGroup> groups = calculator.calculateStandings(
                event, games, registrations,
                teams(1, "甲队", 2, "乙队", 3, "丙队", 4, "丁队", 5, "戊队", 6, "己队"),
                new ArrayList<>());

        assertEquals(2, groups.size());
        assertEquals("A", groups.get(0).groupCode());
        assertEquals("A 组", groups.get(0).groupName());
        assertTrue(groups.get(0).rows().get(0).qualified());
        assertTrue(groups.get(0).rows().get(1).qualified());
        assertFalse(groups.get(0).rows().get(2).qualified());
        assertEquals("B", groups.get(1).groupCode());
        assertTrue(groups.get(1).rows().get(0).qualified());
        assertTrue(groups.get(1).rows().get(1).qualified());
        assertFalse(groups.get(1).rows().get(2).qualified());
    }

    @Test
    @DisplayName("淘汰赛：按轮次输出对阵、冠军与三四名")
    void knockout_buildsRoundsChampionAndThirdPlace() {
        Event event = event(TournamentStandingsCalculator.FORMAT_KNOCKOUT);
        List<Game> games = List.of(
                bracketGame(1, 1, 2, 7, 3, "QF", 1),
                bracketGame(2, 3, 4, 5, 4, "QF", 2),
                bracketGame(3, 5, 6, 2, 6, "QF", 3),
                bracketGame(4, 7, 8, 3, 1, "QF", 4),
                bracketGame(5, 1, 3, 4, 2, "SF", 1),
                bracketGame(6, 6, 7, 5, 3, "SF", 2),
                bracketGame(7, 1, 6, 6, 2, "FINAL", 1),
                bracketGame(8, 3, 7, 7, 5, "THIRD_PLACE", 1));

        TournamentViewDTO.Bracket bracket = calculator.calculateBracket(
                event, games,
                teams(1, "甲队", 2, "乙队", 3, "丙队", 4, "丁队",
                        5, "戊队", 6, "己队", 7, "庚队", 8, "辛队"),
                new ArrayList<>());

        assertEquals(List.of("QF", "SF", "FINAL"),
                bracket.rounds().stream().map(TournamentViewDTO.BracketRound::code).toList());
        assertEquals(4, bracket.rounds().get(0).matches().size());
        assertEquals(2, bracket.rounds().get(1).matches().size());
        assertEquals(1, bracket.rounds().get(2).matches().size());
        assertEquals(1L, bracket.championTeamId().longValue());
        assertEquals(3L, bracket.thirdPlace().winnerTeamId().longValue());
    }

    @Test
    @DisplayName("小组+淘汰赛：积分榜只统计小组赛，淘汰赛单独输出对阵")
    void groupKnockout_separatesStandingsAndBracket() {
        Event event = event(TournamentStandingsCalculator.FORMAT_GROUP_KNOCKOUT);
        List<EventTeam> registrations = List.of(registration(1, "A"), registration(2, "A"));
        List<Game> games = List.of(
                groupGame(1, 1, 2, 5, 2, "A"),
                bracketGame(2, 1, 2, 3, 1, "FINAL", 1));

        List<String> warnings = new ArrayList<>();
        List<TournamentViewDTO.StandingGroup> groups = calculator.calculateStandings(
                event, games, registrations, teams(1, "甲队", 2, "乙队"), warnings);
        TournamentViewDTO.Bracket bracket = calculator.calculateBracket(
                event, games, teams(1, "甲队", 2, "乙队"), warnings);

        assertEquals(1, groups.size());
        assertEquals(1, groups.get(0).rows().get(0).played());
        assertNull(bracket.thirdPlace());
        assertEquals(1L, bracket.championTeamId().longValue());
    }

    @Test
    @DisplayName("空赛事：返回到空积分表，淘汰赛为空对阵")
    void emptyEvent_returnsEmptyStandingsAndBracket() {
        Event event = event(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN);

        List<TournamentViewDTO.StandingGroup> groups = calculator.calculateStandings(
                event, List.of(), List.of(), Map.of(), new ArrayList<>());
        TournamentViewDTO.Bracket bracket = calculator.calculateBracket(
                event, List.of(), Map.of(), new ArrayList<>());

        assertEquals(1, groups.size());
        assertTrue(groups.get(0).rows().isEmpty());
        assertNull(bracket);
    }

    private Event event(String format) {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setName("测试赛事");
        event.setSeason("2026");
        event.setFormatType(format);
        event.setPointsWin(2);
        event.setPointsDraw(1);
        event.setPointsLoss(0);
        event.setGroupCount(2);
        event.setAdvancePerGroup(2);
        event.setThirdPlace(true);
        event.setBracketSize(8);
        return event;
    }

    private EventTeam registration(long teamId, String groupCode) {
        EventTeam registration = new EventTeam();
        registration.setEventId(EVENT_ID);
        registration.setTeamId(teamId);
        registration.setStatus(EventTeam.STATUS_CONFIRMED);
        registration.setGroupCode(groupCode);
        return registration;
    }

    private Game finalGame(long id, long home, long away, int homeScore, int awayScore,
            String stageCode, String groupCode) {
        return game(id, home, away, homeScore, awayScore, "final", stageCode, groupCode, null);
    }

    private Game groupGame(long id, long home, long away, int homeScore, int awayScore, String groupCode) {
        return finalGame(id, home, away, homeScore, awayScore, "GROUP", groupCode);
    }

    private Game bracketGame(long id, long home, long away, int homeScore, int awayScore,
            String stageCode, int slot) {
        return game(id, home, away, homeScore, awayScore, "final", stageCode, null, slot);
    }

    private Game game(long id, long home, long away, Integer homeScore, Integer awayScore,
            String status, String stageCode, String groupCode, Integer slot) {
        Game game = new Game();
        game.setId(id);
        game.setEventId(EVENT_ID);
        game.setHomeTeamId(home);
        game.setAwayTeamId(away);
        game.setHomeScore(homeScore);
        game.setAwayScore(awayScore);
        game.setStatus(status);
        game.setStageCode(stageCode);
        game.setGroupCode(groupCode);
        game.setBracketSlot(slot);
        game.setGameNumber((int) id);
        return game;
    }

    private Map<Long, Team> teams(Object... entries) {
        Map<Long, Team> teams = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            Long id = ((Number) entries[i]).longValue();
            Team team = new Team();
            team.setId(id);
            team.setName((String) entries[i + 1]);
            teams.put(id, team);
        }
        return teams;
    }
}
