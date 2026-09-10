/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.stats.PlayerStatDrillFieldingMetric
 */
package com.bsball.stats;

import java.util.Locale;
import java.util.Map;

public enum PlayerStatDrillFieldingMetric {
    GP("1::integer"),
    INN("COALESCE(s.def_inn, 0)"),
    TC("(COALESCE(s.tc, COALESCE(s.po,0)+COALESCE(s.a,0)+COALESCE(s.e,0)))::integer"),
    PO("COALESCE(s.po, 0)"),
    A("COALESCE(s.a, 0)"),
    E("COALESCE(s.e, 0)"),
    PB("(CASE WHEN s.position = 'C' THEN COALESCE(s.pb, 0) ELSE 0 END)::integer"),
    CATCHER_CS("(CASE WHEN s.position = 'C' THEN COALESCE(s.catcher_cs, 0) ELSE 0 END)::integer"),
    TC_PCT("CASE WHEN COALESCE(s.tc, COALESCE(s.po,0)+COALESCE(s.a,0)+COALESCE(s.e,0)) > 0 THEN ROUND(100.0 * (COALESCE(s.po,0) + COALESCE(s.a,0))::numeric / NULLIF(COALESCE(s.tc, COALESCE(s.po,0)+COALESCE(s.a,0)+COALESCE(s.e,0)),0), 1) ELSE NULL END");

    private final String valueSql;
    private static final Map<String, PlayerStatDrillFieldingMetric> ALIASES;

    private PlayerStatDrillFieldingMetric(String valueSql) {
        this.valueSql = valueSql;
    }

    public String getValueSql() {
        return this.valueSql;
    }

    public static PlayerStatDrillFieldingMetric fromApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if ("tc%".equals(lower) || "tcpct".equals(lower)) {
            return TC_PCT;
        }
        String k = lower.replace("%", "").replace("+", "");
        return (PlayerStatDrillFieldingMetric)ALIASES.get(k);
    }

    static {
        ALIASES = Map.ofEntries((Map.Entry[])new Map.Entry[]{Map.entry((Object)"gp", (Object)GP), Map.entry((Object)"inn", (Object)INN), Map.entry((Object)"tc", (Object)TC), Map.entry((Object)"po", (Object)PO), Map.entry((Object)"a", (Object)A), Map.entry((Object)"e", (Object)E), Map.entry((Object)"pb", (Object)PB), Map.entry((Object)"catchercs", (Object)CATCHER_CS), Map.entry((Object)"cs", (Object)CATCHER_CS), Map.entry((Object)"tcpct", (Object)TC_PCT)});
    }
}

