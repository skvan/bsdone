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
    @DisplayName("守备钻取支持 DP 与捕手 SB")
    void fieldingDrillSupportsDpAndCatcherSb() {
        assertEquals(PlayerStatDrillFieldingMetric.DP, PlayerStatDrillFieldingMetric.fromApi("dp"));
        assertEquals(PlayerStatDrillFieldingMetric.CATCHER_SB, PlayerStatDrillFieldingMetric.fromApi("catcherSb"));
        assertNotNull(PlayerStatDrillFieldingMetric.fromApi("sballowed"));
    }
}
