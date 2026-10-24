package com.bsball.service;

import com.bsball.utils.StatsFormatUtil;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 个人驾驶舱总体评分的支撑算法：wOBA、FIP、小样本可靠性与守位分组。
 *
 * <p>首版 wOBA 使用固定业余近似权重，不按赛季/联赛动态校准；FIP 的常数由当前租户同运动类型的
 * 投球池反推，使联盟平均 FIP 对齐联盟平均 ERA。投球局数（ip）沿用项目统计行的棒球记法
 * （5.2 表示 5 又 2/3 局），内部换算为十进制局数后再参与除法。</p>
 */
public final class ClubhouseRatingSupport {

    private static final double WOBA_WALK = 0.70;
    private static final double WOBA_HIT_BY_PITCH = 0.70;
    private static final double WOBA_SINGLE = 0.90;
    private static final double WOBA_DOUBLE = 1.25;
    private static final double WOBA_TRIPLE = 1.60;
    private static final double WOBA_HOME_RUN = 2.00;
    private static final double WOBA_STOLEN_BASE = 0.20;
    private static final double WOBA_CAUGHT_STEALING = 0.40;

    private ClubhouseRatingSupport() {
    }

    /**
     * 计算 wOBA（参考值）。
     *
     * <p>分子为固定业余权重加权，分母为 AB + BB − IBB + SF + HBP；分母不为正时返回 0。
     * 场内本垒打（insideParkHr）与普通本垒打同样按本垒打权重计入，且不计入 singles。</p>
     *
     * @param row 打击统计行，接受数值或字符串数值
     * @return wOBA；无有效打席口径时返回 0
     */
    public static double woba(Map<String, Object> row) {
        if (row == null) {
            return 0.0;
        }
        double denominator = number(row, "ab") + number(row, "bb") - number(row, "ibb")
                + number(row, "sf") + number(row, "hbp");
        if (denominator <= 0) {
            return 0.0;
        }
        double numerator = WOBA_WALK * number(row, "bb")
                + WOBA_HIT_BY_PITCH * number(row, "hbp")
                + WOBA_SINGLE * number(row, "singles")
                + WOBA_DOUBLE * number(row, "doubles")
                + WOBA_TRIPLE * number(row, "triples")
                + WOBA_HOME_RUN * (number(row, "hr") + number(row, "insideParkHr"))
                + WOBA_STOLEN_BASE * number(row, "sb")
                - WOBA_CAUGHT_STEALING * number(row, "cs");
        return numerator / denominator;
    }

    /**
     * 计算 FIP（参考值）：{@code (13×HR + 3×(BB+HBP) − 2×SO) / IP + C}。
     *
     * <p>HR 含场上本垒打与被场内本垒打（pitchHr + pitchInsideParkHr）。</p>
     *
     * @param row      投球统计行，键为 pitchHr/pitchInsideParkHr/pitchBb/pitchHbp/pitchSo/ip
     * @param constant 租户级 FIP 常数，通常由 {@link #fipConstant(List)} 得出
     * @return FIP；无有效投球局时返回 0
     */
    public static double fip(Map<String, Object> row, double constant) {
        if (row == null) {
            return 0.0;
        }
        double innings = decimalInnings(row);
        if (innings <= 0) {
            return 0.0;
        }
        double raw = (13.0 * (number(row, "pitchHr") + number(row, "pitchInsideParkHr"))
                + 3.0 * (number(row, "pitchBb") + number(row, "pitchHbp"))
                - 2.0 * number(row, "pitchSo")) / innings;
        return raw + constant;
    }

    /**
     * 计算小样本可靠性：{@code min(1, sample / stableThreshold)}，并截断到 0–1。
     *
     * @param sample          样本量（PA / IP / 守备局）
     * @param stableThreshold 稳定门槛；非正数或非数值时视为无效，返回 0
     * @return 0–1 的可靠性系数
     */
    public static double reliability(double sample, double stableThreshold) {
        if (Double.isNaN(sample) || Double.isNaN(stableThreshold) || stableThreshold <= 0) {
            return 0.0;
        }
        if (sample <= 0) {
            return 0.0;
        }
        return Math.min(1.0, sample / stableThreshold);
    }

    /**
     * 向 50 分中位收缩：{@code 50 + (score − 50) × reliability}。
     *
     * @param score       原始分项评分
     * @param reliability 0–1 的可靠性系数
     * @return 收缩后的评分
     */
    public static double shrink(double score, double reliability) {
        return 50.0 + (score - 50.0) * reliability;
    }

    /**
     * 守位分组：C、1B、INF（2B/3B/SS/IF）、OF（LF/CF/RF）、DH、P，其余归 UTIL。
     *
     * <p>门户守位列表同时提供 IF（内野）与 2B/3B/SS，IF 与具体内野守位同归 INF 组，
     * 避免门户可选守位落到 UTIL 兜底组。</p>
     *
     * @param position 守位代码，忽略大小写与首尾空白
     * @return 守位组代码
     */
    public static String positionGroup(String position) {
        if (position == null) {
            return "UTIL";
        }
        String normalized = position.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "C" -> "C";
            case "1B" -> "1B";
            case "2B", "3B", "SS", "IF" -> "INF";
            case "LF", "CF", "RF", "OF" -> "OF";
            case "DH" -> "DH";
            case "P" -> "P";
            default -> "UTIL";
        };
    }

    /**
     * 反推租户/联盟级 FIP 常数：{@code 联盟平均 ERA − 联盟原始 FIP}。
     *
     * <p>先按投球局数汇总整个投球池的 HR（含 pitchHr + pitchInsideParkHr）/BB/HBP/SO/IP/ER，
     * 再用合计值计算，避免逐人平均的偏差。无投球局的统计行整行跳过，避免只累加分子造成的偏差。
     * 池内无有效投球局时返回 0，调用方应标注“参考 FIP”。</p>
     *
     * @param pitchingRows 投球统计行集合，可为 null
     * @return FIP 常数；无有效样本时返回 0
     */
    public static double fipConstant(List<Map<String, Object>> pitchingRows) {
        if (pitchingRows == null || pitchingRows.isEmpty()) {
            return 0.0;
        }
        Totals totals = new Totals();
        for (Map<String, Object> row : pitchingRows) {
            if (row == null) {
                continue;
            }
            double innings = decimalInnings(row);
            if (innings <= 0) {
                continue;
            }
            totals.homeRuns += number(row, "pitchHr") + number(row, "pitchInsideParkHr");
            totals.walks += number(row, "pitchBb");
            totals.hitByPitch += number(row, "pitchHbp");
            totals.strikeouts += number(row, "pitchSo");
            totals.earnedRuns += number(row, "er");
            totals.innings += innings;
        }
        if (totals.innings <= 0) {
            return 0.0;
        }
        double leagueEra = totals.earnedRuns * 9.0 / totals.innings;
        double leagueFip = (13.0 * totals.homeRuns
                + 3.0 * (totals.walks + totals.hitByPitch)
                - 2.0 * totals.strikeouts) / totals.innings;
        return leagueEra - leagueFip;
    }

    /** 投球局按棒球记法（5.2 = 5 又 2/3 局）换算为十进制局数。 */
    private static double decimalInnings(Map<String, Object> row) {
        double raw = number(row, "ip");
        return raw <= 0 ? 0.0 : StatsFormatUtil.baseballIpToDecimalInnings(raw);
    }

    private static double number(Map<String, Object> row, String key) {
        Object raw = row.get(key);
        if (raw instanceof Number value) {
            return value.doubleValue();
        }
        if (raw == null) {
            return 0.0;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || "-".equals(text)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static final class Totals {
        private double homeRuns;
        private double walks;
        private double hitByPitch;
        private double strikeouts;
        private double earnedRuns;
        private double innings;
    }
}
