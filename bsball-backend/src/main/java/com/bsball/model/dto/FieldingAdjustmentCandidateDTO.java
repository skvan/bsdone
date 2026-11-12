package com.bsball.model.dto;

import java.util.List;

/**
 * 赛后守备补记候选：从比赛快照中筛出失误 Play，并给出守备方可补记的球员。
 *
 * <p>结果录入页无现场快照上下文，由后端统一计算候选，避免前端重复解析快照。</p>
 */
public record FieldingAdjustmentCandidateDTO(
        String sourceEventId,
        Integer inning,
        String topBottom,
        Long teamId,
        Integer sequence,
        String inningLabel,
        Integer outsBefore,
        String baseLabel,
        String resultText,
        List<PlayerOption> players) {

    /** 可补记球员选项；id 字段与前端球员对象保持一致。 */
    public record PlayerOption(
            Long id,
            String name,
            String number,
            String position,
            List<String> positions) {
    }
}
