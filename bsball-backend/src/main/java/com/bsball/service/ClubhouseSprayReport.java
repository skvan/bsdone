package com.bsball.service;

import com.bsball.model.entity.HitSpray;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 驾驶舱打击落点分区统计。
 * <p>外野按落点相对本垒的极角分 7 条扇带（对应 Issue #526 附图：LL/LS/LC/MI/RC/RS/RL），
 * 内野（storedZone 为 P/C/1B/2B/3B/SS/IF）单独一区。场地坐标系与录入页一致：
 * viewBox -300 -150 1080 780，本垒约 (240,555)，两条边线约正负 47 度。</p>
 */
public final class ClubhouseSprayReport {
    private static final double HOME_X = 240.0;
    private static final double HOME_Y = 555.0;
    private static final double FOUL_ANGLE = 47.0;
    private static final int MAX_POINTS = 600;
    private static final Set<String> INFIELD_ZONES = Set.of("P", "C", "1B", "2B", "3B", "SS", "IF");
    private static final Set<String> HIT_CODES = Set.of("H1", "H2", "H3", "HR", "IPHR");

    private record Lane(String code, String label, double from, double to) {
    }

    private static final List<Lane> LANES = List.of(
            new Lane("LL", "左边线", -47.0, -38.0),
            new Lane("LS", "左路穿越", -38.0, -27.0),
            new Lane("LC", "左中路", -27.0, -15.0),
            new Lane("MI", "中路", -15.0, 15.0),
            new Lane("RC", "右中路", 15.0, 27.0),
            new Lane("RS", "右路穿越", 27.0, 38.0),
            new Lane("RL", "右边线", 38.0, 47.0));

    private ClubhouseSprayReport() {
    }

    /** 汇总为 { summary, zones[], points[] }；points 按记录顺序保留最近 600 个落点。 */
    public static Map<String, Object> build(List<HitSpray> sprays) {
        List<HitSpray> rows = sprays == null ? List.of() : sprays;
        Map<String, int[]> tally = new LinkedHashMap<>();
        tally.put("IF", new int[2]);
        for (Lane lane : LANES) {
            tally.put(lane.code(), new int[2]);
        }
        Set<Long> games = new LinkedHashSet<>();
        List<Map<String, Object>> points = new ArrayList<>();
        int hits = 0;
        int groundRule = 0;
        for (HitSpray row : rows) {
            if (row == null || row.getSprayX() == null || row.getSprayY() == null) {
                continue;
            }
            double x = row.getSprayX().doubleValue();
            double y = row.getSprayY().doubleValue();
            String zone = classify(x, y, row.getSprayZone());
            boolean hit = isHit(row.getResultCode());
            int[] counts = tally.get(zone);
            counts[1] += 1;
            if (hit) {
                counts[0] += 1;
                hits += 1;
            }
            String resultCode = normalize(row.getResultCode());
            if (resultCode != null && resultCode.contains("GR")) {
                groundRule += 1;
            }
            if (row.getGameId() != null) {
                games.add(row.getGameId());
            }
            LinkedHashMap<String, Object> point = new LinkedHashMap<>();
            point.put("x", round1(x));
            point.put("y", round1(y));
            point.put("resultCode", row.getResultCode());
            point.put("zone", zone);
            point.put("depth", row.getSprayDepth());
            point.put("hit", hit);
            points.add(point);
        }
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("balls", points.size());
        summary.put("hits", hits);
        summary.put("games", games.size());
        summary.put("groundRule", groundRule);

        List<Map<String, Object>> zones = new ArrayList<>();
        zones.add(zoneRow("IF", "内野", tally.get("IF")));
        for (Lane lane : LANES) {
            zones.add(zoneRow(lane.code(), lane.label(), tally.get(lane.code())));
        }

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("summary", summary);
        out.put("zones", zones);
        out.put("points", points.size() > MAX_POINTS
                ? points.subList(points.size() - MAX_POINTS, points.size())
                : points);
        return out;
    }

    /** 落点分区：内野优先按记录区域；外野按极角扇带（越界贴边到两侧边线）。 */
    static String classify(double x, double y, String storedZone) {
        String zone = normalize(storedZone);
        if (zone != null && INFIELD_ZONES.contains(zone)) {
            return "IF";
        }
        double angle = Math.toDegrees(Math.atan2(x - HOME_X, HOME_Y - y));
        double clamped = Math.max(-FOUL_ANGLE, Math.min(FOUL_ANGLE, angle));
        for (Lane lane : LANES) {
            if (clamped >= lane.from() && clamped < lane.to()) {
                return lane.code();
            }
        }
        return clamped >= 0 ? "RL" : "LL";
    }

    static boolean isHit(String resultCode) {
        String code = normalize(resultCode);
        return code != null && HIT_CODES.contains(code);
    }

    private static Map<String, Object> zoneRow(String code, String label, int[] counts) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("label", label);
        row.put("hits", counts[0]);
        row.put("balls", counts[1]);
        return row;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase();
        return value.isEmpty() ? null : value;
    }

    private static double round1(double value) {
        return Math.round(value * 10d) / 10d;
    }
}
