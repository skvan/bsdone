package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
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
    @DisplayName("打击雷达按同队同赛事计算百分位，并保留原始值")
    void buildsBattingRadarWithPercentiles() {
        stubBattingPool();
        stubFieldingPool();
        stubPitchingPool();

        Map<String, Object> evaluation = service.build(10L, 1L, 2L, "BASEBALL", allDomains());
        List<Map<String, Object>> cards = cards(evaluation);

        assertEquals(3, cards.size());
        Map<String, Object> batting = card(cards, "batting");
        assertEquals(true, batting.get("available"));
        assertEquals("打击评价", batting.get("title"));
        assertEquals(60, batting.get("score"));
        assertEquals("C+", batting.get("grade"));
        assertEquals(5, batting.get("sampleSize"));
        List<Map<String, Object>> axes = axes(batting);
        assertEquals(5, axes.size());
        assertEquals("AVG", axes.get(0).get("abbr"));
        assertEquals(100, axes.get(0).get("percentile"));
        assertEquals(50, axes.get(1).get("percentile"));
        assertEquals(50, axes.get(2).get("percentile"));
        assertEquals(100, axes.get(3).get("percentile"));
        assertEquals(0, axes.get(4).get("percentile"));
        assertNotNull(axes.get(0).get("value"));
    }

    @Test
    @DisplayName("守备与投球雷达使用职业口径派生率值")
    void buildsFieldingAndPitchingRadar() {
        stubBattingPool();
        stubFieldingPool();
        stubPitchingPool();

        Map<String, Object> evaluation = service.build(10L, 1L, 2L, "BASEBALL", allDomains());
        List<Map<String, Object>> cards = cards(evaluation);

        Map<String, Object> fielding = card(cards, "fielding");
        assertEquals(true, fielding.get("available"));
        assertEquals(5, axes(fielding).size());
        assertEquals("FPCT", axes(fielding).get(0).get("abbr"));
        assertEquals("RF/9", axes(fielding).get(4).get("abbr"));

        Map<String, Object> pitching = card(cards, "pitching");
        assertEquals(true, pitching.get("available"));
        assertEquals(5, axes(pitching).size());
        assertEquals("ERA", axes(pitching).get(0).get("abbr"));
        assertEquals(100, axes(pitching).get(0).get("percentile"));
        assertEquals(100, axes(pitching).get(1).get("percentile"));
        assertEquals(0, axes(pitching).get(3).get("percentile"));
        assertEquals("3.2", axes(pitching).get(4).get("value"));
        verifyLeaderPoolsUseSelectedScope();
    }

    @Test
    @DisplayName("未选择球队或赛事时不生成雷达值，但保留守备占位卡")
    void withoutScopeKeepsUnavailableCard() {
        Map<String, Object> stats = Map.of(
                "batting", Map.of("pa", 20),
                "pitching", Map.of(),
                "fielding", Map.of());

        Map<String, Object> evaluation = service.build(10L, null, null, "BASEBALL", stats);
        List<Map<String, Object>> cards = cards(evaluation);

        assertEquals(false, evaluation.get("available"));
        assertEquals(2, cards.size());
        assertEquals(false, card(cards, "batting").get("available"));
        assertEquals("请先选择代表球队与赛事", card(cards, "batting").get("reason"));
        assertEquals(false, card(cards, "fielding").get("available"));
        assertEquals("暂无守备数据", card(cards, "fielding").get("reason"));
        assertFalse(cards.stream().anyMatch(row -> "pitching".equals(row.get("domain"))));
    }

    @Test
    @DisplayName("同队同赛事不足 3 人时不给雷达值")
    void sampleBelowMinimumReturnsReason() {
        List<Map<String, Object>> rows = List.of(
                battingRow(10L, 0.400, 0.260, 0.400, 40, 0),
                battingRow(11L, 0.350, 0.300, 0.500, 30, 10));
        when(statsService.getBattingLeaders(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(PageResult.of(rows, rows.size()));

        Map<String, Object> evaluation = service.build(10L, 1L, 2L, "BASEBALL",
                Map.of("batting", Map.of("pa", 20)));
        Map<String, Object> batting = card(cards(evaluation), "batting");

        assertEquals(false, batting.get("available"));
        assertTrue(String.valueOf(batting.get("reason")).contains("样本不足"));
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

    private void stubBattingPool() {
        List<Map<String, Object>> rows = List.of(
                battingRow(10L, 0.400, 0.260, 0.400, 40, 0),
                battingRow(11L, 0.350, 0.300, 0.500, 30, 10),
                battingRow(12L, 0.300, 0.280, 0.450, 20, 8),
                battingRow(13L, 0.250, 0.240, 0.350, 10, 5),
                battingRow(14L, 0.200, 0.220, 0.300, 5, 1));
        when(statsService.getBattingLeaders(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(PageResult.of(rows, rows.size()));
    }

    private void stubFieldingPool() {
        List<Map<String, Object>> rows = List.of(
                fieldingRow(10L, 10, 5, 1, 2),
                fieldingRow(11L, 9, 4, 1, 1),
                fieldingRow(12L, 8, 4, 1, 1),
                fieldingRow(13L, 7, 3, 1, 0),
                fieldingRow(14L, 6, 3, 1, 0));
        when(statsService.getFieldingLeaders(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(PageResult.of(rows, rows.size()));
    }

    private void stubPitchingPool() {
        List<Map<String, Object>> rows = List.of(
                pitchingRow(10L, 2.00, 1.00, 12, 6, 3.2),
                pitchingRow(11L, 3.00, 1.10, 10, 2, 28.0),
                pitchingRow(12L, 4.00, 1.20, 8, 4, 25.0),
                pitchingRow(13L, 5.00, 1.30, 6, 5, 20.0),
                pitchingRow(14L, 6.00, 1.40, 4, 6, 15.0));
        when(statsService.getPitchingLeaders(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(PageResult.of(rows, rows.size()));
    }

    private Map<String, Object> battingRow(Long playerId, double avg, double obp, double slg,
            int rbi, int sb) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", playerId);
        row.put("pa", 20);
        row.put("ab", 18);
        row.put("h", Math.round(avg * 18));
        row.put("tb", Math.round(slg * 18));
        row.put("avg", avg);
        row.put("obp", obp);
        row.put("slg", slg);
        row.put("rbi", rbi);
        row.put("sb", sb);
        return row;
    }

    private Map<String, Object> fieldingRow(Long playerId, int po, int a, int e, int dp) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", playerId);
        row.put("gp", 5);
        row.put("inn", 9.0);
        row.put("po", po);
        row.put("a", a);
        row.put("e", e);
        row.put("dp", dp);
        row.put("tc", po + a + e);
        return row;
    }

    private Map<String, Object> pitchingRow(Long playerId, double era, double whip,
            int strikeouts, int walks, double innings) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("playerId", playerId);
        row.put("ip", innings);
        row.put("era", era);
        row.put("whip", whip);
        row.put("pitchSo", strikeouts);
        row.put("pitchBb", walks);
        return row;
    }

    private void verifyLeaderPoolsUseSelectedScope() {
        verify(statsService).getBattingLeaders(eq(2L), isNull(), isNull(), eq(List.of(1L)), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("pa"), eq("desc"), eq(false));
        verify(statsService).getPitchingLeaders(eq(2L), isNull(), isNull(), eq(List.of(1L)), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("ip"), eq("desc"), eq(false));
        verify(statsService).getFieldingLeaders(eq(2L), isNull(), isNull(), eq(List.of(1L)), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq("BASEBALL"), eq(1), eq(500), eq("tcPct"), eq("desc"));
    }

    private Map<String, Object> allDomains() {
        return Map.of(
                "batting", Map.of("pa", 20),
                "pitching", Map.of("ip", 30.0),
                "fielding", Map.of("gp", 5, "tc", 16));
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
}
