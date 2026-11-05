package com.bsball.utils;

import com.bsball.model.dto.StatRowDeleteCheckDTO;
import com.bsball.model.dto.StatRowDeleteCheckDTO.Blocker;
import com.bsball.model.entity.GamePlayerStat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 统计行删除诊断：把剩余字段与领域阻断项组装为统一结构。 */
public final class GameStatDeleteDiagnostics {

    private static final Set<String> RUN_FIELDS = Set.of(
            "pitchR", "er", "unearnedR", "pendingR", "earnedR");

    private GameStatDeleteDiagnostics() {
    }

    public static StatRowDeleteCheckDTO build(
            GamePlayerStat stat,
            int decisionCount,
            long adjustmentCount,
            long positionCount) {
        boolean pitcher = Integer.valueOf(1).equals(stat.getIsPitcher());
        String dataAction = pitcher ? "TRANSFER" : "CLEAR_FIELDS";
        List<Blocker> blockers = new ArrayList<>();
        for (GamePlayerStatZeros.NonZeroField field : GamePlayerStatZeros.nonZeroFields(stat)) {
            String action = RUN_FIELDS.contains(field.key()) ? "ER_REVIEW" : dataAction;
            blockers.add(new Blocker(
                    "DATA_FIELD", field.key(), field.label(), field.value(), action));
        }
        if (decisionCount > 0) {
            blockers.add(new Blocker(
                    "ER_DECISION", null, "有效自责分判定",
                    (double) decisionCount, "ER_REVIEW"));
        }
        if (adjustmentCount > 0) {
            blockers.add(new Blocker(
                    "FIELDING_ADJUSTMENT", null, "守备补记",
                    (double) adjustmentCount, "FIELDING_REVIEW"));
        }
        if (positionCount > 0) {
            // 投手守备位置可随转记整体搬移；非投手没有转记入口，仍需人工处理。
            blockers.add(new Blocker(
                    "FIELDING_POSITION", null,
                    pitcher ? "守备位置数据（可随转记搬移）" : "守备位置数据",
                    (double) positionCount,
                    pitcher ? "TRANSFER" : "FIELDING_REVIEW"));
        }
        return new StatRowDeleteCheckDTO(
                stat.getId(), stat.getPlayerId(), pitcher,
                blockers.isEmpty(), preferredAction(blockers, dataAction), blockers);
    }

    private static String preferredAction(List<Blocker> blockers, String dataAction) {
        Set<String> actions = blockers.stream()
                .map(Blocker::recommendedAction)
                .collect(Collectors.toSet());
        if (actions.contains("ER_REVIEW")) {
            return "ER_REVIEW";
        }
        if (actions.contains("FIELDING_REVIEW")) {
            return "FIELDING_REVIEW";
        }
        return blockers.isEmpty() ? "NONE" : dataAction;
    }
}
