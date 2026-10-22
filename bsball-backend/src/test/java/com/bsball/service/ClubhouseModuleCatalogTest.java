package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseModuleCatalogTest {

    @Test
    @DisplayName("默认模块顺序与现有驾驶舱视觉一致")
    void defaultModulesFollowExistingLayout() {
        List<Map<String, Object>> modules = ClubhouseModuleCatalog.defaultModules();

        assertEquals(9, modules.size());
        assertEquals(List.of("ranks", "evaluation", "milestones", "performance",
                "teams", "schedule", "about", "recentGames", "quickLinks"),
                modules.stream().map(row -> row.get("key")).toList());
        assertEquals(true, modules.get(0).get("enabled"));
    }

    @Test
    @DisplayName("模块目录返回固定宽度元数据")
    void catalogReturnsFixedSpans() {
        List<Map<String, Object>> catalog = ClubhouseModuleCatalog.catalog();

        assertEquals(9, catalog.size());
        assertEquals(12, catalog.get(0).get("span"));
        assertEquals("队内坐标", catalog.get(0).get("label"));
        assertEquals("生涯里程碑", catalog.get(2).get("label"));
        assertEquals(12, catalog.get(2).get("span"));
        assertEquals(8, catalog.get(3).get("span"));
        assertEquals(4, catalog.get(4).get("span"));
    }

    @Test
    @DisplayName("未知模块 key 不命中")
    void unknownModuleIsNull() {
        assertNull(ClubhouseModuleCatalog.find("unknown"));
        assertNull(ClubhouseModuleCatalog.find(null));
    }
}
