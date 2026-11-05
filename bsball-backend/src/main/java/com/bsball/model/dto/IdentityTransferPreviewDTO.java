package com.bsball.model.dto;

import java.util.List;

/** 整名球员身份转记预览。 */
public record IdentityTransferPreviewDTO(
        Long sourcePlayerId,
        String sourcePlayerName,
        Long targetPlayerId,
        String targetPlayerName,
        Long teamId,
        boolean transferable,
        List<String> blockers,
        List<RowPreview> rows,
        List<String> fieldingPositions,
        int erDecisionCount,
        boolean sourceOverride,
        boolean targetOverride,
        boolean liveSnapshotAvailable,
        int liveReferenceCount,
        int fieldingAdjustmentCount,
        boolean liveRosterConflict) {

    public record RowPreview(
            Integer isPitcher,
            Long sourceStatId,
            Long targetStatId,
            boolean targetExists) {
    }
}
