package com.bsball.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.model.dto.StatRowDeleteCheckDTO;
import com.bsball.model.entity.GamePlayerStat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GameStatDeleteDiagnostics：统计行删除诊断")
class GameStatDeleteDiagnosticsTest {

    @Test
    @DisplayName("全零且无领域阻断时可删除")
    void zeroRowWithoutBlockers_isDeletable() {
        StatRowDeleteCheckDTO result = GameStatDeleteDiagnostics.build(
                pitcherRow(), 0, 0, 0);

        assertTrue(result.deletable());
        assertEquals("NONE", result.recommendedAction());
        assertTrue(result.blockers().isEmpty());
        assertEquals(700L, result.playerId());
        assertTrue(result.isPitcher());
    }

    @Test
    @DisplayName("剩余字段、ER 判定、守备补记与守备位置全部列出，ER 优先")
    void blockersCoverFieldsAndDomainData() {
        GamePlayerStat row = pitcherRow();
        row.setPitchPa(11);
        row.setNp(27);
        row.setFo(3);

        StatRowDeleteCheckDTO result = GameStatDeleteDiagnostics.build(
                row, 2, 1, 1);

        assertFalse(result.deletable());
        assertEquals("ER_REVIEW", result.recommendedAction());
        assertEquals(6, result.blockers().size());
        assertEquals("DATA_FIELD", result.blockers().get(0).type());
        assertEquals("pitchPa", result.blockers().get(0).key());
        assertEquals("PA(投手)", result.blockers().get(0).label());
        assertEquals(11d, result.blockers().get(0).value());
        assertEquals("TRANSFER", result.blockers().get(0).recommendedAction());
        assertEquals("ER_DECISION", result.blockers().get(3).type());
        assertEquals(2d, result.blockers().get(3).value());
        assertEquals("FIELDING_ADJUSTMENT", result.blockers().get(4).type());
        assertEquals("FIELDING_POSITION", result.blockers().get(5).type());
    }

    @Test
    @DisplayName("只有守备阻断时推荐去守备补记；非投手剩余字段推荐清零")
    void recommendedActionFollowsBlockerType() {
        StatRowDeleteCheckDTO fieldingOnly = GameStatDeleteDiagnostics.build(
                pitcherRow(), 0, 1, 0);
        assertEquals("FIELDING_REVIEW", fieldingOnly.recommendedAction());

        StatRowDeleteCheckDTO positionOnly = GameStatDeleteDiagnostics.build(
                pitcherRow(), 0, 0, 1);
        assertEquals("TRANSFER", positionOnly.recommendedAction());
        assertEquals("TRANSFER", positionOnly.blockers().get(0).recommendedAction());

        GamePlayerStat catcher = pitcherRow();
        catcher.setIsPitcher(0);
        StatRowDeleteCheckDTO catcherPosition = GameStatDeleteDiagnostics.build(
                catcher, 0, 0, 1);
        assertEquals("FIELDING_REVIEW", catcherPosition.recommendedAction());

        GamePlayerStat runRow = pitcherRow();
        runRow.setPitchR(2);
        StatRowDeleteCheckDTO runFields = GameStatDeleteDiagnostics.build(
                runRow, 0, 0, 0);
        assertEquals("ER_REVIEW", runFields.recommendedAction());
        assertEquals("ER_REVIEW", runFields.blockers().get(0).recommendedAction());

        GamePlayerStat batter = pitcherRow();
        batter.setIsPitcher(0);
        batter.setH(1);
        StatRowDeleteCheckDTO batterFields = GameStatDeleteDiagnostics.build(
                batter, 0, 0, 0);
        assertEquals("CLEAR_FIELDS", batterFields.recommendedAction());
        assertEquals("CLEAR_FIELDS", batterFields.blockers().get(0).recommendedAction());
        assertFalse(batterFields.isPitcher());
    }

    private static GamePlayerStat pitcherRow() {
        GamePlayerStat row = new GamePlayerStat();
        row.setId(500L);
        row.setGameId(20L);
        row.setTenantId(1L);
        row.setTeamId(1L);
        row.setPlayerId(700L);
        row.setIsPitcher(1);
        return row;
    }
}
