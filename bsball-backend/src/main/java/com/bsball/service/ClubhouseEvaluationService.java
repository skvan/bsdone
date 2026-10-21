package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.utils.StatsFormatUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 个人驾驶舱「总体评价」：把职业棒球口径的打击、守备、投球指标换算为同队同赛事百分位。
 *
 * <p>百分位只在所选代表球队与赛事内计算，不跨赛事、不跨租户。指标原始值仍返回给前端，
 * 避免只看雷达形状、不看数据。守备指标受守位影响，前端需同时展示比较口径。</p>
 */
@Service
@RequiredArgsConstructor
public class ClubhouseEvaluationService {
    private static final int MIN_SAMPLE = 3;
    private static final int MAX_POOL = 500;
    private static final int MIN_BATTING_PA = 3;
    private static final double MIN_PITCHING_IP = 1.0;
    private static final double MIN_FIELDING_INN = 3.0;
    private static final String NO_SCOPE_REASON = "请先选择代表球队与赛事";
    private static final CardMeta BATTING_META = new CardMeta(
            "batting", "打击评价", "AVG · OBP · SLG · RBI · SB");
    private static final CardMeta FIELDING_META = new CardMeta(
            "fielding", "守备评价", "FPCT · PO/G · A/G · DP/G · RF/9");
    private static final CardMeta PITCHING_META = new CardMeta(
            "pitching", "投球评价", "ERA · WHIP · K/9 · BB/9 · IP");

    private final StatsService statsService;

    /**
     * 构建总体评价卡片。打击、守备固定返回；有投球数据时追加投球卡。
     *
     * @param playerId 当前球员 ID
     * @param teamId   所选代表球队；为空时不计算百分位
     * @param eventId  所选赛事；为空时不计算百分位
     * @param gameMode BASEBALL / SOFTBALL
     * @param stats    所选范围的球员统计 Map，仅用于判断领域可用性
     * @return 含 available / comparison / minSample / cards 的评价数据
     */
    public Map<String, Object> build(Long playerId, Long teamId, Long eventId, String gameMode,
            Map<String, Object> stats) {
        Map<String, Object> safeStats = stats == null ? Map.of() : stats;
        boolean hasBatting = hasBatting(safeStats);
        boolean hasPitching = hasPitching(safeStats);
        boolean hasFielding = hasFielding(safeStats);
        List<Map<String, Object>> battingPool = hasBatting
                ? battingPool(teamId, eventId, gameMode) : List.of();
        List<Map<String, Object>> pitchingPool = hasPitching
                ? pitchingPool(teamId, eventId, gameMode) : List.of();
        List<Map<String, Object>> fieldingPool = hasFielding
                ? fieldingPool(teamId, eventId, gameMode) : List.of();

        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(battingCard(playerId, teamId, eventId, hasBatting, battingPool));
        cards.add(fieldingCard(playerId, teamId, eventId, hasFielding, fieldingPool));
        if (hasPitching) {
            cards.add(pitchingCard(playerId, teamId, eventId, pitchingPool));
        }

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("available", cards.stream().anyMatch(card -> Boolean.TRUE.equals(card.get("available"))));
        out.put("comparison", "同队同赛事");
        out.put("minSample", MIN_SAMPLE);
        out.put("cards", cards);
        return out;
    }

    private Map<String, Object> battingCard(Long playerId, Long teamId, Long eventId,
            boolean available, List<Map<String, Object>> pool) {
        if (!available) {
            return unavailableCard(BATTING_META, "暂无打击数据");
        }
        if (!hasScope(teamId, eventId)) {
            return unavailableCard(BATTING_META, NO_SCOPE_REASON);
        }
        Map<String, Object> self = findSelf(pool, playerId);
        if (self == null) {
            return unavailableCard(BATTING_META, "有效打席不足（至少 " + MIN_BATTING_PA + "）");
        }
        if (pool.size() < MIN_SAMPLE) {
            return sampleInsufficientCard(BATTING_META);
        }
        return availableCard(BATTING_META, List.of(
                battingRateAxis("avg", "打击率", "AVG", self, pool),
                battingRateAxis("obp", "上垒率", "OBP", self, pool),
                battingRateAxis("slg", "长打率", "SLG", self, pool),
                battingCountAxis("rbi", "打点", "RBI", self, pool),
                battingCountAxis("sb", "盗垒", "SB", self, pool)));
    }

    private Map<String, Object> fieldingCard(Long playerId, Long teamId, Long eventId,
            boolean available, List<Map<String, Object>> pool) {
        if (!available) {
            return unavailableCard(FIELDING_META, "暂无守备数据");
        }
        if (!hasScope(teamId, eventId)) {
            return unavailableCard(FIELDING_META, NO_SCOPE_REASON);
        }
        Map<String, Object> self = findSelf(pool, playerId);
        if (self == null) {
            return unavailableCard(FIELDING_META,
                    "守备局数不足（至少 " + integer(MIN_FIELDING_INN) + " 局）");
        }
        if (pool.size() < MIN_SAMPLE) {
            return sampleInsufficientCard(FIELDING_META);
        }
        return availableCard(FIELDING_META, List.of(
                axis("fpct", "守备率", "FPCT", fieldingPct(self), true, pool,
                        this::fieldingPct, value -> StatsFormatUtil.fmtDecimal(value, 3)),
                fieldingPerGameAxis("poPerGame", "场均刺杀", "PO/G", "po", 1, self, pool),
                fieldingPerGameAxis("aPerGame", "场均助杀", "A/G", "a", 1, self, pool),
                fieldingPerGameAxis("dpPerGame", "场均双杀", "DP/G", "dp", 2, self, pool),
                axis("rangeFactor", "守备范围", "RF/9", rangeFactor(self), true, pool,
                        this::rangeFactor, value -> StatsFormatUtil.fmtDecimal(value, 2))));
    }

    private Map<String, Object> pitchingCard(Long playerId, Long teamId, Long eventId,
            List<Map<String, Object>> pool) {
        if (!hasScope(teamId, eventId)) {
            return unavailableCard(PITCHING_META, NO_SCOPE_REASON);
        }
        Map<String, Object> self = findSelf(pool, playerId);
        if (self == null) {
            return unavailableCard(PITCHING_META,
                    "投球局数不足（至少 " + integer(MIN_PITCHING_IP) + " 局）");
        }
        if (pool.size() < MIN_SAMPLE) {
            return sampleInsufficientCard(PITCHING_META);
        }
        return availableCard(PITCHING_META, List.of(
                pitchingRateAxis("era", "防御率", "ERA", self, pool),
                pitchingRateAxis("whip", "被上垒率", "WHIP", self, pool),
                pitchingPerNineAxis("k9", "夺三振率", "K/9", "pitchSo", true, self, pool),
                pitchingPerNineAxis("bb9", "保送率", "BB/9", "pitchBb", false, self, pool),
                axis("ip", "投球局数", "IP", innings(self), true, pool,
                        this::innings, ClubhouseEvaluationService::baseballInnings)));
    }

    private Axis battingRateAxis(String field, String label, String abbr,
            Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(field, label, abbr, battingNumber(self, field), true, pool,
                row -> battingNumber(row, field), StatsFormatUtil::fmtAvg);
    }

    private Axis battingCountAxis(String field, String label, String abbr,
            Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(field, label, abbr, number(self.get(field)), true, pool,
                row -> number(row.get(field)), ClubhouseEvaluationService::integer);
    }

    private Axis fieldingPerGameAxis(String key, String label, String abbr, String field, int decimals,
            Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(key, label, abbr, perGame(self, field), true, pool,
                row -> perGame(row, field), value -> StatsFormatUtil.fmtDecimal(value, decimals));
    }

    private Axis pitchingRateAxis(String field, String label, String abbr,
            Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(field, label, abbr, number(self.get(field)), false, pool,
                row -> number(row.get(field)), value -> StatsFormatUtil.fmtDecimal(value, 2));
    }

    private Axis pitchingPerNineAxis(String key, String label, String abbr, String field,
            boolean higherBetter, Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(key, label, abbr, perNine(self, field), higherBetter, pool,
                row -> perNine(row, field), value -> StatsFormatUtil.fmtDecimal(value, 1));
    }

    private Map<String, Object> availableCard(CardMeta meta, List<Axis> axes) {
        int sampleSize = axes.stream().mapToInt(axis -> axis.poolValues().size()).min().orElse(0);
        int score = (int) Math.round(axes.stream()
                .mapToInt(axis -> percentile(axis.poolValues(), axis.rawValue(), axis.higherBetter()))
                .average()
                .orElse(0));
        LinkedHashMap<String, Object> card = new LinkedHashMap<>();
        card.put("domain", meta.domain());
        card.put("title", meta.title());
        card.put("subtitle", meta.subtitle());
        card.put("available", true);
        card.put("score", score);
        card.put("grade", grade(score));
        card.put("sampleSize", sampleSize);
        card.put("axes", axes.stream().map(this::axisView).toList());
        return card;
    }

    private Map<String, Object> axisView(Axis axis) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("key", axis.key());
        row.put("label", axis.label());
        row.put("abbr", axis.abbr());
        row.put("value", axis.displayValue());
        row.put("percentile", percentile(axis.poolValues(), axis.rawValue(), axis.higherBetter()));
        return row;
    }

    private Map<String, Object> unavailableCard(CardMeta meta, String reason) {
        LinkedHashMap<String, Object> card = new LinkedHashMap<>();
        card.put("domain", meta.domain());
        card.put("title", meta.title());
        card.put("subtitle", meta.subtitle());
        card.put("available", false);
        card.put("score", null);
        card.put("grade", null);
        card.put("sampleSize", 0);
        card.put("axes", List.of());
        card.put("reason", reason);
        return card;
    }

    private Map<String, Object> sampleInsufficientCard(CardMeta meta) {
        return unavailableCard(meta, "同队同赛事有效样本不足（至少 " + MIN_SAMPLE + " 人）");
    }

    private Axis axis(String key, String label, String abbr, Double raw, boolean higherBetter,
            List<Map<String, Object>> pool, Function<Map<String, Object>, Double> extractor,
            Function<Double, String> formatter) {
        double value = raw == null ? 0.0 : raw;
        List<Double> values = pool.stream()
                .map(extractor)
                .filter(item -> item != null && !item.isNaN() && !item.isInfinite())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        return new Axis(key, label, abbr, value, formatter.apply(value), higherBetter, values);
    }

    /**
     * 百分位：0 为同组最低，100 为最高；并列值取平均名次。反向指标先倒序再计算。
     * 单样本时给 50，但调用方会先用 {@link #MIN_SAMPLE} 拦住。
     */
    static int percentile(List<Double> values, double raw, boolean higherBetter) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        long less = values.stream().filter(value -> higherBetter ? value < raw : value > raw).count();
        long equal = values.stream().filter(value -> Double.compare(value, raw) == 0).count();
        if (values.size() == 1) {
            return 50;
        }
        double rank = less + Math.max(0, equal - 1) / 2.0;
        return (int) Math.max(0, Math.min(100, Math.round(rank * 100.0 / (values.size() - 1))));
    }

    private List<Map<String, Object>> battingPool(Long teamId, Long eventId, String gameMode) {
        if (!hasScope(teamId, eventId)) {
            return List.of();
        }
        return safeList(statsService.getBattingLeaders(eventId, null, null, List.of(teamId), null, null,
                null, null, null, gameMode, 1, MAX_POOL, "pa", "desc", false)).stream()
                .filter(row -> number(row.get("pa")) >= MIN_BATTING_PA)
                .toList();
    }

    private List<Map<String, Object>> pitchingPool(Long teamId, Long eventId, String gameMode) {
        if (!hasScope(teamId, eventId)) {
            return List.of();
        }
        return safeList(statsService.getPitchingLeaders(eventId, null, null, List.of(teamId), null, null,
                null, null, null, gameMode, 1, MAX_POOL, "ip", "desc", false)).stream()
                .filter(row -> innings(row) >= MIN_PITCHING_IP)
                .toList();
    }

    private List<Map<String, Object>> fieldingPool(Long teamId, Long eventId, String gameMode) {
        if (!hasScope(teamId, eventId)) {
            return List.of();
        }
        return safeList(statsService.getFieldingLeaders(eventId, null, null, List.of(teamId), null, null,
                null, null, null, gameMode, 1, MAX_POOL, "tcPct", "desc")).stream()
                .filter(row -> innings(row) >= MIN_FIELDING_INN)
                .toList();
    }

    private static boolean hasScope(Long teamId, Long eventId) {
        return teamId != null && eventId != null;
    }

    private List<Map<String, Object>> safeList(PageResult<Map<String, Object>> page) {
        if (page == null || page.getList() == null) {
            return List.of();
        }
        return page.getList();
    }

    private Map<String, Object> findSelf(List<Map<String, Object>> pool, Long playerId) {
        if (playerId == null) {
            return null;
        }
        return pool.stream()
                .filter(row -> playerId.equals(asLong(row.get("playerId"))))
                .findFirst()
                .orElse(null);
    }

    private boolean hasBatting(Map<String, Object> stats) {
        Map<String, Object> batting = asMap(stats.get("batting"));
        return number(batting.get("pa")) > 0 || number(batting.get("ab")) > 0;
    }

    private boolean hasPitching(Map<String, Object> stats) {
        return innings(asMap(stats.get("pitching"))) > 0;
    }

    private boolean hasFielding(Map<String, Object> stats) {
        Map<String, Object> fielding = asMap(stats.get("fielding"));
        return number(fielding.get("gp")) > 0 || number(fielding.get("tc")) > 0;
    }

    private Double battingNumber(Map<String, Object> row, String field) {
        Double raw = optionalNumber(row.get(field));
        if (raw != null) {
            return raw;
        }
        return switch (field) {
            case "avg" -> ratio(row, "h", "ab");
            case "obp" -> obpFallback(row);
            case "slg" -> ratio(row, "tb", "ab");
            default -> null;
        };
    }

    private Double obpFallback(Map<String, Object> row) {
        double hits = number(row.get("h"));
        double walks = number(row.get("bb"));
        double hitByPitch = number(row.get("hbp"));
        double sacFlies = number(row.get("sf"));
        double below = number(row.get("ab")) + walks + hitByPitch + sacFlies;
        return below > 0 ? (hits + walks + hitByPitch) / below : 0.0;
    }

    private Double ratio(Map<String, Object> row, String numerator, String denominator) {
        double below = number(row.get(denominator));
        if (below <= 0) {
            return 0.0;
        }
        return number(row.get(numerator)) / below;
    }

    private Double perGame(Map<String, Object> row, String field) {
        double games = number(row.get("gp"));
        return games > 0 ? number(row.get(field)) / games : 0.0;
    }

    private Double fieldingPct(Map<String, Object> row) {
        double chances = number(row.get("tc"));
        if (chances <= 0) {
            return 0.0;
        }
        return (number(row.get("po")) + number(row.get("a"))) / chances;
    }

    private Double rangeFactor(Map<String, Object> row) {
        double innings = innings(row);
        return innings > 0 ? (number(row.get("po")) + number(row.get("a"))) * 9.0 / innings : 0.0;
    }

    private Double perNine(Map<String, Object> row, String field) {
        double innings = innings(row);
        return innings > 0 ? number(row.get(field)) * 9.0 / innings : 0.0;
    }

    private Double innings(Map<String, Object> row) {
        double raw = number(row.get("ip"));
        if (raw <= 0) {
            raw = number(row.get("inn"));
        }
        return raw <= 0 ? 0.0 : StatsFormatUtil.baseballIpToDecimalInnings(raw);
    }

    private static String grade(int score) {
        if (score >= 90) {
            return "S";
        }
        if (score >= 85) {
            return "A+";
        }
        if (score >= 80) {
            return "A";
        }
        if (score >= 75) {
            return "B+";
        }
        if (score >= 70) {
            return "B";
        }
        if (score >= 60) {
            return "C+";
        }
        if (score >= 50) {
            return "C";
        }
        return "D";
    }

    private static String integer(double value) {
        return String.valueOf((int) Math.round(value));
    }

    /** 百分位使用十进制局数；展示仍用棒球记法（5.2 = 5 又 2/3 局）。 */
    private static String baseballInnings(double decimalInnings) {
        int outs = (int) Math.round(decimalInnings * 3);
        return StatsFormatUtil.fmtDecimal(StatsFormatUtil.baseballOutsToIp(outs), 1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object raw) {
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static double number(Object raw) {
        Double value = optionalNumber(raw);
        return value == null ? 0.0 : value;
    }

    private static Double optionalNumber(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim().replace("%", "");
        if (text.isEmpty() || "-".equals(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long asLong(Object raw) {
        if (raw instanceof Number value) {
            return value.longValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record CardMeta(String domain, String title, String subtitle) {
    }

    private record Axis(String key, String label, String abbr, double rawValue, String displayValue,
            boolean higherBetter, List<Double> poolValues) {
    }
}
