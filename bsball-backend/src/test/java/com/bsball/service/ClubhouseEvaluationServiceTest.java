package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubhouseEvaluationServiceTest {
    @Mock
    private StatsService statsService;

    @InjectMocks
    private ClubhouseEvaluationService service;

    @Test
    @DisplayName("三张雷达各 6 轴，并输出全租户总体评分")
    void buildsSixAxisCardsAndOverall() {
        stubCareerPools(bigBattingPool(), bigPitchingPool(), bigFieldingPool());

        Map<String, Object> evaluation = service.buildCareer(
                10L, "BASEBALL", bigCareerStats(), "CF");
        List<Map<String, Object>> cards = cards(evaluation);

        assertEquals(3, cards.size());
        assertEquals(List.of("AVG", "OBP", "SLG", "OPS", "RBI", "SB"),
                abbrs(card(cards, "batting")));
        assertEquals(List.of("FPCT", "TC/G", "PO/G", "A/G", "DP/G", "RF/9"),
                abbrs(card(cards, "fielding")));
        assertEquals(List.of("ERA", "WHIP", "K/9", "BB/9", "HR/9", "IP"),
                abbrs(card(cards, "pitching")));
        assertEquals(6, axes(card(cards, "batting")).size());
        assertEquals(6, axes(card(cards, "fielding")).size());
        assertEquals(6, axes(card(cards, "pitching")).size());

        assertEquals("全租户生涯", evaluation.get("comparison"));
        Map<String, Object> overall = overall(evaluation);
        assertEquals(100, overall.get("score"));
        assertEquals("S", overall.get("grade"));
        assertEquals(1.0, overall.get("reliability"));
        assertEquals("样本充足", overall.get("confidenceLabel"));
        assertEquals("TWO_WAY", overall.get("role"));
        assertEquals(Map.of("batting", 0.4, "pitching", 0.4, "fielding", 0.2),
                overall.get("weights"));
    }

    @Test
    @DisplayName("雷达池固定使用全租户生涯查询，并输出 wOBA/FIP 说明")
    void careerPoolsAreAllTenantAndExposeQualityMeta() {
        stubCareerPools(bigBattingPool(), bigPitchingPool(), bigFieldingPool());

        Map<String, Object> evaluation = service.buildCareer(
                10L, "BASEBALL", bigCareerStats(), "CF");
        List<Map<String, Object>> cards = cards(evaluation);

        verifyAllTenantPools();
        Map<String, Object> battingMeta = meta(card(cards, "batting"));
        assertNotNull(battingMeta.get("woba"));
        assertEquals("参考 wOBA", card(cards, "batting").get("note"));
        Map<String, Object> pitchingMeta = meta(card(cards, "pitching"));
        assertNotNull(pitchingMeta.get("fip"));
        assertNotNull(pitchingMeta.get("fipConstant"));
        assertEquals("参考 FIP", card(cards, "pitching").get("note"));
    }

    @Test
    @DisplayName("小样本向 50 分收缩并按可靠性标注参考值")
    void shrinksSmallSampleTowardFifty() {
        Map<String, Object> self = battingRow(10L, "CF", 25, 20, 8, 2, 0, 2,
                4, 1, 0, 15, 5, 0);
        List<Map<String, Object>> pool = List.of(self,
                battingRow(11L, "CF", 25, 25, 2, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        Map<String, Object> career = careerStats(self, Map.of(), Map.of());
        stubCareerPools(pool, List.of(), List.of());

        Map<String, Object> evaluation = service.buildCareer(10L, "BASEBALL", career, "CF");
        Map<String, Object> batting = card(cards(evaluation), "batting");

        assertEquals(true, batting.get("available"));
        assertEquals(75, batting.get("score"));
        assertEquals("B+", batting.get("grade"));
        assertEquals(0.5, batting.get("reliability"));
        assertEquals("参考值", batting.get("confidenceLabel"));
        assertNotNull(batting.get("sampleSize"));
        Map<String, Object> overall = overall(evaluation);
        assertEquals(75, overall.get("score"));
        assertEquals(0.5, overall.get("reliability"));
        assertEquals("参考值", overall.get("confidenceLabel"));
        assertEquals("POSITION_PLAYER", overall.get("role"));
        assertEquals(Map.of("batting", 1.0), overall.get("weights"));
    }

    @Test
    @DisplayName("达到稳定门槛时可靠性为 1 并显示样本充足")
    void fullReliabilityShowsSufficientSample() {
        Map<String, Object> self = battingRow(10L, "CF", 50, 40, 16, 4, 0, 4,
                8, 1, 0, 30, 10, 0);
        List<Map<String, Object>> pool = List.of(self,
                battingRow(11L, "CF", 25, 25, 2, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        stubCareerPools(pool, List.of(), List.of());

        Map<String, Object> evaluation = service.buildCareer(
                10L, "BASEBALL", careerStats(self, Map.of(), Map.of()), "CF");
        Map<String, Object> batting = card(cards(evaluation), "batting");

        assertEquals(100, batting.get("score"));
        assertEquals(1.0, batting.get("reliability"));
        assertEquals("样本充足", batting.get("confidenceLabel"));
    }

    @Test
    @DisplayName("投手无打击样本时按投球 80%、守备 20% 计算总体")
    void pitcherWithoutBattingUsesEightyTwentyWeights() {
        stubCareerPools(bigBattingPool(), bigPitchingPool(), bigFieldingPool());
        Map<String, Object> career = careerStats(Map.of(), bigPitchingRow(), bigFieldingRow());

        Map<String, Object> evaluation = service.buildCareer(10L, "BASEBALL", career, "P");

        assertEquals(false, card(cards(evaluation), "batting").get("available"));
        Map<String, Object> overall = overall(evaluation);
        assertEquals("PITCHER", overall.get("role"));
        assertEquals(Map.of("pitching", 0.8, "fielding", 0.2), overall.get("weights"));
        assertEquals(100, overall.get("score"));
    }

    @Test
    @DisplayName("野手按打击 55%、守备 45% 合并总体")
    void positionPlayerUsesBattingFieldingWeights() {
        stubCareerPools(bigBattingPool(), bigPitchingPool(), bigFieldingPool());
        Map<String, Object> career = careerStats(bigBattingRow(), Map.of(), bigFieldingRow());

        Map<String, Object> evaluation = service.buildCareer(10L, "BASEBALL", career, "CF");

        Map<String, Object> overall = overall(evaluation);
        assertEquals("POSITION_PLAYER", overall.get("role"));
        assertEquals(Map.of("batting", 0.55, "fielding", 0.45), overall.get("weights"));
    }

    @Test
    @DisplayName("低于展示门槛的分项不参与总体，剩余权重归一化")
    void belowDisplayThresholdExcludesDomain() {
        Map<String, Object> lowBatting = battingRow(10L, "CF", 5, 4, 1, 0, 0, 0,
                1, 0, 0, 0, 0, 0);
        Map<String, Object> lowPitching = pitchingRow(10L, 1.0, 2, 0, 1, 0, 1, 1.5);
        Map<String, Object> lowFielding = fieldingRow(10L, "CF", 1, 3.0, 3, 1, 1, 0);
        Map<String, Object> career = careerStats(lowBatting, lowPitching, lowFielding);
        stubCareerPools(bigBattingPool(), bigPitchingPool(), bigFieldingPool());

        Map<String, Object> evaluation = service.buildCareer(10L, "BASEBALL", career, "CF");
        List<Map<String, Object>> cards = cards(evaluation);

        assertEquals(3, cards.size());
        assertEquals(false, card(cards, "batting").get("available"));
        assertTrue(String.valueOf(card(cards, "batting").get("reason")).contains("样本不足"));
        assertEquals(false, card(cards, "pitching").get("available"));
        assertTrue(String.valueOf(card(cards, "pitching").get("reason")).contains("样本不足"));
        assertEquals(false, card(cards, "fielding").get("available"));
        assertTrue(String.valueOf(card(cards, "fielding").get("reason")).contains("样本不足"));
        assertEquals(false, evaluation.get("available"));
        Map<String, Object> overall = overall(evaluation);
        assertNull(overall.get("score"));
        assertNull(overall.get("grade"));
        assertEquals(0.0, overall.get("reliability"));
        assertEquals("样本不足", overall.get("confidenceLabel"));
        assertEquals(Map.of(), overall.get("weights"));
    }

    @Test
    @DisplayName("守位组不足 5 人时回退全体并标注未按守位分组")
    void fallsBackToAllFieldersWhenPositionGroupIsSmall() {
        List<Map<String, Object>> pool = new ArrayList<>();
        pool.add(fieldingRow(10L, "SS", 10, 40.0, 20, 18, 2, 4));
        pool.add(fieldingRow(11L, "2B", 8, 30.0, 28, 4, 0, 1));
        pool.add(fieldingRow(12L, "CF", 8, 30.0, 12, 10, 2, 1));
        pool.add(fieldingRow(13L, "LF", 8, 26.0, 10, 8, 2, 1));
        pool.add(fieldingRow(14L, "RF", 8, 24.0, 8, 6, 2, 0));
        pool.add(fieldingRow(15L, "OF", 8, 22.0, 6, 5, 2, 0));
        pool.add(fieldingRow(16L, "CF", 8, 20.0, 5, 4, 2, 0));
        Map<String, Object> career = careerStats(Map.of(), Map.of(),
                fieldingRow(10L, "SS", 10, 40.0, 20, 18, 2, 4));
        stubCareerPools(List.of(), List.of(), pool);

        Map<String, Object> evaluation = service.buildCareer(10L, "BASEBALL", career, "SS");
        Map<String, Object> fielding = card(cards(evaluation), "fielding");

        assertEquals(true, fielding.get("available"));
        assertEquals("未按守位分组", fielding.get("note"));
    }

    @Test
    @DisplayName("守位组达到 5 人时只用组内百分位，不受其他守位影响")
    void comparesWithinPositionGroupWhenEnoughPeers() {
        List<Map<String, Object>> pool = new ArrayList<>();
        pool.add(fieldingRow(10L, "CF", 10, 40.0, 20, 18, 2, 4));
        pool.add(fieldingRow(11L, "LF", 8, 30.0, 12, 10, 2, 1));
        pool.add(fieldingRow(12L, "RF", 8, 28.0, 11, 9, 2, 1));
        pool.add(fieldingRow(13L, "CF", 8, 26.0, 9, 7, 2, 1));
        pool.add(fieldingRow(14L, "OF", 8, 24.0, 7, 6, 2, 0));
        pool.add(fieldingRow(15L, "SS", 10, 40.0, 30, 10, 0, 5));
        pool.add(fieldingRow(16L, "2B", 10, 40.0, 28, 8, 0, 4));
        Map<String, Object> career = careerStats(Map.of(), Map.of(),
                fieldingRow(10L, "CF", 10, 40.0, 20, 18, 2, 4));
        stubCareerPools(List.of(), List.of(), pool);

        Map<String, Object> evaluation = service.buildCareer(10L, "BASEBALL", career, "CF");
        Map<String, Object> fielding = card(cards(evaluation), "fielding");

        assertNull(fielding.get("note"));
        assertEquals(100, axes(fielding).get(0).get("percentile"));
        assertEquals(100, axes(fielding).get(5).get("percentile"));
    }

    @Test
    @DisplayName("本人不在 500 人池中时用生涯统计补齐本人行")
    void fallsBackToCareerStatsWhenSelfIsOutsidePool() {
        List<Map<String, Object>> batting = List.of(
                battingRow(11L, "CF", 40, 30, 6, 1, 0, 0, 2, 0, 0, 8, 1, 0),
                battingRow(12L, "CF", 30, 25, 4, 0, 0, 0, 1, 0, 0, 5, 0, 0));
        List<Map<String, Object>> pitching = List.of(
                pitchingRow(11L, 10.0, 6, 1, 4, 0, 8, 1.4),
                pitchingRow(12L, 8.0, 6, 1, 4, 0, 6, 1.6));
        List<Map<String, Object>> fielding = List.of(
                fieldingRow(11L, "CF", 8, 30.0, 15, 13, 3, 2),
                fieldingRow(12L, "CF", 6, 20.0, 10, 8, 2, 1));
        stubCareerPools(batting, pitching, fielding);

        Map<String, Object> evaluation = service.buildCareer(
                10L, "BASEBALL", bigCareerStats(), "CF");
        List<Map<String, Object>> cards = cards(evaluation);
        Map<String, Object> battingCard = card(cards, "batting");

        assertEquals(true, battingCard.get("available"));
        assertEquals("0.36", axes(battingCard).get(0).get("value"));
        assertEquals(100, axes(battingCard).get(1).get("percentile"));
        assertNotNull(overall(evaluation).get("score"));
        assertEquals("TWO_WAY", overall(evaluation).get("role"));
    }

    @Test
    @DisplayName("旧 build 调用兼容为全租户生涯口径")
    void legacyBuildDelegatesToCareer() {
        stubCareerPools(bigBattingPool(), bigPitchingPool(), bigFieldingPool());

        Map<String, Object> evaluation = service.build(10L, 1L, 2L, "BASEBALL", bigCareerStats());

        assertEquals("全租户生涯", evaluation.get("comparison"));
        assertEquals(3, cards(evaluation).size());
        assertNotNull(overall(evaluation).get("score"));
        verifyAllTenantPools();
    }

    @Test
    @DisplayName("百分位支持并列、反向指标和单样本")
    void percentileHandlesTiesReverseAndSingleSample() {
        assertEquals(100, ClubhouseEvaluationService.percentile(List.of(1.0, 2.0, 3.0), 3.0, true));
        assertEquals(0, ClubhouseEvaluationService.percentile(List.of(1.0, 2.0, 3.0), 1.0, true));
        assertEquals(100, ClubhouseEvaluationService.percentile(List.of(1.0, 2.0, 3.0), 1.0, false));
        assertEquals(50, ClubhouseEvaluationService.percentile(List.of(1.0, 2.0, 2.0, 3.0), 2.0, true));
        assertEquals(50, ClubhouseEvaluationService.percentile(List.of(2.0), 2.0, true));
        assertEquals(0, ClubhouseEvaluationService.percentile(List.of(), 2.0, true));
    }

    private void stubCareerPools(List<Map<String, Object>> batting,
            List<Map<String, Object>> pitching, List<Map<String, Object>> fielding) {
        when(statsService.getBattingLeaders(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("pa"), eq("desc"),
                eq(false))).thenReturn(PageResult.of(batting, batting.size()));
        when(statsService.getPitchingLeaders(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("ip"), eq("desc"),
                eq(false))).thenReturn(PageResult.of(pitching, pitching.size()));
        when(statsService.getFieldingLeaders(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("inn"), eq("desc")))
                .thenReturn(PageResult.of(fielding, fielding.size()));
    }

    private void verifyAllTenantPools() {
        verify(statsService).getBattingLeaders(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("pa"), eq("desc"),
                eq(false));
        verify(statsService).getPitchingLeaders(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("ip"), eq("desc"),
                eq(false));
        verify(statsService).getFieldingLeaders(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("inn"), eq("desc"));
    }

    private List<Map<String, Object>> bigBattingPool() {
        return List.of(bigBattingRow(),
                battingRow(11L, "CF", 40, 30, 6, 1, 0, 0, 2, 0, 0, 8, 1, 0));
    }

    private List<Map<String, Object>> bigPitchingPool() {
        return List.of(bigPitchingRow(),
                pitchingRow(11L, 10.0, 6, 1, 4, 0, 8, 1.4));
    }

    private List<Map<String, Object>> bigFieldingPool() {
        return List.of(bigFieldingRow(),
                fieldingRow(11L, "CF", 8, 30.0, 15, 13, 3, 2));
    }

    private Map<String, Object> bigBattingRow() {
        return battingRow(10L, "CF", 60, 50, 18, 5, 1, 2, 8, 1, 1, 30, 8, 1);
    }

    private Map<String, Object> bigPitchingRow() {
        return pitchingRow(10L, 20.0, 8, 2, 6, 1, 18, 1.2);
    }

    private Map<String, Object> bigFieldingRow() {
        return fieldingRow(10L, "CF", 10, 40.0, 20, 18, 2, 4);
    }

    private Map<String, Object> bigCareerStats() {
        return careerStats(bigBattingRow(), bigPitchingRow(), bigFieldingRow());
    }

    private Map<String, Object> careerStats(Map<String, Object> batting,
            Map<String, Object> pitching, Map<String, Object> fielding) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("batting", batting);
        out.put("pitching", pitching);
        out.put("fielding", fielding);
        return out;
    }

    private Map<String, Object> battingRow(Long playerId, String position, int pa, int ab, int h,
            int doubles, int triples, int hr, int bb, int hbp, int sf, int rbi, int sb, int cs) {
        int singles = Math.max(0, h - doubles - triples - hr);
        int totalBases = singles + doubles * 2 + triples * 3 + hr * 4;
        double avg = ab > 0 ? (double) h / ab : 0.0;
        double obp = obpDenominator(ab, bb, hbp, sf) > 0
                ? (double) (h + bb + hbp) / obpDenominator(ab, bb, hbp, sf) : 0.0;
        double slg = ab > 0 ? (double) totalBases / ab : 0.0;
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", playerId);
        row.put("position", position);
        row.put("pa", pa);
        row.put("ab", ab);
        row.put("h", h);
        row.put("rbi", rbi);
        row.put("sb", sb);
        row.put("cs", cs);
        row.put("singles", singles);
        row.put("doubles", doubles);
        row.put("triples", triples);
        row.put("hr", hr);
        row.put("insideParkHr", 0);
        row.put("bb", bb);
        row.put("hbp", hbp);
        row.put("sf", sf);
        row.put("ibb", 0);
        row.put("tb", totalBases);
        row.put("avg", format3(avg));
        row.put("obp", format3(obp));
        row.put("slg", format3(slg));
        row.put("ops", format3(obp + slg));
        return row;
    }

    private int obpDenominator(int ab, int bb, int hbp, int sf) {
        return ab + bb + hbp + sf;
    }

    private Map<String, Object> pitchingRow(Long playerId, double ip, int er, int hr,
            int walks, int hitByPitch, int strikeouts, double whip) {
        double decimalInnings = StatsFormatUtilTestSupport.decimalInnings(ip);
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", playerId);
        row.put("ip", ip);
        row.put("er", er);
        row.put("pitchHr", hr);
        row.put("pitchInsideParkHr", 0);
        row.put("pitchBb", walks);
        row.put("pitchHbp", hitByPitch);
        row.put("pitchSo", strikeouts);
        row.put("pitchH", Math.max(0, (int) Math.round(whip * decimalInnings) - walks));
        row.put("era", format2(decimalInnings > 0 ? 9.0 * er / decimalInnings : 0.0));
        row.put("whip", format2(whip));
        return row;
    }

    private Map<String, Object> fieldingRow(Long playerId, String position, int gp, double inn,
            int po, int assists, int errors, int dp) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", playerId);
        row.put("position", position);
        row.put("gp", gp);
        row.put("inn", inn);
        row.put("tc", po + assists + errors);
        row.put("po", po);
        row.put("a", assists);
        row.put("e", errors);
        row.put("dp", dp);
        return row;
    }

    private static String format3(double value) {
        return String.format("%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String format2(double value) {
        return String.format("%.2f", value);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cards(Map<String, Object> evaluation) {
        return (List<Map<String, Object>>) evaluation.get("cards");
    }

    private Map<String, Object> card(List<Map<String, Object>> cards, String domain) {
        return cards.stream()
                .filter(row -> domain.equals(row.get("domain")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> axes(Map<String, Object> card) {
        return (List<Map<String, Object>>) card.get("axes");
    }

    private List<Object> abbrs(Map<String, Object> card) {
        return axes(card).stream().map(axis -> axis.get("abbr")).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> overall(Map<String, Object> evaluation) {
        return (Map<String, Object>) evaluation.get("overall");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> meta(Map<String, Object> card) {
        Object raw = card.get("meta");
        return raw instanceof Map<?, ?> ? (Map<String, Object>) raw : Map.of();
    }

    /** 测试内独立换算棒球记法局数，避免与生产代码共用实现。 */
    private static final class StatsFormatUtilTestSupport {
        private static double decimalInnings(double ip) {
            int whole = (int) Math.floor(ip);
            int outs = Math.round((float) ((ip - whole) * 10.0));
            return whole + outs / 3.0;
        }
    }
}
