/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.StatsLeadersOrderBy
 */
package com.bsball.utils;

import java.util.Locale;
import java.util.Set;

/*
 * Exception performing whole class analysis ignored.
 */
public final class StatsLeadersOrderBy {
    private static final String TIE_BREAK_PLAYER = ", q.gp DESC, q.playerid ASC";
    private static final Set<String> BATTING = Set.of((Object[])new String[]{"gp", "pa", "ab", "r", "h", "doubles", "triples", "hr", "insideParkHr", "rbi", "bb", "so", "sb", "cs", "avg", "obp", "slg", "ops", "tb"});
    private static final Set<String> PITCHING = Set.of((Object[])new String[]{"w", "l", "era", "gp", "gs", "cg", "sv", "svo", "ip", "pitchH", "pitchR", "er", "pitchHr", "pitchInsideParkHr", "pitchHbp", "pitchBb", "pitchSo", "whip", "pitchPa", "pitchBf", "np"});
    private static final Set<String> FIELDING = Set.of((Object)"gp", (Object)"gs", (Object)"inn", (Object)"tc", (Object)"po", (Object)"a", (Object)"e", (Object)"dp", (Object)"tcPct");

    private StatsLeadersOrderBy() {
    }

    private static String dir(String sortOrder) {
        if (sortOrder != null && sortOrder.toLowerCase(Locale.ROOT).startsWith("asc")) {
            return "ASC";
        }
        return "DESC";
    }

    public static String batting(String sortProp, String sortOrder) {
        String p;
        String d = StatsLeadersOrderBy.dir((String)sortOrder);
        String string = p = sortProp == null ? "avg" : sortProp.trim();
        if (!BATTING.contains(p)) {
            p = "avg";
            d = "DESC";
        }
        return (switch (p) {
            case "ops" -> String.format("CASE WHEN (q.obp + q.slg) IS NULL THEN 1 ELSE 0 END, (q.obp + q.slg) %s", d);
            default -> String.format("CASE WHEN q.%s IS NULL THEN 1 ELSE 0 END, q.%s %s", p, p, d);
        }) + ", q.gp DESC, q.playerid ASC";
    }

    public static String pitching(String sortProp, String sortOrder) {
        String p;
        String d = StatsLeadersOrderBy.dir((String)sortOrder);
        String string = p = sortProp == null ? "era" : sortProp.trim();
        if (!PITCHING.contains(p)) {
            p = "era";
            d = "ASC";
        }
        String primary = switch (p) {
            case "pitchInsideParkHr" -> "q.pitch_inside_park_hr " + d;
            default -> "q." + p + " " + d;
        };
        return primary + ", q.gp DESC, q.playerid ASC";
    }

    public static String fielding(String sortProp, String sortOrder) {
        String p;
        String d = StatsLeadersOrderBy.dir((String)sortOrder);
        String string = p = sortProp == null ? "tc" : sortProp.trim();
        if (!FIELDING.contains(p)) {
            p = "tc";
            d = "DESC";
        }
        Object primary = "tc".equals(p) || "po".equals(p) || "a".equals(p) || "e".equals(p) ? String.format("CASE WHEN q.%s IS NULL THEN 1 ELSE 0 END ASC, q.%s %s", p, p, d) : "q." + p + " " + d;
        return (String)primary + ", q.gp DESC, q.playerid ASC";
    }
}

