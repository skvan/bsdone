package com.bsball.service;

import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.repository.TeamRepository;
import com.bsball.utils.StatsFormatUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 生涯里程碑聚合：全租户生涯累计 + 下一目标 + 最近达成时间线。
 * <p>累计与时间线都来自同一份计入排行的比赛日志，避免两个口径。</p>
 */
@Service
@RequiredArgsConstructor
public class ClubhouseMilestoneService {
    private static final int MAX_TIMELINE = 5;
    private static final String SCOPE_LABEL = "全租户生涯 · 跨赛事与球队";
    private static final String NOTE = "统计自系统记录 · 仅计入排行的比赛";

    private final StatsService statsService;
    private final TeamRepository teamRepository;

    /**
     * 构建全租户生涯里程碑；时间线取最近 5 项。
     *
     * @param playerId 当前球员 ID
     * @param gameMode 比赛模式；null 表示棒球/垒球合并统计
     * @param config   用户配置
     * @return 累计、下一目标与时间线
     */
    public Map<String, Object> build(Long playerId, String gameMode, Map<String, Object> config) {
        List<PlayerGameLogEntryDTO> logs = statsService.getPlayerGameLogForMilestones(playerId, gameMode);
        if (logs == null || logs.isEmpty()) {
            return response(0, List.of(), List.of());
        }
        List<PlayerGameLogEntryDTO> ordered = sortedByGame(logs);
        Map<String, Accumulator> accumulators = accumulators(selectedMetrics(config));
        List<MilestoneEvent> events = accumulate(ordered, teamNames(ordered), accumulators);
        int reachedCount = accumulators.values().stream().mapToInt(acc -> acc.reachedCount).sum();
        return response(reachedCount, metricItems(accumulators), timeline(events));
    }

    private List<ClubhouseMilestoneCatalog.MetricDef> selectedMetrics(Map<String, Object> config) {
        Object rawKeys = config == null ? null : asMap(config.get("milestones")).get("metricKeys");
        if (!(rawKeys instanceof List<?> list) || list.isEmpty()) {
            return defaultMetrics();
        }
        List<ClubhouseMilestoneCatalog.MetricDef> metrics = list.stream()
                .map(this::asText)
                .map(ClubhouseMilestoneCatalog::find)
                .filter(Objects::nonNull)
                .distinct()
                .limit(ClubhouseMilestoneCatalog.MAX_SELECTED)
                .toList();
        return metrics.isEmpty() ? defaultMetrics() : metrics;
    }

    private List<ClubhouseMilestoneCatalog.MetricDef> defaultMetrics() {
        return ClubhouseMilestoneCatalog.all().stream()
                .filter(ClubhouseMilestoneCatalog.MetricDef::defaultSelected)
                .toList();
    }

    private Map<String, Accumulator> accumulators(List<ClubhouseMilestoneCatalog.MetricDef> metrics) {
        LinkedHashMap<String, Accumulator> out = new LinkedHashMap<>();
        for (ClubhouseMilestoneCatalog.MetricDef def : metrics) {
            out.put(def.key(), new Accumulator(def));
        }
        return out;
    }

    private List<PlayerGameLogEntryDTO> sortedByGame(List<PlayerGameLogEntryDTO> logs) {
        List<PlayerGameLogEntryDTO> ordered = new ArrayList<>(logs);
        ordered.sort(Comparator
                .comparing(PlayerGameLogEntryDTO::getGameday, Comparator.nullsLast(String::compareTo))
                .thenComparing(PlayerGameLogEntryDTO::getGameId, Comparator.nullsLast(Long::compareTo)));
        return ordered;
    }

    private List<MilestoneEvent> accumulate(List<PlayerGameLogEntryDTO> logs, Map<Long, String> teamNames,
            Map<String, Accumulator> accumulators) {
        Set<Long> seenGames = new LinkedHashSet<>();
        List<MilestoneEvent> events = new ArrayList<>();
        for (PlayerGameLogEntryDTO log : logs) {
            boolean newGame = log.getGameId() == null || seenGames.add(log.getGameId());
            for (Accumulator accumulator : accumulators.values()) {
                double value;
                if ("batting.gp".equals(accumulator.def.key())) {
                    value = newGame ? 1 : 0;
                } else {
                    value = gameValue(accumulator.def.key(), log);
                }
                accumulator.add(value, log, teamNames, events);
            }
        }
        return events;
    }

    private double gameValue(String key, PlayerGameLogEntryDTO log) {
        return switch (key) {
            case "batting.h" -> number(log.getBatting(), "h");
            case "batting.hr" -> number(log.getBatting(), "hr") + number(log.getBatting(), "insideParkHr");
            case "batting.rbi" -> number(log.getBatting(), "rbi");
            case "batting.r" -> number(log.getBatting(), "r");
            case "batting.sb" -> number(log.getBatting(), "sb");
            case "pitching.ip" -> (double) StatsFormatUtil.baseballIpToOuts(number(log.getPitching(), "ip"));
            case "pitching.so" -> number(log.getPitching(), "pitchSo");
            case "pitching.w" -> number(log.getPitching(), "w");
            case "pitching.sv" -> number(log.getPitching(), "sv");
            case "pitching.np" -> number(log.getPitching(), "np");
            default -> 0.0;
        };
    }

    private List<Map<String, Object>> metricItems(Map<String, Accumulator> accumulators) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Accumulator acc : accumulators.values()) {
            if (acc.total <= 0.0) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("metricKey", acc.def.key());
            item.put("label", acc.def.label());
            item.put("shortLabel", acc.def.shortLabel());
            item.put("domain", acc.def.domain());
            item.put("unit", acc.def.unit());
            item.put("value", displayValue(acc.def, acc.total));
            Integer target = acc.nextTarget();
            item.put("nextTarget", target);
            item.put("remaining", target == null ? "已达成最高里程碑" : remaining(acc.def, target, acc.total));
            item.put("progressPct", target == null ? 100 : progress(acc.def, acc.total, target));
            item.put("reachedCount", acc.reachedCount);
            out.add(item);
        }
        return out;
    }

    private List<Map<String, Object>> timeline(List<MilestoneEvent> events) {
        return events.stream()
                .sorted(Comparator
                        .comparing(MilestoneEvent::gameday, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(MilestoneEvent::gameId, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingInt(MilestoneEvent::threshold).reversed()))
                .limit(MAX_TIMELINE)
                .map(this::eventView)
                .toList();
    }

    private Map<String, Object> eventView(MilestoneEvent event) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("metricKey", event.metricKey());
        row.put("title", event.title());
        row.put("gameday", event.gameday());
        row.put("eventName", event.eventName());
        row.put("opponentName", event.opponentName());
        row.put("teamName", event.teamName());
        row.put("gameId", event.gameId());
        return row;
    }

    private Map<Long, String> teamNames(List<PlayerGameLogEntryDTO> logs) {
        Set<Long> ids = new LinkedHashSet<>();
        for (PlayerGameLogEntryDTO log : logs) {
            if (log.getHomeTeamId() != null) {
                ids.add(log.getHomeTeamId());
            }
            if (log.getAwayTeamId() != null) {
                ids.add(log.getAwayTeamId());
            }
        }
        LinkedHashMap<Long, String> names = new LinkedHashMap<>();
        teamRepository.findAllById(ids).forEach(team -> names.put(team.getId(), team.getName()));
        return names;
    }

    private String opponentName(PlayerGameLogEntryDTO log, Map<Long, String> teamNames) {
        Long opponentId = null;
        if (Objects.equals(log.getTeamId(), log.getHomeTeamId())) {
            opponentId = log.getAwayTeamId();
        } else if (Objects.equals(log.getTeamId(), log.getAwayTeamId())) {
            opponentId = log.getHomeTeamId();
        }
        return opponentId == null ? "对手" : teamNames.getOrDefault(opponentId, "对手");
    }

    private String displayValue(ClubhouseMilestoneCatalog.MetricDef def, double totalUnits) {
        return isInningsPitched(def)
                ? formatOuts(totalUnits)
                : String.valueOf((int) Math.round(totalUnits));
    }

    private String remaining(ClubhouseMilestoneCatalog.MetricDef def, int target, double totalUnits) {
        double remain = Math.max(0.0, thresholdUnits(def, target) - totalUnits);
        return isInningsPitched(def)
                ? formatOuts(remain)
                : String.valueOf((int) Math.round(remain));
    }

    private boolean isInningsPitched(ClubhouseMilestoneCatalog.MetricDef def) {
        return "pitching.ip".equals(def.key());
    }

    private double thresholdUnits(ClubhouseMilestoneCatalog.MetricDef def, int threshold) {
        return isInningsPitched(def) ? threshold * 3.0 : threshold;
    }

    private String formatOuts(double outs) {
        return StatsFormatUtil.fmtDecimal(
                StatsFormatUtil.baseballOutsToIp((int) Math.round(outs)), 1);
    }

    private int progress(ClubhouseMilestoneCatalog.MetricDef def, double totalUnits, int target) {
        double targetUnits = thresholdUnits(def, target);
        if (targetUnits <= 0) {
            return 100;
        }
        return (int) Math.max(0, Math.min(100, Math.round(totalUnits * 100.0 / targetUnits)));
    }

    private Map<String, Object> response(int reachedCount, List<Map<String, Object>> items,
            List<Map<String, Object>> timeline) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("available", !items.isEmpty());
        out.put("scopeLabel", SCOPE_LABEL);
        out.put("note", NOTE);
        out.put("reachedCount", reachedCount);
        out.put("items", items);
        out.put("timeline", timeline);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object raw) {
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static double number(Map<String, Object> row, String key) {
        if (row == null) {
            return 0.0;
        }
        Object raw = row.get(key);
        if (raw == null) {
            return 0.0;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String asText(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private String milestoneTitle(ClubhouseMilestoneCatalog.MetricDef def, int threshold) {
        if (threshold == 1) {
            return switch (def.key()) {
                case "batting.h" -> "生涯首安";
                case "batting.hr" -> "生涯首轰";
                case "batting.rbi" -> "生涯首分打点";
                case "pitching.w" -> "生涯首胜";
                case "pitching.sv" -> "首次救援成功";
                default -> "达成 1 " + def.label();
            };
        }
        if ("batting.gp".equals(def.key())) {
            return "达成 " + threshold + " 场出赛";
        }
        if ("pitching.ip".equals(def.key())) {
            return "达成 " + threshold + " 投球局";
        }
        if ("pitching.np".equals(def.key())) {
            return "达成 " + threshold + " 球";
        }
        return "达成 " + threshold + " " + def.label();
    }

    private final class Accumulator {
        private final ClubhouseMilestoneCatalog.MetricDef def;
        private double total;
        private int nextThresholdIndex;
        private int reachedCount;

        private Accumulator(ClubhouseMilestoneCatalog.MetricDef def) {
            this.def = def;
        }

        private void add(double value, PlayerGameLogEntryDTO log, Map<Long, String> teamNames,
                List<MilestoneEvent> events) {
            total += value;
            while (nextThresholdIndex < def.thresholds().size()
                    && total >= thresholdUnits(def, def.thresholds().get(nextThresholdIndex))) {
                int threshold = def.thresholds().get(nextThresholdIndex);
                reachedCount++;
                events.add(new MilestoneEvent(
                        def.key(),
                        milestoneTitle(def, threshold),
                        log.getGameday(),
                        log.getEventName(),
                        opponentName(log, teamNames),
                        log.getTeamName(),
                        log.getGameId(),
                        threshold));
                nextThresholdIndex++;
            }
        }

        private Integer nextTarget() {
            return nextThresholdIndex < def.thresholds().size()
                    ? def.thresholds().get(nextThresholdIndex) : null;
        }
    }

    private record MilestoneEvent(String metricKey, String title, String gameday, String eventName,
            String opponentName, String teamName, Long gameId, int threshold) {
    }
}
