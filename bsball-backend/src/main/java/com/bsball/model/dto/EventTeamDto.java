package com.bsball.model.dto;

import java.time.LocalDateTime;

/**
 * 赛事参赛登记（电子秩序册）视图 DTO：
 * 赛事侧列表填充 teamName/teamLogo/playerCount；球队侧「我的报名」填充 eventName/season/gameMode
 * 与 canWithdraw（可否撤销报名，仅已确认且本队尚无已开赛比赛为 true）。其余路径为 null。
 */
public record EventTeamDto(
        Long id,
        Long eventId,
        Long teamId,
        String teamName,
        String teamLogo,
        String status,
        Boolean rosterLocked,
        long playerCount,
        LocalDateTime confirmedAt,
        LocalDateTime declinedAt,
        LocalDateTime rosterSubmittedAt,
        String remark,
        String eventName,
        String season,
        String gameMode,
        Boolean canWithdraw) {
}
