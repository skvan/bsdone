/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.stats.PlayerStatDrillPitchingMetric
 */
package com.bsball.stats;

import java.util.Locale;
import java.util.Map;

/*
 * Exception performing whole class analysis ignored.
 */
public enum PlayerStatDrillPitchingMetric {
    GP("1::integer"),
    IP("COALESCE(s.ip, 0)::double precision"),
    ER("COALESCE(s.er, 0)"),
    PITCH_H("COALESCE(s.pitchh, 0)"),
    PITCH_BB_HP("COALESCE(s.pitch_bb_hp, 0)"),
    PITCH_SO("COALESCE(s.pitch_so, 0)"),
    PITCH_HR("COALESCE(s.pitch_hr, 0)"),
    PITCH_INSIDE_PARK_HR("COALESCE(s.pitch_inside_park_hr, 0)"),
    GS("COALESCE(s.gs, 0)"),
    SVO("COALESCE(s.svo, 0)"),
    CG("COALESCE(s.cg, 0)"),
    PG("COALESCE(s.pg, 0)"),
    W("COALESCE(s.w, 0)"),
    L("COALESCE(s.l, 0)"),
    SV("COALESCE(s.sv, 0)"),
    HLD("COALESCE(s.hld, 0)"),
    PITCH_PA("COALESCE(s.pitch_pa, 0)"),
    PITCH_BF("COALESCE(s.pitch_bf, 0)"),
    NP("COALESCE(s.np, 0)"),
    PITCH_BB("COALESCE(s.pitch_bb, 0)"),
    PITCH_IBB("COALESCE(s.pitch_ibb, 0)"),
    PITCH_HBP("COALESCE(s.pitch_hbp, 0)"),
    WP("COALESCE(s.wp, 0)"),
    BK("COALESCE(s.bk, 0)"),
    PITCH_R("COALESCE(s.pitchr, 0)"),
    GO("COALESCE(s.go, 0)"),
    FO("COALESCE(s.fo, 0)"),
    PITCH_AB("COALESCE(s.pitch_ab, GREATEST(0, COALESCE(s.pitch_bf,0) - COALESCE(s.pitch_bb,0) - COALESCE(s.pitch_ibb,0) - COALESCE(s.pitch_hbp,0)))::integer"),
    ERA(PlayerStatDrillPitchingMetric.drillDownEraSql()),
    WHIP(PlayerStatDrillPitchingMetric.drillDownWhipSql());

    private final String valueSql;
    private static final Map<String, PlayerStatDrillPitchingMetric> ALIASES;

    private PlayerStatDrillPitchingMetric(String valueSql) {
        this.valueSql = valueSql;
    }

    public String getValueSql() {
        return this.valueSql;
    }

    private static String drillDownPitchIpOutsSql() {
        return "(CAST(FLOOR(CAST(COALESCE(s.ip, 0) AS NUMERIC)) AS INTEGER) * 3 + LEAST(2, GREATEST(0, CAST(ROUND((CAST(COALESCE(s.ip, 0) AS NUMERIC) - FLOOR(CAST(COALESCE(s.ip, 0) AS NUMERIC))) * 10.0) AS INTEGER))))";
    }

    private static String drillDownEraSql() {
        String outs = PlayerStatDrillPitchingMetric.drillDownPitchIpOutsSql();
        return "CASE WHEN " + outs + " > 0 THEN ROUND(CAST(COALESCE(s.er, 0) AS NUMERIC) * 27.0 / NULLIF(CAST(" + outs + " AS NUMERIC), 0), 2) ELSE NULL END";
    }

    private static String drillDownWhipSql() {
        String outs = PlayerStatDrillPitchingMetric.drillDownPitchIpOutsSql();
        return "CASE WHEN " + outs + " > 0 THEN ROUND((CAST(COALESCE(s.pitchh, 0) AS NUMERIC) + CAST(COALESCE(s.pitch_bb_hp, 0) AS NUMERIC)) * 3.0 / NULLIF(CAST(" + outs + " AS NUMERIC), 0), 2) ELSE NULL END";
    }

    public static PlayerStatDrillPitchingMetric fromApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String k = raw.trim().toLowerCase(Locale.ROOT).replace("+", "");
        return (PlayerStatDrillPitchingMetric)ALIASES.get(k);
    }

    static {
        ALIASES = Map.ofEntries((Map.Entry[])new Map.Entry[]{Map.entry((Object)"gp", (Object)GP), Map.entry((Object)"ip", (Object)IP), Map.entry((Object)"er", (Object)ER), Map.entry((Object)"pitchh", (Object)PITCH_H), Map.entry((Object)"pitchbbhp", (Object)PITCH_BB_HP), Map.entry((Object)"pitchso", (Object)PITCH_SO), Map.entry((Object)"pitchhr", (Object)PITCH_HR), Map.entry((Object)"pitchinsideparkhr", (Object)PITCH_INSIDE_PARK_HR), Map.entry((Object)"iphr", (Object)PITCH_INSIDE_PARK_HR), Map.entry((Object)"gs", (Object)GS), Map.entry((Object)"svo", (Object)SVO), Map.entry((Object)"cg", (Object)CG), Map.entry((Object)"gg", (Object)CG), Map.entry((Object)"pg", (Object)PG), Map.entry((Object)"w", (Object)W), Map.entry((Object)"l", (Object)L), Map.entry((Object)"sv", (Object)SV), Map.entry((Object)"hld", (Object)HLD), Map.entry((Object)"pitchpa", (Object)PITCH_PA), Map.entry((Object)"pitchbf", (Object)PITCH_BF), Map.entry((Object)"bf", (Object)PITCH_BF), Map.entry((Object)"np", (Object)NP), Map.entry((Object)"pitchbb", (Object)PITCH_BB), Map.entry((Object)"pitchibb", (Object)PITCH_IBB), Map.entry((Object)"pitchhbp", (Object)PITCH_HBP), Map.entry((Object)"wp", (Object)WP), Map.entry((Object)"bk", (Object)BK), Map.entry((Object)"pitchr", (Object)PITCH_R), Map.entry((Object)"go", (Object)GO), Map.entry((Object)"fo", (Object)FO), Map.entry((Object)"pitchab", (Object)PITCH_AB), Map.entry((Object)"ab", (Object)PITCH_AB), Map.entry((Object)"era", (Object)ERA), Map.entry((Object)"whip", (Object)WHIP)});
    }
}

