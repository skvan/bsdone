package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.service.ClubhouseBoardLayout.ModulePlacement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseBoardLayoutTest {

    @Test
    @DisplayName("满宽模块逐行排列，纵向间距 16")
    void packsFullWidthRowsWithGap() {
        List<ModulePlacement> rows = ClubhouseBoardLayout.pack(List.of(
                m("ranks", 1216, 220),
                m("evaluation", 1216, 620),
                m("milestones", 1216, 500)));

        assertEquals(0, rows.get(0).x());
        assertEquals(0, rows.get(0).y());
        assertEquals(236, rows.get(1).y());
        assertEquals(872, rows.get(2).y());
    }

    @Test
    @DisplayName("800 与 400 模块并排，水平间距 16")
    void packsPairedModulesWithSixteenGap() {
        List<ModulePlacement> rows = ClubhouseBoardLayout.pack(List.of(
                m("performance", 800, 460), m("teams", 400, 300)));

        assertEquals(0, rows.get(0).x());
        assertEquals(816, rows.get(1).x());
        assertEquals(0, rows.get(1).y());
        assertFalse(ClubhouseBoardLayout.overlaps(rows.get(0), rows.get(1)));
    }

    @Test
    @DisplayName("16px 间距合法，15px 视为重叠")
    void gapBoundaryIsSixteenPixels() {
        ModulePlacement left = new ModulePlacement("performance", true, 0, 0, 800, 460);

        assertFalse(ClubhouseBoardLayout.overlaps(
                left, new ModulePlacement("teams", true, 816, 0, 400, 300)));
        assertTrue(ClubhouseBoardLayout.overlaps(
                left, new ModulePlacement("teams", true, 815, 0, 400, 300)));
        assertFalse(ClubhouseBoardLayout.overlaps(
                left, new ModulePlacement("schedule", true, 0, 476, 800, 560)));
        assertTrue(ClubhouseBoardLayout.overlaps(
                left, new ModulePlacement("schedule", true, 0, 475, 800, 560)));
    }

    @Test
    @DisplayName("重叠的启用模块按数组顺序下移到所有已接受模块底部")
    void resolvesOverlapByMovingLaterModuleBelow() {
        List<ModulePlacement> rows = ClubhouseBoardLayout.resolveOverlaps(List.of(
                new ModulePlacement("performance", true, 0, 0, 800, 460),
                new ModulePlacement("teams", true, 0, 0, 400, 300)));

        assertEquals(476, rows.get(1).y());
        assertEquals(0, rows.get(1).x());
        assertFalse(ClubhouseBoardLayout.overlaps(rows.get(0), rows.get(1)));
    }

    @Test
    @DisplayName("下移基于所有已接受模块的最大下边缘")
    void resolvesOverlapBelowAllAcceptedModules() {
        List<ModulePlacement> rows = ClubhouseBoardLayout.resolveOverlaps(List.of(
                new ModulePlacement("ranks", true, 0, 0, 1216, 220),
                new ModulePlacement("evaluation", true, 0, 0, 1216, 620),
                new ModulePlacement("milestones", true, 10, 300, 680, 360)));

        assertEquals(236, rows.get(1).y());
        assertEquals(872, rows.get(2).y());
        assertEquals(0, rows.get(2).x());
        for (int i = 0; i < rows.size(); i++) {
            for (int j = i + 1; j < rows.size(); j++) {
                assertFalse(ClubhouseBoardLayout.overlaps(rows.get(i), rows.get(j)));
            }
        }
    }

    @Test
    @DisplayName("禁用模块不参与重叠检测但保留几何")
    void disabledModulesAreIgnoredButPreserveGeometry() {
        List<ModulePlacement> rows = ClubhouseBoardLayout.resolveOverlaps(List.of(
                new ModulePlacement("teams", false, 32, 48, 400, 300),
                new ModulePlacement("performance", true, 0, 0, 800, 460)));

        assertEquals(new ModulePlacement("teams", false, 32, 48, 400, 300), rows.get(0));
        assertEquals(0, rows.get(1).x());
        assertEquals(0, rows.get(1).y());
    }

    @Test
    @DisplayName("placeBelow 左对齐放到所有模块底部并留出 16px")
    void placesBelowAllModules() {
        List<ModulePlacement> placed = List.of(
                new ModulePlacement("ranks", true, 10, 20, 400, 300),
                new ModulePlacement("evaluation", true, 0, 100, 800, 460));

        ModulePlacement below = ClubhouseBoardLayout.placeBelow(m("quickLinks", 400, 220), placed);

        assertEquals(0, below.x());
        assertEquals(576, below.y());
        assertFalse(ClubhouseBoardLayout.overlaps(below, placed.get(0)));
        assertFalse(ClubhouseBoardLayout.overlaps(below, placed.get(1)));
    }

    @Test
    @DisplayName("placeBelow 空列表放到原点")
    void placesBelowEmptyAtOrigin() {
        ModulePlacement below = ClubhouseBoardLayout.placeBelow(m("ranks", 1216, 220), List.of());

        assertEquals(0, below.x());
        assertEquals(0, below.y());
    }

    @Test
    @DisplayName("超宽模块装箱时收敛到画布宽度")
    void packClampsWidthToBoardWidth() {
        List<ModulePlacement> rows = ClubhouseBoardLayout.pack(List.of(m("wide", 1400, 300)));

        assertEquals(1216, rows.get(0).width());
        assertEquals(0, rows.get(0).x());
    }

    @Test
    @DisplayName("maxBottom 取最大下边缘，空列表为 0")
    void maxBottomReturnsLargestBottomEdge() {
        assertEquals(0, ClubhouseBoardLayout.maxBottom(List.of()));
        assertEquals(560, ClubhouseBoardLayout.maxBottom(List.of(
                new ModulePlacement("ranks", true, 0, 0, 1216, 220),
                new ModulePlacement("performance", true, 0, 100, 800, 460))));
    }

    private ModulePlacement m(String key, int width, int height) {
        return new ModulePlacement(key, true, 0, 0, width, height);
    }
}
