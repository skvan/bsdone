package com.bsball.model.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** 球队端赛前阵容编辑上下文：比赛/赛事/本队/规则/已保存阵容。 */
public record TeamLineupContextDto(
        Long gameId,
        Long eventId,
        String eventName,
        String season,
        String gameMode,
        Map<String, Boolean> specialRules,
        Long teamId,
        String teamName,
        String teamLogo,
        String side,
        Long opponentTeamId,
        String opponentName,
        Integer gameNumber,
        String gameday,
        String gameTime,
        String venue,
        String status,
        Boolean rosterRestricted,
        long rosterCount,
        String lineupJson,
        LocalDateTime lineupUpdatedAt,
        Boolean complete) {
}
