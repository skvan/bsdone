package com.bsball.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 基座快照与当前有效值对比；无基座时 legacy=true。 */
public record StatBaseCompareDTO(
        boolean legacy,
        List<PlayerDiff> rows) {

    public record PlayerDiff(
            Long playerId,
            String playerName,
            Long teamId,
            Integer isPitcher,
            String source,
            LocalDateTime capturedAt,
            List<FieldDiff> fields) {
    }

    public record FieldDiff(
            String key,
            String label,
            Double baseValue,
            Double effectiveValue) {
    }
}
