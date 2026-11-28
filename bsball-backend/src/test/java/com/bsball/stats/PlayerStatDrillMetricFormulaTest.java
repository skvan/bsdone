package com.bsball.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("钻取指标：OBP/WHIP 与主统计同口径")
class PlayerStatDrillMetricFormulaTest {

    @Test
    @DisplayName("OBP 钻取使用 AB+BB+HBP+SF，不使用 PA")
    void obpDrillUsesStandardDenominator() {
        String sql = PlayerStatDrillBattingMetric.OBP.getValueSql();

        assertTrue(sql.contains("COALESCE(s.ab,0)"), sql);
        assertTrue(sql.contains("COALESCE(s.sf,0)"), sql);
        assertFalse(sql.contains("NULLIF(s.pa,0)"), sql);
    }

    @Test
    @DisplayName("OPS 钻取复用标准 OBP 分母与 SLG")
    void opsDrillUsesStandardObpDenominator() {
        String sql = PlayerStatDrillBattingMetric.OPS.getValueSql();

        assertTrue(sql.contains("COALESCE(s.sf,0)"), sql);
        assertFalse(sql.contains("COALESCE(s.pa,0) > 0"), sql);
    }

    @Test
    @DisplayName("WHIP 钻取使用 BB/HBP 分项，旧数据回退 BBHP-HBP")
    void whipDrillExcludesHitByPitch() {
        String sql = PlayerStatDrillPitchingMetric.WHIP.getValueSql();

        assertTrue(sql.contains("s.pitch_bb"), sql);
        assertTrue(sql.contains("s.pitch_hbp"), sql);
        assertTrue(sql.contains("s.pitch_bb_hp"), sql);
    }

    @Test
    @DisplayName("投手 AB 钻取扣除 SH/SF，并动态修正历史旧公式库值（#702）")
    void pitchAbDrillExcludesSacrifices() {
        String sql = PlayerStatDrillPitchingMetric.PITCH_AB.getValueSql();

        // 空值 → 新公式
        assertTrue(sql.contains("s.pitch_ab IS NULL"), sql);
        // 历史库值 = 旧公式且 SH/SF 存在 → 新公式
        assertTrue(sql.contains("s.pitch_ab ="), sql);
        // 其他非空值（人工覆写）→ 保留
        assertTrue(sql.contains("ELSE s.pitch_ab END"), sql);
        assertTrue(sql.contains("COALESCE(s.pitch_sh,0)"), sql);
        assertTrue(sql.contains("COALESCE(s.pitch_sf,0)"), sql);
        // 保留旧数据 BBHP 回退与整数转换
        assertTrue(sql.contains("COALESCE(s.pitch_bb_hp,0)"), sql);
        assertTrue(sql.contains("::integer"), sql);
    }

    @Test
    @DisplayName("守备钻取支持 DP 与捕手 SB")
    void fieldingDrillSupportsDpAndCatcherSb() {
        assertEquals(PlayerStatDrillFieldingMetric.DP, PlayerStatDrillFieldingMetric.fromApi("dp"));
        assertEquals(PlayerStatDrillFieldingMetric.CATCHER_SB, PlayerStatDrillFieldingMetric.fromApi("catcherSb"));
        assertNotNull(PlayerStatDrillFieldingMetric.fromApi("sballowed"));
    }
}
