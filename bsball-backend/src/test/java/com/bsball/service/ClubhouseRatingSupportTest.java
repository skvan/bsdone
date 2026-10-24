package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseRatingSupportTest {

    @Test
    @DisplayName("wOBA 使用固定业余权重")
    void computesWoba() {
        Map<String, Object> row = new HashMap<>();
        row.put("ab", 10);
        row.put("bb", 2);
        row.put("hbp", 0);
        row.put("singles", 3);
        row.put("doubles", 1);
        row.put("triples", 0);
        row.put("hr", 1);
        row.put("insideParkHr", 0);
        row.put("sf", 0);
        row.put("ibb", 0);
        row.put("sb", 1);
        row.put("cs", 0);
        assertEquals(0.629, ClubhouseRatingSupport.woba(row), 0.001);
    }

    @Test
    @DisplayName("wOBA 每一项权重都生效，场内本垒打按本垒打计入")
    void wobaCountsInsideTheParkHomeRunsAndEveryTerm() {
        Map<String, Object> row = new HashMap<>();
        row.put("ab", 20);
        row.put("bb", 4);
        row.put("hbp", 2);
        row.put("singles", 5);
        row.put("doubles", 2);
        row.put("triples", 1);
        row.put("hr", 2);
        row.put("insideParkHr", 1);
        row.put("sf", 1);
        row.put("ibb", 1);
        row.put("sb", 2);
        row.put("cs", 1);
        // 分母 = 20+4-1+1+2 = 26。
        // 分子 = .70×4 + .70×2 + .90×5 + 1.25×2 + 1.60×1 + 2.00×(2+1) + .20×2 - .40×1 = 18.8。
        assertEquals(18.8 / 26.0, ClubhouseRatingSupport.woba(row), 0.0001);
        // 仅去掉 HBP（权重 .70）：分子 17.4，结果 0.6692，与本断言不同。
        assertEquals(0.7231, ClubhouseRatingSupport.woba(row), 0.0001);
    }

    @Test
    @DisplayName("wOBA 接受字符串数值，分母为零时返回 0")
    void wobaHandlesStringsAndZeroDenominator() {
        Map<String, Object> row = new HashMap<>();
        row.put("ab", "4");
        row.put("bb", "1");
        row.put("hbp", "0");
        row.put("singles", "2");
        row.put("doubles", 0);
        row.put("triples", 0);
        row.put("hr", "0");
        row.put("sf", 0);
        row.put("ibb", 0);
        row.put("sb", "0");
        row.put("cs", 0);
        assertEquals((0.70 + 1.80) / 5.0, ClubhouseRatingSupport.woba(row), 0.0001);

        assertEquals(0.0, ClubhouseRatingSupport.woba(new HashMap<>()), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.woba(null), 0.0001);
        Map<String, Object> negative = new HashMap<>();
        negative.put("ab", 0);
        negative.put("bb", 0);
        negative.put("ibb", 2);
        assertEquals(0.0, ClubhouseRatingSupport.woba(negative), 0.0001);
    }

    @Test
    @DisplayName("FIP 与可靠性收缩")
    void computesFipAndReliability() {
        Map<String, Object> row = Map.of("pitchHr", 2, "pitchBb", 3, "pitchHbp", 1,
                "pitchSo", 10, "ip", 9.0);
        assertEquals(5.10, ClubhouseRatingSupport.fip(row, 3.10), 0.01);
        assertEquals(0.5, ClubhouseRatingSupport.reliability(25, 50), 0.001);
        assertEquals(60.0, ClubhouseRatingSupport.shrink(70, 0.5), 0.001);
    }

    @Test
    @DisplayName("FIP 把被场内本垒打算作本垒打")
    void fipCountsPitchInsideParkHomeRuns() {
        Map<String, Object> row = Map.of("pitchHr", 1, "pitchInsideParkHr", 1,
                "pitchBb", 3, "pitchHbp", 1, "pitchSo", 10, "ip", 9.0);
        // 等价于 2 支本垒打：(26+12-20)/9 + 3.10 = 5.10。
        assertEquals(5.10, ClubhouseRatingSupport.fip(row, 3.10), 0.01);
    }

    @Test
    @DisplayName("FIP 按棒球记法换算局数，无投球局返回 0")
    void fipConvertsBaseballInningsAndGuardsZero() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("pitchHr", 1);
        row.put("pitchBb", 0);
        row.put("pitchHbp", 0);
        row.put("pitchSo", 0);
        row.put("ip", "5.2");
        assertEquals(13.0 / (17.0 / 3.0) + 3.0, ClubhouseRatingSupport.fip(row, 3.0), 0.0001);

        assertEquals(0.0, ClubhouseRatingSupport.fip(Map.of("ip", 0.0), 3.1), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.fip(null, 3.1), 0.0001);
    }

    @Test
    @DisplayName("可靠性按样本比例截断到 0–1，非法门槛返回 0")
    void reliabilityClampsAndGuardsInvalidThreshold() {
        assertEquals(1.0, ClubhouseRatingSupport.reliability(75, 50), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.reliability(0, 50), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.reliability(-5, 50), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.reliability(25, 0), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.reliability(25, -10), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.reliability(Double.NaN, 50), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.reliability(25, Double.NaN), 0.0001);
    }

    @Test
    @DisplayName("收缩向 50 靠拢，可靠性为 0 时回到中位")
    void shrinkPullsScoreTowardFifty() {
        assertEquals(50.0, ClubhouseRatingSupport.shrink(70, 0), 0.0001);
        assertEquals(70.0, ClubhouseRatingSupport.shrink(70, 1), 0.0001);
        assertEquals(45.0, ClubhouseRatingSupport.shrink(40, 0.5), 0.0001);
    }

    @Test
    @DisplayName("守位分组")
    void groupsPositions() {
        assertEquals("C", ClubhouseRatingSupport.positionGroup("C"));
        assertEquals("INF", ClubhouseRatingSupport.positionGroup("SS"));
        assertEquals("OF", ClubhouseRatingSupport.positionGroup("CF"));
        assertEquals("DH", ClubhouseRatingSupport.positionGroup("DH"));
    }

    @Test
    @DisplayName("守位分组忽略大小写与空白，未知守位归 UTIL")
    void groupsPositionsCaseInsensitively() {
        assertEquals("1B", ClubhouseRatingSupport.positionGroup(" 1b "));
        assertEquals("INF", ClubhouseRatingSupport.positionGroup("2b"));
        assertEquals("INF", ClubhouseRatingSupport.positionGroup("3B"));
        assertEquals("OF", ClubhouseRatingSupport.positionGroup("lf"));
        assertEquals("OF", ClubhouseRatingSupport.positionGroup("RF"));
        assertEquals("P", ClubhouseRatingSupport.positionGroup("p"));
        assertEquals("UTIL", ClubhouseRatingSupport.positionGroup("XX"));
        assertEquals("UTIL", ClubhouseRatingSupport.positionGroup(""));
        assertEquals("UTIL", ClubhouseRatingSupport.positionGroup(null));
    }

    @Test
    @DisplayName("FIP 常数对齐联盟平均防御率")
    void computesFipConstantFromLeagueTotals() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("pitchHr", 1);
        first.put("pitchInsideParkHr", 1);
        first.put("pitchBb", 3);
        first.put("pitchHbp", 1);
        first.put("pitchSo", 10);
        first.put("ip", 9.0);
        first.put("er", 5);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("pitchHr", 1);
        second.put("pitchInsideParkHr", 0);
        second.put("pitchBb", 2);
        second.put("pitchHbp", 0);
        second.put("pitchSo", 5);
        second.put("ip", 5.0);
        second.put("er", 3);
        // 合计：HR(含场内)=3、BB=5、HBP=1、SO=15、IP=14、ER=8。
        // 联盟 ERA = 72/14 ≈ 5.1429；raw FIP = 27/14 ≈ 1.9286。
        // ERA ≠ 2×rawFIP（5.1429 ≠ 3.8571），因此只能由 ERA−rawFIP 得到常数 45/14 ≈ 3.2143。
        double constant = ClubhouseRatingSupport.fipConstant(List.of(first, second));
        assertEquals(3.2143, constant, 0.001);

        // 性质断言：池化 raw FIP + 常数 ≈ 联盟 ERA。
        double pooledRawFip = (13.0 * 3 + 3.0 * (5 + 1) - 2.0 * 15) / 14.0;
        double leagueEra = 8 * 9.0 / 14.0;
        assertEquals(leagueEra, pooledRawFip + constant, 0.0001);
    }

    @Test
    @DisplayName("FIP 常数跳过无投球局的统计行")
    void fipConstantSkipsRowsWithoutInnings() {
        Map<String, Object> valid = Map.of("pitchHr", 1, "pitchBb", 1, "pitchHbp", 0,
                "pitchSo", 3, "ip", 9.0, "er", 3);
        Map<String, Object> noInnings = Map.of("pitchHr", 5, "pitchBb", 5, "pitchHbp", 5,
                "pitchSo", 0, "ip", 0.0, "er", 20);
        // 仅按有效行：ERA=3，raw FIP=10/9，常数=17/9≈1.8889。
        assertEquals(17.0 / 9.0,
                ClubhouseRatingSupport.fipConstant(List.of(valid, noInnings)), 0.0001);
    }

    @Test
    @DisplayName("FIP 常数在无有效投球局时返回 0")
    void fipConstantGuardsEmptyPool() {
        assertEquals(0.0, ClubhouseRatingSupport.fipConstant(List.of()), 0.0001);
        assertEquals(0.0, ClubhouseRatingSupport.fipConstant(null), 0.0001);
        assertEquals(0.0,
                ClubhouseRatingSupport.fipConstant(List.of(Map.of("ip", 0.0, "er", 5))), 0.0001);
    }
}
