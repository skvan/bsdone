package com.bsball.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LiveSnapshotIdentityTransferTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("现场记录改写：Play、阵容、统计缓存、跑者与替补历史全部指向目标球员")
    void rewriteMovesAllPlayerReferences() throws Exception {
        String json = """
                {
                  "gameId": 10,
                  "homeLineup": [{"id": 1, "name": "甲"}, {"id": 102, "name": "野手"}],
                  "homeBench": [{"id": 1, "name": "甲重复"}],
                  "awayLineup": [{"id": 2, "name": "乙"}],
                  "homeFieldingPitcherId": 1,
                  "awayFieldingPitcherId": 2,
                  "homeFieldingPitcherRow": {"id": 1, "name": "甲"},
                  "homeLastDhAddedFromPoolId": 1,
                  "homeUnavailablePlayerIds": [1, 3],
                  "homeLineupStarterIds": [1, null, 4],
                  "homeSlotSubHistory": [[1, 5], [1]],
                  "mvp": {"id": 1, "name": "甲"},
                  "gameState": {
                    "inning": 3, "batterIndex": {"home": 1}, "runners": {"1": 1, "3": 1},
                    "mvp": 1, "svp": null,
                    "flow": {"runner": 1}, "playEvents": []
                  },
                  "playEvents": [
                    {
                      "playId": "p1", "batterPlayerId": 1, "pitcherPlayerId": 2,
                      "runnerDragCiRunnerId": 1,
                      "basesBefore": {"1": 2},
                      "basesAfter": {"1": 1, "2": 1},
                      "runnerIdsBefore": {"1": 1, "2": 2, "3": 1},
                      "scoringRunnerIds": [1],
                      "scoringRunners": [
                        {"runnerId": 1, "responsiblePitcherId": 2},
                        {"runnerId": 2, "responsiblePitcherId": 1}
                      ]
                    }
                  ],
                  "atBatSummaries": [{"batterId": 1, "pitcherId": 2}],
                  "runLedger": [{"runnerId": 1}],
                  "playerStatsById": {
                    "1": {"batting": {"pa": 2, "h": 1}, "fielding": {"po": 1},
                          "pitching": {"ip": 0.2, "gs": 1}},
                    "101": {"batting": {"pa": 3, "h": 0}, "pitching": {"ip": 0.1, "gs": 1}}
                  },
                  "undoHistory": [{"playEvents": [{"batterPlayerId": 1}]}],
                  "redoHistory": [{"playEvents": [{"batterPlayerId": 1}]}]
                }
                """;

        var result = LiveSnapshotIdentityTransfer.rewrite(json, 1L, 101L, mapper);
        JsonNode root = mapper.readTree(result.json());

        assertTrue(result.changed());
        assertEquals(2, root.get("homeLineup").size());
        assertEquals(101, root.get("homeLineup").get(0).get("id").asLong());
        assertEquals(102, root.get("homeLineup").get(1).get("id").asLong());
        assertEquals(0, root.get("homeBench").size());
        assertEquals(101, root.get("homeFieldingPitcherId").asLong());
        assertEquals(101, root.get("homeFieldingPitcherRow").get("id").asLong());
        assertEquals(101, root.get("homeLastDhAddedFromPoolId").asLong());
        assertEquals(101, root.get("homeUnavailablePlayerIds").get(0).asLong());
        assertEquals(101, root.get("homeLineupStarterIds").get(0).asLong());
        assertEquals(101, root.get("mvp").get("id").asLong());
        JsonNode gameState = root.get("gameState");
        assertEquals(101, gameState.get("runners").get("1").asLong());
        assertEquals(101, gameState.get("runners").get("3").asLong());
        assertEquals(101, gameState.get("mvp").asLong());
        assertTrue(gameState.get("svp").isNull());
        assertEquals(101, gameState.get("flow").get("runner").asLong());
        JsonNode play = root.get("playEvents").get(0);
        assertEquals(101, play.get("batterPlayerId").asLong());
        assertEquals(101, play.get("runnerDragCiRunnerId").asLong());
        assertEquals(101, play.get("basesAfter").get("1").asLong());
        assertEquals(101, play.get("basesAfter").get("2").asLong());
        assertEquals(101, play.get("runnerIdsBefore").get("1").asLong());
        assertEquals(2, play.get("runnerIdsBefore").get("2").asLong());
        assertEquals(101, play.get("runnerIdsBefore").get("3").asLong());
        assertEquals(101, play.get("scoringRunners").get(0).get("runnerId").asLong());
        assertEquals(101, play.get("scoringRunners").get(1).get("responsiblePitcherId").asLong());
        assertEquals(101, root.get("atBatSummaries").get(0).get("batterId").asLong());
        assertEquals(101, root.get("runLedger").get(0).get("runnerId").asLong());
        JsonNode stats = root.get("playerStatsById");
        assertFalse(stats.has("1"));
        assertEquals(5, stats.get("101").get("batting").get("pa").asInt());
        assertEquals(1, stats.get("101").get("batting").get("h").asInt());
        assertEquals(1, stats.get("101").get("fielding").get("po").asInt());
        assertEquals(1.0, stats.get("101").get("pitching").get("ip").asDouble());
        assertEquals(1, stats.get("101").get("pitching").get("gs").asInt());
        assertEquals(1, root.get("undoHistory").get(0)
                .get("playEvents").get(0).get("batterPlayerId").asLong());
        assertEquals(1, root.get("redoHistory").get(0)
                .get("playEvents").get(0).get("batterPlayerId").asLong());
        assertTrue(result.references() > 0);
    }

    @Test
    @DisplayName("现场记录改写：目标已在同一阵容时拒绝自动改写")
    void rewriteRejectsRosterConflict() {
        String json = "{\"homeLineup\":[{\"id\":1},{\"id\":101}]}";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> LiveSnapshotIdentityTransfer.rewrite(json, 1L, 101L, mapper));

        assertTrue(error.getMessage().contains("目标球员已在同一阵容"));
        assertTrue(LiveSnapshotIdentityTransfer.hasRosterConflict(json, 1L, 101L, mapper));
    }

    @Test
    @DisplayName("现场记录改写：没有源球员引用时不修改")
    void rewriteLeavesSnapshotWhenSourceMissing() {
        String json = "{\"homeLineup\":[{\"id\":101}],\"playEvents\":[]}";

        var result = LiveSnapshotIdentityTransfer.rewrite(json, 1L, 101L, mapper);

        assertFalse(result.changed());
        assertEquals(0, result.references());
        assertEquals(json, result.json());
    }

    @Test
    @DisplayName("现场记录改写：非法 JSON 返回可读错误")
    void rewriteRejectsInvalidJson() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> LiveSnapshotIdentityTransfer.rewrite("not-json", 1L, 2L, mapper));

        assertTrue(error.getMessage().contains("现场记录 JSON 解析失败"));
    }

    @Test
    @DisplayName("转记审计快照：保留现场数据但剔除撤销历史")
    void auditSnapshotDropsUndoHistory() throws Exception {
        String json = """
                {"gameId":10,"playEvents":[{"batterPlayerId":1}],
                 "homeLineup":[{"id":1}],"undoHistory":[{"x":1}],"redoHistory":[{"x":1}]}
                """;

        JsonNode audit = mapper.readTree(
                LiveSnapshotIdentityTransfer.auditSnapshot(json, mapper));

        assertEquals(10, audit.get("gameId").asInt());
        assertTrue(audit.has("playEvents"));
        assertTrue(audit.has("homeLineup"));
        assertFalse(audit.has("undoHistory"));
        assertFalse(audit.has("redoHistory"));
    }
}
