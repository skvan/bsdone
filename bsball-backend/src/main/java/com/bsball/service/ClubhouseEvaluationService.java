package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.utils.StatsFormatUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 个人驾驶舱「总体评价」：按全租户生涯口径计算打击、守备、投球三张六轴雷达与总体评分。
 *
 * <p>雷达与外层球队/赛事筛选无关：对比样本为当前租户、同一运动类型、有有效生涯数据的全部球员，
 * 不跨租户、不混棒球/垒球。分项按百分位加权，再按角色权重合并，并按样本量向 50 分收缩；
 * 指标原始值与样本量仍返回给前端，避免只看雷达形状、不看数据。</p>
 */
@Service
@RequiredArgsConstructor
public class ClubhouseEvaluationService {
    private static final int MAX_POOL = 500;
    private static final int BATTING_DISPLAY_PA = 10;
    private static final int BATTING_STABLE_PA = 50;
    private static final double PITCHING_DISPLAY_IP = 3.0;
    private static final double PITCHING_STABLE_IP = 15.0;
    private static final double FIELDING_DISPLAY_INN = 6.0;
    private static final double FIELDING_STABLE_INN = 30.0;
    private static final int TWO_WAY_MIN_PA = 50;
    private static final double TWO_WAY_MIN_IP = 15.0;
    private static final int MIN_POSITION_GROUP = 5;
    private static final String UNIT_PA = "PA";
    private static final String UNIT_IP = "IP";
    private static final String UNIT_INN = "INN";
    private static final String COMPARISON = "全租户生涯";
    private static final String SAMPLE_SUFFICIENT = "样本充足";
    private static final String SAMPLE_REFERENCE = "参考值";
    private static final String SAMPLE_INSUFFICIENT = "样本不足";
    private static final String ROLE_POSITION_PLAYER = "POSITION_PLAYER";
    private static final String ROLE_PITCHER = "PITCHER";
    private static final String ROLE_TWO_WAY = "TWO_WAY";
    private static final String NOTE_WOBA = "参考 wOBA";
    private static final String NOTE_FIP = "参考 FIP";
    private static final String NOTE_NO_POSITION_GROUP = "未按守位分组";
    private static final CardMeta BATTING_META = new CardMeta(
            "batting", "打击评价", "AVG · OBP · SLG · OPS · RBI · SB");
    private static final CardMeta FIELDING_META = new CardMeta(
            "fielding", "守备评价", "FPCT · TC/G · PO/G · A/G · DP/G · RF/9");
    private static final CardMeta PITCHING_META = new CardMeta(
            "pitching", "投球评价", "ERA · WHIP · K/9 · BB/9 · HR/9 · IP");

    private final StatsService statsService;

    /**
     * 构建全租户生涯评价。打击、守备、投球三张卡固定返回，样本不足的卡不可用。
     *
     * <p>仅为可用的分项拉取对比池：无数据或未达展示门槛时不再查询该分项排行榜，
     * 避免驾驶舱首屏为不可用分项付出额外查询。</p>
     *
     * @param playerId        当前球员 ID
     * @param gameMode        BASEBALL / SOFTBALL；为空或非法时按棒球处理
     * @param careerStats     当前运动类型下跨球队、跨赛事的生涯统计
     * @param primaryPosition 主要守位，用于守备同守位组比较；为空时尝试取统计行守位
     * @return 含 available / comparison / minSample / cards / overall 的评价数据
     */
    public Map<String, Object> buildCareer(Long playerId, String gameMode,
            Map<String, Object> careerStats, String primaryPosition) {
        Map<String, Object> stats = careerStats == null ? Map.of() : careerStats;
        String mode = normalizeGameMode(gameMode);
        Map<String, Object> battingStats = asMap(stats.get("batting"));
        Map<String, Object> pitchingStats = asMap(stats.get("pitching"));
        Map<String, Object> fieldingStats = asMap(stats.get("fielding"));
        List<Map<String, Object>> battingPool = battingReady(battingStats)
                ? battingPool(mode) : List.of();
        List<Map<String, Object>> pitchingPool = pitchingReady(pitchingStats)
                ? pitchingPool(mode) : List.of();
        List<Map<String, Object>> fieldingPool = fieldingReady(fieldingStats)
                ? fieldingPool(mode) : List.of();

        CardResult batting = battingCard(playerId, stats, battingPool);
        CardResult fielding = fieldingCard(playerId, primaryPosition, stats, fieldingPool);
        CardResult pitching = pitchingCard(playerId, stats, pitchingPool);
        Map<String, Object> overall = overall(batting.domain(), fielding.domain(), pitching.domain());

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("available", overall.get("score") != null);
        out.put("comparison", COMPARISON);
        out.put("minSample", minSample());
        out.put("cards", List.of(batting.card(), fielding.card(), pitching.card()));
        out.put("overall", overall);
        return out;
    }

    /**
     * 旧调用兼容层：忽略球队/赛事参数，统一走全租户生涯口径。
     *
     * @param playerId 当前球员 ID
     * @param teamId   旧口径代表球队，已忽略
     * @param eventId  旧口径赛事，已忽略
     * @param gameMode BASEBALL / SOFTBALL
     * @param stats    生涯统计
     * @return 与 {@link #buildCareer(Long, String, Map, String)} 相同
     */
    public Map<String, Object> build(Long playerId, Long teamId, Long eventId, String gameMode,
            Map<String, Object> stats) {
        return buildCareer(playerId, gameMode, stats, null);
    }

    private CardResult battingCard(Long playerId, Map<String, Object> stats,
            List<Map<String, Object>> rawPool) {
        Map<String, Object> batting = asMap(stats.get("batting"));
        if (!hasBatting(batting)) {
            return unavailableCard(BATTING_META, "暂无打击数据");
        }
        double plateAppearances = number(batting.get("pa"));
        if (plateAppearances < BATTING_DISPLAY_PA) {
            return unavailableCard(BATTING_META,
                    "打击样本不足（至少 " + BATTING_DISPLAY_PA + " PA）");
        }
        List<Map<String, Object>> pool = rawPool.stream()
                .filter(row -> number(row.get("pa")) >= BATTING_DISPLAY_PA)
                .toList();
        Map<String, Object> self = selfRow(playerId, null, batting, pool);
        double reliability = ClubhouseRatingSupport.reliability(
                plateAppearances, BATTING_STABLE_PA);

        Axis avg = rateAxis("avg", "打击率", "AVG", self, pool);
        Axis obp = rateAxis("obp", "上垒率", "OBP", self, pool);
        Axis slg = rateAxis("slg", "长打率", "SLG", self, pool);
        Axis ops = axis("ops", "整体攻击", "OPS", battingNumber(self, "ops"), true, pool,
                row -> battingNumber(row, "ops"), value -> StatsFormatUtil.fmtDecimal(value, 3));
        Axis rbi = countAxis("rbi", "打点", "RBI", self, pool);
        Axis sb = countAxis("sb", "盗垒", "SB", self, pool);
        double wobaValue = ClubhouseRatingSupport.woba(self);
        Axis woba = axis("woba", "加权上垒率", "wOBA", wobaValue, true,
                pool, ClubhouseRatingSupport::woba, value -> StatsFormatUtil.fmtDecimal(value, 3));

        List<Axis> displayAxes = List.of(avg, obp, slg, ops, rbi, sb);
        List<WeightedAxis> qualityAxes = List.of(
                new WeightedAxis(woba, 0.50),
                new WeightedAxis(obp, 0.20),
                new WeightedAxis(slg, 0.15),
                new WeightedAxis(rbi, 0.10),
                new WeightedAxis(sb, 0.05));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("woba", round3(wobaValue));
        return availableCard(BATTING_META, displayAxes, qualityAxes, reliability,
                pool.size(), plateAppearances, NOTE_WOBA, meta);
    }

    private CardResult pitchingCard(Long playerId, Map<String, Object> stats,
            List<Map<String, Object>> rawPool) {
        Map<String, Object> pitching = asMap(stats.get("pitching"));
        if (!hasPitching(pitching)) {
            return unavailableCard(PITCHING_META, "暂无投球数据");
        }
        double innings = innings(pitching);
        if (innings < PITCHING_DISPLAY_IP) {
            return unavailableCard(PITCHING_META,
                    "投球样本不足（至少 " + integer(PITCHING_DISPLAY_IP) + " 局）");
        }
        List<Map<String, Object>> pool = rawPool.stream()
                .filter(row -> innings(row) >= PITCHING_DISPLAY_IP)
                .toList();
        Map<String, Object> self = selfRow(playerId, null, pitching, pool);
        double constant = ClubhouseRatingSupport.fipConstant(rawPool);
        double reliability = ClubhouseRatingSupport.reliability(innings, PITCHING_STABLE_IP);

        Axis era = axis("era", "防御率", "ERA", pitchingNumber(self, "era"), false, pool,
                row -> pitchingNumber(row, "era"), value -> StatsFormatUtil.fmtDecimal(value, 2));
        Axis whip = axis("whip", "被上垒率", "WHIP", pitchingNumber(self, "whip"), false, pool,
                row -> pitchingNumber(row, "whip"), value -> StatsFormatUtil.fmtDecimal(value, 2));
        Axis k9 = perNineAxis("k9", "夺三振率", "K/9", "pitchSo", true, self, pool);
        Axis bb9 = perNineAxis("bb9", "保送率", "BB/9", "pitchBb", false, self, pool);
        Axis hr9 = axis("hr9", "本垒打率", "HR/9", homeRunsPerNine(self), false, pool,
                this::homeRunsPerNine, value -> StatsFormatUtil.fmtDecimal(value, 2));
        Axis ip = axis("ip", "投球局数", "IP", innings, true, pool, this::innings,
                ClubhouseEvaluationService::baseballInnings);
        double fipValue = ClubhouseRatingSupport.fip(self, constant);
        Axis fipAxis = axis("fip", "防御率独立值", "FIP", fipValue, false, pool,
                row -> ClubhouseRatingSupport.fip(row, constant),
                value -> StatsFormatUtil.fmtDecimal(value, 2));

        List<Axis> displayAxes = List.of(era, whip, k9, bb9, hr9, ip);
        List<WeightedAxis> qualityAxes = List.of(
                new WeightedAxis(fipAxis, 0.50),
                new WeightedAxis(k9, 0.20),
                new WeightedAxis(bb9, 0.15),
                new WeightedAxis(whip, 0.15));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("fip", round2(fipValue));
        meta.put("fipConstant", round2(constant));
        return availableCard(PITCHING_META, displayAxes, qualityAxes, reliability,
                pool.size(), innings, NOTE_FIP, meta);
    }

    private CardResult fieldingCard(Long playerId, String primaryPosition, Map<String, Object> stats,
            List<Map<String, Object>> rawPool) {
        Map<String, Object> fielding = asMap(stats.get("fielding"));
        if (!hasFielding(fielding)) {
            return unavailableCard(FIELDING_META, "暂无守备数据");
        }
        double innings = innings(fielding);
        if (innings < FIELDING_DISPLAY_INN) {
            return unavailableCard(FIELDING_META,
                    "守备样本不足（至少 " + integer(FIELDING_DISPLAY_INN) + " 局）");
        }
        List<Map<String, Object>> validPool = rawPool.stream()
                .filter(row -> innings(row) >= FIELDING_DISPLAY_INN)
                .toList();
        Map<String, Object> self = selfRow(playerId, primaryPosition, fielding, validPool);
        String position = blank(primaryPosition) ? text(self.get("position")) : primaryPosition;
        String group = primaryGroup(position);
        List<Map<String, Object>> groupPool = validPool.stream()
                .filter(row -> inGroup(text(row.get("position")), group))
                .toList();
        boolean grouped = groupPool.size() >= MIN_POSITION_GROUP;
        List<Map<String, Object>> pool = grouped ? groupPool : validPool;
        double reliability = ClubhouseRatingSupport.reliability(innings, FIELDING_STABLE_INN);

        List<Axis> displayAxes = List.of(
                axis("fpct", "守备率", "FPCT", fieldingPct(self), true, pool,
                        this::fieldingPct, value -> StatsFormatUtil.fmtDecimal(value, 3)),
                fieldingPerGameAxis("tcPerGame", "场均守备机会", "TC/G", "tc", 2, self, pool),
                fieldingPerGameAxis("poPerGame", "场均刺杀", "PO/G", "po", 2, self, pool),
                fieldingPerGameAxis("aPerGame", "场均助杀", "A/G", "a", 2, self, pool),
                fieldingPerGameAxis("dpPerGame", "场均双杀", "DP/G", "dp", 2, self, pool),
                axis("rangeFactor", "守备范围", "RF/9", rangeFactor(self), true, pool,
                        this::rangeFactor, value -> StatsFormatUtil.fmtDecimal(value, 2)));
        List<WeightedAxis> qualityAxes = List.of(
                new WeightedAxis(displayAxes.get(0), 0.30),
                new WeightedAxis(displayAxes.get(1), 0.15),
                new WeightedAxis(displayAxes.get(2), 0.15),
                new WeightedAxis(displayAxes.get(3), 0.15),
                new WeightedAxis(displayAxes.get(4), 0.10),
                new WeightedAxis(displayAxes.get(5), 0.15));
        String note = grouped ? null : NOTE_NO_POSITION_GROUP;
        return availableCard(FIELDING_META, displayAxes, qualityAxes, reliability,
                pool.size(), innings, note, Map.of());
    }

    private Map<String, Object> overall(DomainScore batting, DomainScore fielding,
            DomainScore pitching) {
        String role = role(batting, pitching);
        Map<String, Double> baseWeights = baseWeights(role, batting);
        List<DomainScore> domains = List.of(batting, fielding, pitching);
        double weightSum = 0.0;
        for (DomainScore domain : domains) {
            Double weight = baseWeights.get(domain.key());
            if (weight != null && domain.available()) {
                weightSum += weight;
            }
        }

        boolean scored = weightSum > 0.0;
        double score = 0.0;
        double reliability = 0.0;
        LinkedHashMap<String, Double> normalized = new LinkedHashMap<>();
        if (scored) {
            for (DomainScore domain : domains) {
                Double weight = baseWeights.get(domain.key());
                if (weight == null || !domain.available()) {
                    continue;
                }
                double share = weight / weightSum;
                normalized.put(domain.key(), round3(share));
                score += domain.score() * share;
                reliability += domain.reliability() * share;
            }
        }
        int roundedScore = scored ? (int) Math.round(score) : 0;

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("score", scored ? roundedScore : null);
        out.put("grade", scored ? grade(roundedScore) : null);
        out.put("reliability", round2(scored ? reliability : 0.0));
        out.put("confidenceLabel", confidenceLabel(scored ? reliability : 0.0));
        out.put("role", role);
        out.put("weights", normalized);
        out.put("sampleSize", sampleSizes(batting, fielding, pitching));
        return out;
    }

    private String role(DomainScore batting, DomainScore pitching) {
        if (!pitching.available()) {
            return ROLE_POSITION_PLAYER;
        }
        if (!batting.available()) {
            return ROLE_PITCHER;
        }
        boolean twoWay = batting.ownSample() >= TWO_WAY_MIN_PA
                && pitching.ownSample() >= TWO_WAY_MIN_IP;
        return twoWay ? ROLE_TWO_WAY : ROLE_PITCHER;
    }

    private Map<String, Double> baseWeights(String role, DomainScore batting) {
        LinkedHashMap<String, Double> weights = new LinkedHashMap<>();
        switch (role) {
            case ROLE_TWO_WAY -> {
                weights.put("batting", 0.40);
                weights.put("pitching", 0.40);
                weights.put("fielding", 0.20);
            }
            case ROLE_PITCHER -> {
                if (batting.available()) {
                    weights.put("pitching", 0.70);
                    weights.put("fielding", 0.15);
                    weights.put("batting", 0.15);
                } else {
                    weights.put("pitching", 0.80);
                    weights.put("fielding", 0.20);
                }
            }
            default -> {
                weights.put("batting", 0.55);
                weights.put("fielding", 0.45);
            }
        }
        return weights;
    }

    private Map<String, Object> sampleSizes(DomainScore batting, DomainScore fielding,
            DomainScore pitching) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("batting", batting.ownSample());
        out.put("pitching", pitching.ownSample());
        out.put("fielding", fielding.ownSample());
        return out;
    }

    private CardResult availableCard(CardMeta meta, List<Axis> displayAxes,
            List<WeightedAxis> qualityAxes, double reliability, int sampleSize,
            double ownSample, String note, Map<String, Object> metaExtra) {
        double rawScore = weightedPercentile(qualityAxes);
        double adjusted = ClubhouseRatingSupport.shrink(rawScore, reliability);
        int score = (int) Math.max(0, Math.min(100, Math.round(adjusted)));

        LinkedHashMap<String, Object> card = baseCard(meta);
        card.put("available", true);
        card.put("score", score);
        card.put("grade", grade(score));
        card.put("sampleSize", sampleSize);
        card.put("ownSample", displaySample(meta.domain(), ownSample));
        card.put("sampleUnit", sampleUnit(meta.domain()));
        card.put("reliability", round2(reliability));
        card.put("confidenceLabel", confidenceLabel(reliability));
        card.put("axes", displayAxes.stream().map(this::axisView).toList());
        if (note != null && !note.isBlank()) {
            card.put("note", note);
        }
        if (metaExtra != null && !metaExtra.isEmpty()) {
            card.put("meta", metaExtra);
        }
        return new CardResult(card,
                new DomainScore(meta.domain(), true, score, reliability, ownSample));
    }

    private CardResult unavailableCard(CardMeta meta, String reason) {
        LinkedHashMap<String, Object> card = baseCard(meta);
        card.put("available", false);
        card.put("score", null);
        card.put("grade", null);
        card.put("sampleSize", 0);
        card.put("ownSample", 0);
        card.put("sampleUnit", sampleUnit(meta.domain()));
        card.put("reliability", 0.0);
        card.put("confidenceLabel", SAMPLE_INSUFFICIENT);
        card.put("axes", List.of());
        card.put("reason", reason);
        return new CardResult(card, new DomainScore(meta.domain(), false, 0.0, 0.0, 0.0));
    }

    private LinkedHashMap<String, Object> baseCard(CardMeta meta) {
        LinkedHashMap<String, Object> card = new LinkedHashMap<>();
        card.put("domain", meta.domain());
        card.put("title", meta.title());
        card.put("subtitle", meta.subtitle());
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

    private double weightedPercentile(List<WeightedAxis> axes) {
        double sum = 0.0;
        double weights = 0.0;
        for (WeightedAxis weighted : axes) {
            Axis axis = weighted.axis();
            if (axis.poolValues().isEmpty()) {
                continue;
            }
            sum += percentile(axis.poolValues(), axis.rawValue(), axis.higherBetter())
                    * weighted.weight();
            weights += weighted.weight();
        }
        return weights > 0.0 ? sum / weights : 50.0;
    }

    private Axis rateAxis(String field, String label, String abbr, Map<String, Object> self,
            List<Map<String, Object>> pool) {
        return axis(field, label, abbr, battingNumber(self, field), true, pool,
                row -> battingNumber(row, field), StatsFormatUtil::fmtAvg);
    }

    private Axis countAxis(String field, String label, String abbr, Map<String, Object> self,
            List<Map<String, Object>> pool) {
        return axis(field, label, abbr, number(self.get(field)), true, pool,
                row -> number(row.get(field)), ClubhouseEvaluationService::integer);
    }

    private Axis fieldingPerGameAxis(String key, String label, String abbr, String field,
            int decimals, Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(key, label, abbr, perGame(self, field), true, pool,
                row -> perGame(row, field),
                value -> StatsFormatUtil.fmtDecimal(value, decimals));
    }

    private Axis perNineAxis(String key, String label, String abbr, String field,
            boolean higherBetter, Map<String, Object> self, List<Map<String, Object>> pool) {
        return axis(key, label, abbr, perNine(self, field), higherBetter, pool,
                row -> perNine(row, field), value -> StatsFormatUtil.fmtDecimal(value, 2));
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
     * 单样本时给 50，表示缺少可比对象。
     */
    static int percentile(List<Double> values, double raw, boolean higherBetter) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        long less = values.stream()
                .filter(value -> higherBetter ? value < raw : value > raw)
                .count();
        long equal = values.stream().filter(value -> Double.compare(value, raw) == 0).count();
        if (values.size() == 1) {
            return 50;
        }
        double rank = less + Math.max(0, equal - 1) / 2.0;
        return (int) Math.max(0, Math.min(100, Math.round(rank * 100.0 / (values.size() - 1))));
    }

    private List<Map<String, Object>> battingPool(String gameMode) {
        return safeList(statsService.getBattingLeaders(null, null, null, null, null, null,
                null, null, null, gameMode, 1, MAX_POOL, "pa", "desc", false));
    }

    private List<Map<String, Object>> pitchingPool(String gameMode) {
        return safeList(statsService.getPitchingLeaders(null, null, null, null, null, null,
                null, null, null, gameMode, 1, MAX_POOL, "ip", "desc", false));
    }

    private List<Map<String, Object>> fieldingPool(String gameMode) {
        return safeList(statsService.getFieldingLeaders(null, null, null, null, null, null,
                null, null, null, gameMode, 1, MAX_POOL, "inn", "desc"));
    }

    private List<Map<String, Object>> safeList(PageResult<Map<String, Object>> page) {
        if (page == null || page.getList() == null) {
            return List.of();
        }
        return new ArrayList<>(page.getList());
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

    /**
     * 本人行优先取池内同人；因池上限或过滤找不到时，用生涯统计构造，
     * 仍以样本池计算百分位。
     */
    private Map<String, Object> selfRow(Long playerId, String position,
            Map<String, Object> careerDomain, List<Map<String, Object>> pool) {
        Map<String, Object> found = findSelf(pool, playerId);
        if (found != null) {
            return found;
        }
        LinkedHashMap<String, Object> self = new LinkedHashMap<>(careerDomain);
        self.put("playerId", playerId);
        if (self.get("position") == null) {
            self.put("position", position);
        }
        return self;
    }

    private boolean hasBatting(Map<String, Object> batting) {
        return number(batting.get("pa")) > 0 || number(batting.get("ab")) > 0;
    }

    private boolean hasPitching(Map<String, Object> pitching) {
        return innings(pitching) > 0;
    }

    private boolean hasFielding(Map<String, Object> fielding) {
        return number(fielding.get("gp")) > 0 || number(fielding.get("tc")) > 0;
    }

    private boolean battingReady(Map<String, Object> batting) {
        return hasBatting(batting) && number(batting.get("pa")) >= BATTING_DISPLAY_PA;
    }

    private boolean pitchingReady(Map<String, Object> pitching) {
        return hasPitching(pitching) && innings(pitching) >= PITCHING_DISPLAY_IP;
    }

    private boolean fieldingReady(Map<String, Object> fielding) {
        return hasFielding(fielding) && innings(fielding) >= FIELDING_DISPLAY_INN;
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
            case "ops" -> {
                Double obp = battingNumber(row, "obp");
                Double slg = battingNumber(row, "slg");
                yield (obp == null ? 0.0 : obp) + (slg == null ? 0.0 : slg);
            }
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

    private Double pitchingNumber(Map<String, Object> row, String field) {
        Double raw = optionalNumber(row.get(field));
        if (raw != null) {
            return raw;
        }
        double innings = innings(row);
        if (innings <= 0) {
            return 0.0;
        }
        return switch (field) {
            case "era" -> 9.0 * number(row.get("er")) / innings;
            case "whip" -> (number(row.get("pitchH")) + number(row.get("pitchBb"))) / innings;
            default -> null;
        };
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

    private Double homeRunsPerNine(Map<String, Object> row) {
        double innings = innings(row);
        if (innings <= 0) {
            return 0.0;
        }
        return (number(row.get("pitchHr")) + number(row.get("pitchInsideParkHr")))
                * 9.0 / innings;
    }

    private Double innings(Map<String, Object> row) {
        double raw = number(row.get("ip"));
        if (raw <= 0) {
            raw = number(row.get("inn"));
        }
        return raw <= 0 ? 0.0 : StatsFormatUtil.baseballIpToDecimalInnings(raw);
    }

    private Map<String, Object> minSample() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("batting", BATTING_DISPLAY_PA);
        out.put("pitching", (int) PITCHING_DISPLAY_IP);
        out.put("fielding", (int) FIELDING_DISPLAY_INN);
        return out;
    }

    private static String sampleUnit(String domain) {
        return switch (domain) {
            case "batting" -> UNIT_PA;
            case "pitching" -> UNIT_IP;
            default -> UNIT_INN;
        };
    }

    /** 展示用自有样本：打席取整，投球/守备局保留 1 位小数；可靠性仍用原始值。 */
    private static Object displaySample(String domain, double ownSample) {
        if (UNIT_PA.equals(sampleUnit(domain))) {
            return (int) Math.round(ownSample);
        }
        return round1(ownSample);
    }

    /**
     * 把排行榜守位字符串拆成守位组；多守位行以顿号或逗号分隔（如 {@code "CF、LF"}）。
     * 无法识别的守位映射为 UTIL，多守位行只要有一个守位可识别就不会落入 UTIL。
     */
    private static List<String> positionGroups(String position) {
        if (blank(position)) {
            return List.of();
        }
        return Arrays.stream(position.split("[、,，]"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(ClubhouseRatingSupport::positionGroup)
                .toList();
    }

    /** 当前球员的对比组：优先取第一个可识别的守位组，全部无法识别时归 UTIL。 */
    private static String primaryGroup(String position) {
        return positionGroups(position).stream()
                .filter(group -> !"UTIL".equals(group))
                .findFirst()
                .orElse("UTIL");
    }

    /** 同组判定：多守位行只要有任一守位映射到目标组即视为组内。 */
    private static boolean inGroup(String position, String group) {
        return positionGroups(position).contains(group);
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

    private static String confidenceLabel(double reliability) {
        if (reliability >= 1.0 - 1e-9) {
            return SAMPLE_SUFFICIENT;
        }
        return reliability > 0.0 ? SAMPLE_REFERENCE : SAMPLE_INSUFFICIENT;
    }

    private static String normalizeGameMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASEBALL";
        }
        String mode = raw.trim().toUpperCase(Locale.ROOT);
        return "SOFTBALL".equals(mode) ? "SOFTBALL" : "BASEBALL";
    }

    private static String integer(double value) {
        return String.valueOf((int) Math.round(value));
    }

    /** 百分位使用十进制局数；展示仍用棒球记法（5.2 = 5 又 2/3 局）。 */
    private static String baseballInnings(double decimalInnings) {
        int outs = (int) Math.round(decimalInnings * 3);
        return StatsFormatUtil.fmtDecimal(StatsFormatUtil.baseballOutsToIp(outs), 1);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String text(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
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

    private record WeightedAxis(Axis axis, double weight) {
    }

    private record CardResult(Map<String, Object> card, DomainScore domain) {
    }

    private record DomainScore(String key, boolean available, double score, double reliability,
            double ownSample) {
    }
}
