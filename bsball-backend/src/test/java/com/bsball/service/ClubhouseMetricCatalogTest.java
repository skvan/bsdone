package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseMetricCatalogTest {

    @Test
    @DisplayName("默认配置包含九个模块并升级到 v3-modular")
    void defaultConfigContainsModules() {
        Map<String, Object> config = ClubhouseMetricCatalog.defaultConfig();

        assertEquals(3, config.get("version"));
        assertEquals("v3-modular", config.get("layout"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) config.get("modules");
        assertEquals(ClubhouseModuleCatalog.defaultModules(), modules);
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) config.get("milestones");
        assertEquals(6, ((List<?>) milestones.get("metricKeys")).size());
    }

    @Test
    @DisplayName("默认队内排名为打击率、安打、上垒率、打点，不包含全垒打")
    void defaultRanksExcludeHomeRuns() {
        Map<String, Object> config = ClubhouseMetricCatalog.defaultConfig();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranks = (List<Map<String, Object>>) config.get("ranks");

        assertEquals(4, ranks.size());
        assertEquals(List.of("batting.avg", "batting.h", "batting.obp", "batting.rbi"),
                ranks.stream().map(row -> row.get("metricKey")).toList());
        assertFalse(ClubhouseMetricCatalog.isRankable("batting.hr"));
    }

    @Test
    @DisplayName("图表类型按指标兼容：比率指标不做环形图，安打构成只做环形图")
    void chartCompatibility() {
        assertTrue(ClubhouseMetricCatalog.supportsViz("batting.avg", "line"));
        assertTrue(ClubhouseMetricCatalog.supportsViz("batting.avg", "bar"));
        assertTrue(ClubhouseMetricCatalog.supportsViz("batting.avg", "kpi"));
        assertFalse(ClubhouseMetricCatalog.supportsViz("batting.avg", "donut"));
        assertTrue(ClubhouseMetricCatalog.supportsViz("batting.hitTypes", "donut"));
        assertFalse(ClubhouseMetricCatalog.supportsViz("batting.hitTypes", "line"));
        assertFalse(ClubhouseMetricCatalog.supportsViz("unknown.metric", "line"));
    }

    @Test
    @DisplayName("指标目录按球员数据可用性标记投打指标")
    void catalogMarksAvailability() {
        List<Map<String, Object>> rows = ClubhouseMetricCatalog.catalog(true, false);
        Map<String, Object> avg = rows.stream()
                .filter(row -> "batting.avg".equals(row.get("key")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> era = rows.stream()
                .filter(row -> "pitching.era".equals(row.get("key")))
                .findFirst()
                .orElseThrow();

        assertEquals(true, avg.get("available"));
        assertEquals(false, era.get("available"));
    }
}
