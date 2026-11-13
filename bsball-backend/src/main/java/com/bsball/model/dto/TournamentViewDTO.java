package com.bsball.model.dto;

import java.util.List;

/**
 * 赛事积分榜与淘汰赛对阵视图（Issue #665）。
 * 单/双循环只返回 standings；单败淘汰只返回 bracket；小组+淘汰赛两者都返回。
 */
public record TournamentViewDTO(
        Long eventId,
        String eventName,
        String season,
        String formatType,
        PointsRule pointsRule,
        List<StandingGroup> standings,
        Bracket bracket,
        List<String> warnings) {

    /** 积分规则：胜/平/负得分。 */
    public record PointsRule(Integer win, Integer draw, Integer loss) {
    }

    /** 一张积分表：单循环/双循环 stage=ROUND_ROBIN；小组赛 stage=GROUP。 */
    public record StandingGroup(
            String stage,
            String groupCode,
            String groupName,
            Integer advanceCount,
            List<StandingRow> rows,
            List<StandingMatch> matches) {
    }

    /** 积分表对应的已完赛比分，前端据此拼成对称赛果矩阵。 */
    public record StandingMatch(
            Long gameId,
            Long homeTeamId,
            Integer homeScore,
            Long awayTeamId,
            Integer awayScore) {
    }

    /** 积分表行。平局用 draws；胜率按 W/(W+L) 计算，平局不计入分母。 */
    public record StandingRow(
            Integer rank,
            Long teamId,
            String teamName,
            String teamLogo,
            Integer played,
            Integer wins,
            Integer losses,
            Integer draws,
            Integer points,
            Double winPct,
            Double gamesBehind,
            Integer runsFor,
            Integer runsAgainst,
            Integer runDiff,
            Boolean qualified) {
    }

    /** 淘汰赛视图：rounds 不含三四名；thirdPlace 单独返回。 */
    public record Bracket(
            List<BracketRound> rounds,
            Long championTeamId,
            BracketMatch thirdPlace) {
    }

    /** 一轮淘汰赛。 */
    public record BracketRound(
            String code,
            String name,
            List<BracketMatch> matches) {
    }

    /** 一场淘汰赛对阵。 */
    public record BracketMatch(
            Integer slot,
            Long gameId,
            Integer gameNumber,
            Long homeTeamId,
            String homeTeamName,
            Integer homeScore,
            Long awayTeamId,
            String awayTeamName,
            Integer awayScore,
            String status,
            Long winnerTeamId) {
    }
}
