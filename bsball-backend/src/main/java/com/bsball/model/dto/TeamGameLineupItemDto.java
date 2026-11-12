package com.bsball.model.dto;

import java.time.LocalDateTime;

/** 球队端赛前阵容比赛列表项。 */
public record TeamGameLineupItemDto(
        Long gameId,
        Integer gameNumber,
        String gameday,
        String gameTime,
        String venue,
        String status,
        String side,
        Long opponentTeamId,
        String opponentName,
        Boolean hasSubmission,
        LocalDateTime lineupUpdatedAt,
        Boolean complete) {
}
