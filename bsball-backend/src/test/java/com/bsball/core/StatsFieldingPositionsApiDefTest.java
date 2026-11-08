package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("按位置守备榜 API 注册")
class StatsFieldingPositionsApiDefTest {

    @Test
    @DisplayName("apiDefs 恰好注册一次 GET /stats/leaders/fielding-positions")
    void registeredOnce() {
        List<DbMigrationRunner.ApiDef> defs = DbMigrationRunner.apiDefs();
        long count = defs.stream()
                .filter(def -> "/stats/leaders/fielding-positions".equals(def.path()) && "GET".equals(def.method()))
                .count();
        assertEquals(1L, count);
    }
}
