package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseModuleCatalogTest {

    @Test
    @DisplayName("默认模块按数组顺序装箱为 v4 画布几何")
    void defaultModulesArePackedOnBoard() {
        List<Map<String, Object>> modules = ClubhouseModuleCatalog.defaultModules();

        assertEquals(9, modules.size());
        assertEquals(List.of("ranks", "evaluation", "milestones", "performance",
                "teams", "schedule", "about", "recentGames", "quickLinks"),
                modules.stream().map(row -> row.get("key")).toList());
        assertGeometry(modules, 0, "ranks", 0, 0, 1216, 220);
        assertGeometry(modules, 1, "evaluation", 0, 236, 1216, 700);
        assertGeometry(modules, 2, "milestones", 0, 952, 1216, 500);
        assertGeometry(modules, 3, "performance", 0, 1468, 800, 500);
        assertGeometry(modules, 4, "teams", 816, 1468, 400, 300);
        assertGeometry(modules, 5, "schedule", 0, 1984, 800, 560);
        assertGeometry(modules, 6, "about", 816, 1984, 400, 280);
        assertGeometry(modules, 7, "recentGames", 0, 2560, 800, 320);
        assertGeometry(modules, 8, "quickLinks", 816, 2560, 400, 220);
        modules.forEach(row -> assertEquals(true, row.get("enabled")));
    }

    @Test
    @DisplayName("默认模块两两不重叠且至少间隔 16px")
    void defaultModulesDoNotOverlap() {
        List<Map<String, Object>> modules = ClubhouseModuleCatalog.defaultModules();

        for (int i = 0; i < modules.size(); i++) {
            for (int j = i + 1; j < modules.size(); j++) {
                assertFalse(ClubhouseBoardLayout.overlaps(placement(modules.get(i)), placement(modules.get(j))));
            }
        }
    }

    @Test
    @DisplayName("模块目录返回像素默认尺寸与最小/最大尺寸")
    void catalogReturnsPixelMetadata() {
        List<Map<String, Object>> catalog = ClubhouseModuleCatalog.catalog();

        assertEquals(9, catalog.size());
        assertEquals("队内坐标", catalog.get(0).get("label"));
        assertEquals("生涯里程碑", catalog.get(2).get("label"));
        assertEquals(List.of("key", "label", "defaultWidth", "defaultHeight",
                "minWidth", "minHeight", "maxWidth", "maxHeight"),
                List.copyOf(catalog.get(0).keySet()));
        assertEquals(1216, catalog.get(0).get("defaultWidth"));
        assertEquals(220, catalog.get(0).get("defaultHeight"));
        assertEquals(600, catalog.get(0).get("minWidth"));
        assertEquals(160, catalog.get(0).get("minHeight"));
        assertEquals(1216, catalog.get(0).get("maxWidth"));
        assertEquals(1600, catalog.get(0).get("maxHeight"));
        assertEquals(800, catalog.get(3).get("defaultWidth"));
        assertEquals(500, catalog.get(3).get("defaultHeight"));
        assertEquals(360, catalog.get(3).get("minWidth"));
        assertEquals(320, catalog.get(3).get("minHeight"));
        assertEquals(400, catalog.get(4).get("defaultWidth"));
        assertEquals(300, catalog.get(4).get("defaultHeight"));
        assertEquals(280, catalog.get(4).get("minWidth"));
        assertEquals(220, catalog.get(4).get("minHeight"));
        assertFalse(catalog.get(0).containsKey("span"));
        assertFalse(catalog.get(0).containsKey("minSpan"));
    }

    @Test
    @DisplayName("旧 12 列宽度转像素公式")
    void legacyWidthConvertsColumnSpans() {
        assertEquals(1216, ClubhouseModuleCatalog.legacyWidth(12));
        assertEquals(805, ClubhouseModuleCatalog.legacyWidth(8));
        assertEquals(600, ClubhouseModuleCatalog.legacyWidth(6));
        assertEquals(395, ClubhouseModuleCatalog.legacyWidth(4));
        assertEquals(87, ClubhouseModuleCatalog.legacyWidth(1));
    }

    @Test
    @DisplayName("未知模块 key 不命中")
    void unknownModuleIsNull() {
        assertNull(ClubhouseModuleCatalog.find("unknown"));
        assertNull(ClubhouseModuleCatalog.find(null));
    }

    private void assertGeometry(List<Map<String, Object>> modules, int index, String key,
            int x, int y, int width, int height) {
        Map<String, Object> row = modules.get(index);
        assertEquals(key, row.get("key"));
        assertEquals(x, row.get("x"));
        assertEquals(y, row.get("y"));
        assertEquals(width, row.get("width"));
        assertEquals(height, row.get("height"));
    }

    private ClubhouseBoardLayout.ModulePlacement placement(Map<String, Object> row) {
        return new ClubhouseBoardLayout.ModulePlacement(
                (String) row.get("key"),
                Boolean.TRUE.equals(row.get("enabled")),
                (Integer) row.get("x"),
                (Integer) row.get("y"),
                (Integer) row.get("width"),
                (Integer) row.get("height"));
    }
}
