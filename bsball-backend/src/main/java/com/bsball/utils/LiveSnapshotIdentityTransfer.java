package com.bsball.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 把 live snapshot 中的球员归属从源球员改到目标球员；保留原始快照由调用方写入审计。 */
public final class LiveSnapshotIdentityTransfer {
    private static final Set<String> PLAYER_ID_FIELDS = Set.of(
            "batterPlayerId", "pitcherPlayerId", "runnerId", "responsiblePitcherId",
            "batterId", "pitcherId", "playerId", "runnerDragCiRunnerId", "mvp", "svp");
    private static final Set<String> TOP_LEVEL_ID_FIELDS = Set.of(
            "homeFieldingPitcherId", "awayFieldingPitcherId",
            "homeStarterFpId", "awayStarterFpId",
            "homeLastDhAddedFromPoolId", "awayLastDhAddedFromPoolId",
            "homeFieldingPitcherRow", "awayFieldingPitcherRow");
    private static final List<String> ROSTER_ARRAYS = List.of(
            "homeLineup", "awayLineup", "homeBench", "awayBench");
    private static final Set<String> HISTORY_FIELDS = Set.of(
            "undoHistory", "redoHistory", "undoHistoryCapped", "redoHistoryCapped");
    private static final List<String> AUDIT_FIELDS = List.of(
            "v", "savedAt", "gameMode", "gameId",
            "homeLineup", "awayLineup", "homeBench", "awayBench",
            "homeFieldingPitcherId", "awayFieldingPitcherId",
            "homeStarterFpId", "awayStarterFpId",
            "homeFieldingPitcherRow", "awayFieldingPitcherRow",
            "homeLastDhAddedFromPoolId", "awayLastDhAddedFromPoolId",
            "homeUnavailablePlayerIds", "awayUnavailablePlayerIds",
            "homeLineupStarterIds", "awayLineupStarterIds",
            "homeSlotSubHistory", "awaySlotSubHistory",
            "gameState", "mvp", "svp", "playEvents", "playerStatsById");

    private LiveSnapshotIdentityTransfer() {
    }

    public record Result(String json, boolean changed, int references) {
    }

    /** 改写现场记录；JSON 非法或根节点不是对象时抛出 IllegalArgumentException。 */
    public static Result rewrite(String json, long sourcePlayerId, long targetPlayerId,
            ObjectMapper mapper) {
        if (json == null || json.isBlank() || sourcePlayerId == targetPlayerId) {
            return new Result(json, false, 0);
        }
        ObjectNode root = parse(json, mapper);
        int[] count = {0};
        rewritePlayerIdFields(root, sourcePlayerId, targetPlayerId, count);
        rewriteIdContainers(root, sourcePlayerId, targetPlayerId, count);
        rewriteTopLevelIds(root, sourcePlayerId, targetPlayerId, count);
        rewritePlayerObjects(root, sourcePlayerId, targetPlayerId, count);
        rewriteRoster(root, sourcePlayerId, targetPlayerId, count);
        rewritePlayerStats(root, sourcePlayerId, targetPlayerId, count);
        try {
            return new Result(mapper.writeValueAsString(root), count[0] > 0, count[0]);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("现场记录序列化失败", ex);
        }
    }

    /** 统计源球员在 live snapshot 中的引用数，不修改数据。 */
    public static int countReferences(String json, long sourcePlayerId, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return 0;
        }
        ObjectNode root = parse(json, mapper);
        int[] count = {0};
        countPlayerIdFields(root, sourcePlayerId, count);
        countIdContainers(root, sourcePlayerId, count);
        countTopLevelIds(root, sourcePlayerId, count);
        countPlayerObjects(root, sourcePlayerId, count);
        for (String field : ROSTER_ARRAYS) {
            if (rosterIndex(root.get(field), sourcePlayerId) >= 0) {
                count[0] += 1;
            }
        }
        if (root.get("playerStatsById") instanceof ObjectNode stats
                && stats.has(String.valueOf(sourcePlayerId))) {
            count[0] += 1;
        }
        return count[0];
    }

    /** 源/目标同时出现在同一阵容或替补名单时无法自动修正槽位，返回 true。 */
    public static boolean hasRosterConflict(String json, long sourcePlayerId, long targetPlayerId,
            ObjectMapper mapper) {
        if (json == null || json.isBlank() || sourcePlayerId == targetPlayerId) {
            return false;
        }
        ObjectNode root = parse(json, mapper);
        for (String field : ROSTER_ARRAYS) {
            JsonNode roster = root.get(field);
            if (rosterIndex(roster, sourcePlayerId) >= 0
                    && rosterIndex(roster, targetPlayerId) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** 转记审计用的现场记录精简快照，不包含 undo/redo 历史。 */
    public static String auditSnapshot(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        ObjectNode root = parse(json, mapper);
        ObjectNode audit = mapper.createObjectNode();
        for (String field : AUDIT_FIELDS) {
            JsonNode value = root.get(field);
            if (value != null) {
                audit.set(field, value.deepCopy());
            }
        }
        try {
            return mapper.writeValueAsString(audit);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("现场记录审计快照序列化失败", ex);
        }
    }

    private static ObjectNode parse(String json, ObjectMapper mapper) {
        JsonNode parsed;
        try {
            parsed = mapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("现场记录 JSON 解析失败", ex);
        }
        if (!(parsed instanceof ObjectNode root)) {
            throw new IllegalArgumentException("现场记录格式不正确");
        }
        return root;
    }

    private static void rewritePlayerIdFields(JsonNode node, long source, long target, int[] count) {
        if (node instanceof ObjectNode object) {
            for (String name : fieldNames(object)) {
                if (HISTORY_FIELDS.contains(name)) {
                    continue;
                }
                JsonNode child = object.get(name);
                if (PLAYER_ID_FIELDS.contains(name) && isPlayerId(child, source)) {
                    object.put(name, target);
                    count[0] += 1;
                } else {
                    rewritePlayerIdFields(child, source, target, count);
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                rewritePlayerIdFields(child, source, target, count);
            }
        }
    }

    private static void countPlayerIdFields(JsonNode node, long source, int[] count) {
        if (node instanceof ObjectNode object) {
            for (String name : fieldNames(object)) {
                if (HISTORY_FIELDS.contains(name)) {
                    continue;
                }
                JsonNode child = object.get(name);
                if (PLAYER_ID_FIELDS.contains(name) && isPlayerId(child, source)) {
                    count[0] += 1;
                } else {
                    countPlayerIdFields(child, source, count);
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                countPlayerIdFields(child, source, count);
            }
        }
    }

    private static void rewriteIdContainers(JsonNode node, long source, long target, int[] count) {
        if (node instanceof ObjectNode object) {
            for (String name : fieldNames(object)) {
                if (HISTORY_FIELDS.contains(name)) {
                    continue;
                }
                JsonNode child = object.get(name);
                if (name.endsWith("UnavailablePlayerIds")) {
                    rewriteIdList(child, source, target, count, true);
                } else if (name.endsWith("LineupStarterIds")) {
                    rewriteIdList(child, source, target, count, false);
                } else if ("runnerIdsBefore".equals(name) || "scoringRunnerIds".equals(name)) {
                    rewriteIdList(child, source, target, count, true);
                } else if (name.endsWith("SlotSubHistory") && child.isArray()) {
                    for (JsonNode slot : child) {
                        if (slot.isArray()) {
                            rewriteNumberArray((ArrayNode) slot, source, target, count, false);
                        }
                    }
                } else if (("basesBefore".equals(name) || "basesAfter".equals(name))
                        && child.isObject()) {
                    rewriteNumberValues((ObjectNode) child, source, target, count);
                } else if ("runners".equals(name) && child.isObject()
                        && object.has("inning") && object.has("batterIndex")) {
                    rewriteNumberValues((ObjectNode) child, source, target, count);
                } else {
                    rewriteIdContainers(child, source, target, count);
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                rewriteIdContainers(child, source, target, count);
            }
        }
    }

    private static void countIdContainers(JsonNode node, long source, int[] count) {
        if (node instanceof ObjectNode object) {
            for (String name : fieldNames(object)) {
                if (HISTORY_FIELDS.contains(name)) {
                    continue;
                }
                JsonNode child = object.get(name);
                if (name.endsWith("UnavailablePlayerIds") || name.endsWith("LineupStarterIds")
                        || "runnerIdsBefore".equals(name) || "scoringRunnerIds".equals(name)) {
                    countIdList(child, source, count);
                } else if (name.endsWith("SlotSubHistory") && child.isArray()) {
                    for (JsonNode slot : child) {
                        countNumberArray(slot, source, count);
                    }
                } else if (("basesBefore".equals(name) || "basesAfter".equals(name))
                        && child.isObject()) {
                    countNumberValues((ObjectNode) child, source, count);
                } else if ("runners".equals(name) && child.isObject()
                        && object.has("inning") && object.has("batterIndex")) {
                    countNumberValues((ObjectNode) child, source, count);
                } else {
                    countIdContainers(child, source, count);
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                countIdContainers(child, source, count);
            }
        }
    }

    private static void rewriteTopLevelIds(ObjectNode root, long source, long target, int[] count) {
        for (String field : TOP_LEVEL_ID_FIELDS) {
            if (isPlayerId(root.get(field), source)) {
                root.put(field, target);
                count[0] += 1;
            }
        }
    }

    private static void countTopLevelIds(ObjectNode root, long source, int[] count) {
        for (String field : TOP_LEVEL_ID_FIELDS) {
            if (isPlayerId(root.get(field), source)) {
                count[0] += 1;
            }
        }
    }

    private static void rewritePlayerObjects(ObjectNode root, long source, long target, int[] count) {
        if (root.get("gameState") instanceof ObjectNode gameState) {
            if (gameState.get("flow") instanceof ObjectNode flow) {
                rewriteObjectId(flow.get("runner"), source, target, count);
                if (isPlayerId(flow.get("runner"), source)) {
                    flow.put("runner", target);
                    count[0] += 1;
                }
            }
            rewriteObjectId(gameState.get("mvp"), source, target, count);
            rewriteObjectId(gameState.get("svp"), source, target, count);
        }
        rewriteObjectId(root.get("mvp"), source, target, count);
        rewriteObjectId(root.get("svp"), source, target, count);
        rewriteObjectId(root.get("homeFieldingPitcherRow"), source, target, count);
        rewriteObjectId(root.get("awayFieldingPitcherRow"), source, target, count);
    }

    private static void countPlayerObjects(ObjectNode root, long source, int[] count) {
        if (root.get("gameState") instanceof ObjectNode gameState) {
            if (gameState.get("flow") instanceof ObjectNode flow) {
                countObjectId(flow.get("runner"), source, count);
                if (isPlayerId(flow.get("runner"), source)) {
                    count[0] += 1;
                }
            }
            countObjectId(gameState.get("mvp"), source, count);
            countObjectId(gameState.get("svp"), source, count);
        }
        countObjectId(root.get("mvp"), source, count);
        countObjectId(root.get("svp"), source, count);
        countObjectId(root.get("homeFieldingPitcherRow"), source, count);
        countObjectId(root.get("awayFieldingPitcherRow"), source, count);
    }

    private static void rewriteRoster(ObjectNode root, long source, long target, int[] count) {
        for (String field : ROSTER_ARRAYS) {
            JsonNode node = root.get(field);
            if (!(node instanceof ArrayNode roster)) {
                continue;
            }
            int sourceIndex;
            while ((sourceIndex = rosterIndex(roster, source)) >= 0) {
                String targetField = rosterFieldOf(root, target);
                if (field.equals(targetField)) {
                    throw new IllegalArgumentException("目标球员已在同一阵容或替补名单中，请先调整阵容");
                }
                if (field.endsWith("Bench") && targetField != null
                        && targetField.endsWith("Lineup")) {
                    roster.remove(sourceIndex);
                    count[0] += 1;
                    continue;
                }
                if (roster.get(sourceIndex) instanceof ObjectNode player) {
                    player.put("id", target);
                    if (player.has("playerId")) {
                        player.put("playerId", target);
                    }
                }
                if (targetField != null && targetField.endsWith("Bench")) {
                    ArrayNode targetRoster = (ArrayNode) root.get(targetField);
                    int duplicateIndex = rosterIndex(targetRoster, target);
                    if (duplicateIndex >= 0) {
                        targetRoster.remove(duplicateIndex);
                    }
                }
                count[0] += 1;
            }
        }
    }

    private static String rosterFieldOf(ObjectNode root, long playerId) {
        for (String field : ROSTER_ARRAYS) {
            if (rosterIndex(root.get(field), playerId) >= 0) {
                return field;
            }
        }
        return null;
    }

    private static void rewritePlayerStats(ObjectNode root, long source, long target, int[] count) {
        JsonNode node = root.get("playerStatsById");
        if (!(node instanceof ObjectNode stats)) {
            return;
        }
        String sourceKey = String.valueOf(source);
        String targetKey = String.valueOf(target);
        JsonNode sourceStats = stats.get(sourceKey);
        if (sourceStats == null) {
            return;
        }
        JsonNode targetStats = stats.get(targetKey);
        if (targetStats instanceof ObjectNode targetNode && sourceStats.isObject()) {
            mergeNumbers(targetNode, sourceStats);
        } else if (targetStats == null) {
            stats.set(targetKey, sourceStats.deepCopy());
        }
        stats.remove(sourceKey);
        count[0] += 1;
    }

    private static void mergeNumbers(ObjectNode target, JsonNode source) {
        for (String name : fieldNames(source)) {
            JsonNode sourceValue = source.get(name);
            JsonNode targetValue = target.get(name);
            if (targetValue == null) {
                target.set(name, sourceValue.deepCopy());
            } else if (targetValue.isObject() && sourceValue.isObject()) {
                mergeNumbers((ObjectNode) targetValue, sourceValue);
            } else if (targetValue.isNumber() && sourceValue.isNumber()) {
                if ("ip".equals(name)) {
                    target.put(name, outsToIp(ipToOuts(targetValue.asDouble())
                            + ipToOuts(sourceValue.asDouble())));
                } else if ("gs".equals(name)) {
                    target.put(name, Math.max(targetValue.asInt(), sourceValue.asInt()));
                } else if (targetValue.isIntegralNumber() && sourceValue.isIntegralNumber()) {
                    target.put(name, targetValue.asLong() + sourceValue.asLong());
                } else {
                    target.put(name, targetValue.asDouble() + sourceValue.asDouble());
                }
            }
        }
    }

    private static int ipToOuts(double inningsPitched) {
        int whole = (int) Math.floor(inningsPitched);
        int tenths = (int) Math.round((inningsPitched - whole) * 10);
        return whole * 3 + Math.min(2, Math.max(0, tenths));
    }

    private static double outsToIp(int outs) {
        return (outs / 3) + (outs % 3) / 10.0;
    }

    private static void rewriteIdList(JsonNode node, long source, long target,
            int[] count, boolean dedupe) {
        if (node.isArray()) {
            rewriteNumberArray((ArrayNode) node, source, target, count, dedupe);
        } else if (node.isObject()) {
            rewriteNumberValues((ObjectNode) node, source, target, count);
        }
    }

    private static void countIdList(JsonNode node, long source, int[] count) {
        if (node.isArray()) {
            countNumberArray(node, source, count);
        } else if (node.isObject()) {
            countNumberValues((ObjectNode) node, source, count);
        }
    }

    private static void rewriteNumberArray(ArrayNode array, long source, long target,
            int[] count, boolean dedupe) {
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<JsonNode> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isIntegralNumber()) {
                values.add(item.deepCopy());
                continue;
            }
            long value = item.asLong();
            if (value == source) {
                value = target;
                count[0] += 1;
            }
            if (!dedupe || seen.add(value)) {
                values.add(array.numberNode(value));
            }
        }
        array.removeAll();
        values.forEach(array::add);
    }

    private static void countNumberArray(JsonNode node, long source, int[] count) {
        if (!node.isArray()) {
            return;
        }
        for (JsonNode item : node) {
            if (isPlayerId(item, source)) {
                count[0] += 1;
            }
        }
    }

    private static void rewriteNumberValues(ObjectNode object, long source, long target, int[] count) {
        for (String name : fieldNames(object)) {
            if (isPlayerId(object.get(name), source)) {
                object.put(name, target);
                count[0] += 1;
            }
        }
    }

    private static void countNumberValues(ObjectNode object, long source, int[] count) {
        for (String name : fieldNames(object)) {
            if (isPlayerId(object.get(name), source)) {
                count[0] += 1;
            }
        }
    }

    private static void rewriteObjectId(JsonNode node, long source, long target, int[] count) {
        if (node instanceof ObjectNode object && isPlayerId(object.get("id"), source)) {
            object.put("id", target);
            count[0] += 1;
        }
    }

    private static void countObjectId(JsonNode node, long source, int[] count) {
        if (node instanceof ObjectNode object && isPlayerId(object.get("id"), source)) {
            count[0] += 1;
        }
    }

    private static boolean isPlayerId(JsonNode node, long playerId) {
        return node != null && node.isIntegralNumber() && node.asLong() == playerId;
    }

    private static int rosterIndex(JsonNode roster, long playerId) {
        if (roster == null || !roster.isArray()) {
            return -1;
        }
        for (int index = 0; index < roster.size(); index += 1) {
            if (isPlayerId(roster.get(index).get("id"), playerId)) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
