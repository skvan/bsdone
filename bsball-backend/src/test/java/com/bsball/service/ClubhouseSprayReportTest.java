package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.model.entity.HitSpray;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseSprayReportTest {

    @Test
    @DisplayName("外野按极角分 7 条扇带，边界落在右侧区间")
    void classifyOutfieldLanesByAngle() {
        assertEquals("MI", ClubhouseSprayReport.classify(240, 100, "CF"));
        assertEquals("MI", ClubhouseSprayReport.classify(240 - 40, 404, null));
        assertEquals("LC", ClubhouseSprayReport.classify(180, 404, null));
        assertEquals("LS", ClubhouseSprayReport.classify(177, 454, "LF"));
        assertEquals("LL", ClubhouseSprayReport.classify(140, 524, "LF"));
        assertEquals("RC", ClubhouseSprayReport.classify(300, 404, "RF"));
        assertEquals("RS", ClubhouseSprayReport.classify(334, 404, "RF"));
        assertEquals("RL", ClubhouseSprayReport.classify(340, 524, "RF"));
    }

    @Test
    @DisplayName("越界角度与内野区域")
    void classifyClampsAndKeepsInfield() {
        assertEquals("RL", ClubhouseSprayReport.classify(700, 540, null));
        assertEquals("LL", ClubhouseSprayReport.classify(-220, 540, null));
        assertEquals("IF", ClubhouseSprayReport.classify(240, 480, "SS"));
        assertEquals("IF", ClubhouseSprayReport.classify(240, 480, "1b"));
        assertEquals("IF", ClubhouseSprayReport.classify(240, 480, "IF"));
        assertEquals("MI", ClubhouseSprayReport.classify(240, 480, "LF"));
    }

    @Test
    @DisplayName("安打结果码识别")
    void detectsHitCodes() {
        assertTrue(ClubhouseSprayReport.isHit("H1"));
        assertTrue(ClubhouseSprayReport.isHit("h2"));
        assertTrue(ClubhouseSprayReport.isHit("HR"));
        assertTrue(ClubhouseSprayReport.isHit("IPHR"));
        assertFalse(ClubhouseSprayReport.isHit("OUT_F"));
        assertFalse(ClubhouseSprayReport.isHit("E"));
        assertFalse(ClubhouseSprayReport.isHit(null));
    }

    @Test
    @DisplayName("汇总分区安打/击球、场次与落点")
    void buildsZoneTallyAndPoints() {
        List<HitSpray> rows = List.of(
                spray(240d, 300d, "H1", "CF", 1L),
                spray(120d, 400d, "OUT_F", "LF", 1L),
                spray(334d, 404d, "H2", "RF", 2L));

        Map<String, Object> report = ClubhouseSprayReport.build(rows);

        Map<?, ?> summary = (Map<?, ?>) report.get("summary");
        assertEquals(3, summary.get("balls"));
        assertEquals(2, summary.get("hits"));
        assertEquals(2, summary.get("games"));

        List<Map<String, Object>> zones = castZones(report.get("zones"));
        assertEquals(8, zones.size());
        assertEquals("IF", zones.get(0).get("code"));
        assertEquals("LL", zones.get(1).get("code"));
        assertEquals("LS", zones.get(2).get("code"));
        assertEquals(0, zones.get(2).get("hits"));
        assertEquals(1, zones.get(2).get("balls"));
        assertEquals("MI", zones.get(4).get("code"));
        assertEquals(1, zones.get(4).get("hits"));
        assertEquals(1, zones.get(4).get("balls"));
        assertEquals("RS", zones.get(6).get("code"));
        assertEquals(1, zones.get(6).get("hits"));
        assertEquals(1, zones.get(6).get("balls"));

        List<Map<String, Object>> points = castZones(report.get("points"));
        assertEquals(3, points.size());
        assertEquals("MI", points.get(0).get("zone"));
        assertEquals(true, points.get(0).get("hit"));
        assertEquals(240d, points.get(0).get("x"));
        assertEquals(false, points.get(1).get("hit"));
    }

    @Test
    @DisplayName("落点最多保留最近 600 个，分区计数仍按全量")
    void capsPointsButKeepsFullTally() {
        List<HitSpray> rows = new ArrayList<>();
        for (int i = 0; i < 601; i++) {
            rows.add(spray(240d, 300d, i == 0 ? "OUT_F" : "H1", "CF", 1L));
        }
        Map<String, Object> report = ClubhouseSprayReport.build(rows);
        assertEquals(600, castZones(report.get("points")).size());
        Map<?, ?> summary = (Map<?, ?>) report.get("summary");
        assertEquals(601, summary.get("balls"));
        assertEquals(600, summary.get("hits"));
    }

    private static HitSpray spray(Double x, Double y, String resultCode, String zone, Long gameId) {
        HitSpray row = new HitSpray();
        row.setSprayX(BigDecimal.valueOf(x));
        row.setSprayY(BigDecimal.valueOf(y));
        row.setResultCode(resultCode);
        row.setSprayZone(zone);
        row.setGameId(gameId);
        return row;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castZones(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
