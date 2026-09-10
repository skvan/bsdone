/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.stats.PlayerStatDrillBattingMetric
 */
package com.bsball.stats;

import java.util.Locale;
import java.util.Map;

public enum PlayerStatDrillBattingMetric {
    GP("1::integer"),
    PA("COALESCE(s.pa, 0)"),
    AB("COALESCE(s.ab, 0)"),
    R("COALESCE(s.r, 0)"),
    H("COALESCE(s.h, 0)"),
    RBI("COALESCE(s.rbi, 0)"),
    HR("COALESCE(s.hr, 0)"),
    INSIDE_PARK_HR("COALESCE(s.inside_park_hr, 0)"),
    SB("COALESCE(s.sb, 0)"),
    BB_HP("COALESCE(s.bb_hp, 0)"),
    SO("COALESCE(s.so, 0)"),
    DOUBLES("COALESCE(s.doubles, 0)"),
    TRIPLES("COALESCE(s.triples, 0)"),
    E("COALESCE(s.e, 0)"),
    GDP("COALESCE(s.gdp, 0)"),
    SH("COALESCE(s.sh, 0)"),
    SF("COALESCE(s.sf, 0)"),
    BB("COALESCE(s.bb, 0)"),
    IBB("COALESCE(s.ibb, 0)"),
    HBP("COALESCE(s.hbp, 0)"),
    CS("COALESCE(s.cs, 0)"),
    SINGLES("(GREATEST(0, COALESCE(s.h,0) - COALESCE(s.doubles,0) - COALESCE(s.triples,0) - COALESCE(s.hr,0) - COALESCE(s.inside_park_hr,0)))::integer"),
    TB("((GREATEST(0, COALESCE(s.h,0) - COALESCE(s.doubles,0) - COALESCE(s.triples,0) - COALESCE(s.hr,0) - COALESCE(s.inside_park_hr,0))) + COALESCE(s.doubles,0)*2 + COALESCE(s.triples,0)*3 + COALESCE(s.hr,0)*4 + COALESCE(s.inside_park_hr,0)*4)::integer"),
    SH_SF("(COALESCE(s.sh,0) + COALESCE(s.sf,0))::integer"),
    AVG("CASE WHEN COALESCE(s.ab,0) > 0 THEN ROUND((COALESCE(s.h,0)::numeric / NULLIF(s.ab,0)), 3) ELSE NULL END"),
    OBP("CASE WHEN COALESCE(s.pa,0) > 0 THEN ROUND(((COALESCE(s.h,0) + (CASE WHEN COALESCE(s.bb,0) + COALESCE(s.hbp,0) > 0 THEN COALESCE(s.bb,0) + COALESCE(s.hbp,0) ELSE COALESCE(s.bb_hp,0) END))::numeric / NULLIF(s.pa,0)), 3) ELSE NULL END"),
    SLG("CASE WHEN COALESCE(s.ab,0) > 0 THEN ROUND(((GREATEST(0, COALESCE(s.h,0) - COALESCE(s.doubles,0) - COALESCE(s.triples,0) - COALESCE(s.hr,0) - COALESCE(s.inside_park_hr,0))) + COALESCE(s.doubles,0)*2 + COALESCE(s.triples,0)*3 + COALESCE(s.hr,0)*4 + COALESCE(s.inside_park_hr,0)*4)::numeric / NULLIF(s.ab,0), 3) ELSE NULL END"),
    OPS("CASE WHEN COALESCE(s.pa,0) > 0 AND COALESCE(s.ab,0) > 0 THEN ROUND(((COALESCE(s.h,0) + (CASE WHEN COALESCE(s.bb,0) + COALESCE(s.hbp,0) > 0 THEN COALESCE(s.bb,0) + COALESCE(s.hbp,0) ELSE COALESCE(s.bb_hp,0) END))::numeric / NULLIF(s.pa,0)) + ((GREATEST(0, COALESCE(s.h,0) - COALESCE(s.doubles,0) - COALESCE(s.triples,0) - COALESCE(s.hr,0) - COALESCE(s.inside_park_hr,0))) + COALESCE(s.doubles,0)*2 + COALESCE(s.triples,0)*3 + COALESCE(s.hr,0)*4 + COALESCE(s.inside_park_hr,0)*4)::numeric / NULLIF(s.ab,0), 3) ELSE NULL END"),
    SB_PCT("CASE WHEN (COALESCE(s.sb,0) + COALESCE(s.cs,0)) > 0 THEN ROUND(100.0 * COALESCE(s.sb,0)::numeric / NULLIF(COALESCE(s.sb,0) + COALESCE(s.cs,0), 0), 1) ELSE NULL END");

    private final String valueSql;
    private static final Map<String, PlayerStatDrillBattingMetric> ALIASES;

    private PlayerStatDrillBattingMetric(String valueSql) {
        this.valueSql = valueSql;
    }

    public String getValueSql() {
        return this.valueSql;
    }

    public static PlayerStatDrillBattingMetric fromApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if ("sb%".equals(t)) {
            return SB_PCT;
        }
        String k = t.replace("+", "").replace("-", "").replace("%", "");
        if ("shsf".equals(k)) {
            return SH_SF;
        }
        if ("bbhp".equals(k)) {
            return BB_HP;
        }
        return (PlayerStatDrillBattingMetric)ALIASES.get(k);
    }

    static {
        ALIASES = Map.ofEntries((Map.Entry[])new Map.Entry[]{Map.entry((Object)"gp", (Object)GP), Map.entry((Object)"pa", (Object)PA), Map.entry((Object)"ab", (Object)AB), Map.entry((Object)"r", (Object)R), Map.entry((Object)"h", (Object)H), Map.entry((Object)"rbi", (Object)RBI), Map.entry((Object)"hr", (Object)HR), Map.entry((Object)"insideparkhr", (Object)INSIDE_PARK_HR), Map.entry((Object)"iphr", (Object)INSIDE_PARK_HR), Map.entry((Object)"sb", (Object)SB), Map.entry((Object)"bbhp", (Object)BB_HP), Map.entry((Object)"so", (Object)SO), Map.entry((Object)"doubles", (Object)DOUBLES), Map.entry((Object)"triples", (Object)TRIPLES), Map.entry((Object)"e", (Object)E), Map.entry((Object)"gdp", (Object)GDP), Map.entry((Object)"sh", (Object)SH), Map.entry((Object)"sf", (Object)SF), Map.entry((Object)"bb", (Object)BB), Map.entry((Object)"ibb", (Object)IBB), Map.entry((Object)"hbp", (Object)HBP), Map.entry((Object)"cs", (Object)CS), Map.entry((Object)"singles", (Object)SINGLES), Map.entry((Object)"1b", (Object)SINGLES), Map.entry((Object)"tb", (Object)TB), Map.entry((Object)"shsf", (Object)SH_SF), Map.entry((Object)"avg", (Object)AVG), Map.entry((Object)"obp", (Object)OBP), Map.entry((Object)"slg", (Object)SLG), Map.entry((Object)"ops", (Object)OPS), Map.entry((Object)"sbpct", (Object)SB_PCT), Map.entry((Object)"sb%", (Object)SB_PCT)});
    }
}

