package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubhouseMilestoneCatalogTest {

    @Test
    @DisplayName("默认里程碑指标为 6 项且顺序固定")
    void defaultKeys() {
        assertEquals(List.of("batting.h", "batting.rbi", "batting.sb", "batting.gp",
                "pitching.ip", "pitching.so"), ClubhouseMilestoneCatalog.defaultKeys());
    }

    @Test
    @DisplayName("里程碑目录包含 11 项并带阈值")
    void catalogHasThresholds() {
        List<Map<String, Object>> catalog = ClubhouseMilestoneCatalog.catalog();

        assertEquals(11, catalog.size());
        Map<String, Object> hits = catalog.get(0);
        assertEquals("batting.h", hits.get("key"));
        assertEquals("安打", hits.get("label"));
        assertEquals(List.of(1, 25, 50, 100, 200), hits.get("thresholds"));
    }

    @Test
    @DisplayName("未知里程碑指标不命中")
    void unknownMetric() {
        assertNull(ClubhouseMilestoneCatalog.find("unknown"));
        assertNull(ClubhouseMilestoneCatalog.find(null));
    }
}
