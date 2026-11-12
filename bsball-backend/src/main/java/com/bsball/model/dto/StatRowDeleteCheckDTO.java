package com.bsball.model.dto;

import java.util.List;

/** 统计行删除前诊断：剩余字段、有效 ER 判定、守备补记与守备位置。 */
public record StatRowDeleteCheckDTO(
        Long statId,
        Long playerId,
        Boolean isPitcher,
        boolean deletable,
        String recommendedAction,
        List<Blocker> blockers) {

    /**
     * 单个阻断项。
     *
     * @param type              DATA_FIELD/ER_DECISION/FIELDING_ADJUSTMENT/FIELDING_POSITION
     * @param key               字段 key（计数类为空）
     * @param label             中文名
     * @param value             字段值或计数
     * @param recommendedAction ER_REVIEW/FIELDING_REVIEW/TRANSFER/CLEAR_FIELDS
     */
    public record Blocker(
            String type,
            String key,
            String label,
            Double value,
            String recommendedAction) {
    }
}
