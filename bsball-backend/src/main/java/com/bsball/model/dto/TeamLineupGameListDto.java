package com.bsball.model.dto;

import java.util.List;

/** 球队端赛前阵容比赛列表（附赛事信息，供页面标题使用）。 */
public record TeamLineupGameListDto(
        Long eventId,
        String eventName,
        String season,
        String gameMode,
        List<TeamGameLineupItemDto> games) {
}
