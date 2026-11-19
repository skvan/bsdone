package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSnapshotRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 结果页逐位置守备编辑：save-result 携带 fieldingPlayerParts 时按球员整体替换逐位置行，
 * 并由逐位置行重算球员级守备合计（Issue #701 后续）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService.saveResult：逐位置守备按球员整体替换")
class GameServiceSaveResultFieldingPartsTest {
    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 282L;
    private static final long EVENT_ID = 18L;
    private static final long TEAM_ID = 1L;
    private static final long PLAYER_ID = 26L;

    @Mock private GameRepository gameRepository;
    @Mock private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock private GameSubstitutionEventRepository gameSubstitutionEventRepository;
    @Mock private GameFieldingPosStatRepository gameFieldingPosStatRepository;
    @Mock private EventRepository eventRepository;
    @Mock private StadiumRepository stadiumRepository;
    @Mock private AccountScopeService accountScopeService;
    @Mock private ScopeQuerySupport scopeQuerySupport;
    @Mock private ResourceGuard resourceGuard;
    @Mock private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock private EarnedRunReconstructionService earnedRunReconstructionService;
    @Mock private EventRegistrationService eventRegistrationService;
    @Mock private GameFieldingAdjustmentService fieldingAdjustmentService;
    @Mock private GameRevisionAuditService revisionAuditService;
    @Mock private GameSnapshotRepository gameSnapshotRepository;

    private GameService gameService;
    private Game game;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository,
                eventRepository, stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService,
                gameFieldingPosStatRepository, fieldingAdjustmentService, revisionAuditService,
                gameSnapshotRepository);
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));
        game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        game.setHomeTeamId(Long.valueOf(TEAM_ID));
        game.setStatus("final");
        game.setIsSpecialResult(Boolean.TRUE);
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));
        lenient().when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID)))
                .thenReturn(new ArrayList<>());
        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(gameFieldingPosStatRepository.saveAll(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("按球员整体替换：重建两行并重算球员合计")
    void saveResult_replacesPlayerPositionsAndRecomputesTotals() {
        GameFieldingPosStat old = positionRow("SS", 12, 0, 0, 2, true);
        old.setId(9001L);
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(old)));
        when(gamePlayerStatRepository.findByGameId(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(existingStatRow())));

        SaveGameResultDTO.FieldingPlayerPart part = new SaveGameResultDTO.FieldingPlayerPart();
        part.setTeamId(TEAM_ID);
        part.setPlayerId(PLAYER_ID);
        part.setRows(List.of(
                rowPart("SS", 9, 1, 2, 0),
                rowPart("P", 3, 0, 0, 0)));

        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setFieldingPlayerParts(List.of(part));
        dto.setStats(new ArrayList<>());

        gameService.saveResult(GAME_ID, dto);

        ArgumentCaptor<List<GameFieldingPosStat>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameFieldingPosStatRepository)
                .deleteByGameIdAndTeamIdAndPlayerId(GAME_ID, TEAM_ID, PLAYER_ID);
        verify(gameFieldingPosStatRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals("RESULT", captor.getValue().get(0).getSource());
        GamePlayerStat stat = gamePlayerStatRepository.findByGameId(GAME_ID).get(0);
        assertEquals(Integer.valueOf(1), stat.getPo());
        assertEquals(Integer.valueOf(2), stat.getA());
        assertEquals(Integer.valueOf(0), stat.getE());
        assertEquals(Double.valueOf(4.0), stat.getDefInn());
        assertEquals(Integer.valueOf(3), stat.getTc());
    }

    @Test
    @DisplayName("rows 为空：清空该球员逐位置行，合计归零")
    void saveResult_emptyRows_clearsPositionsAndTotals() {
        GameFieldingPosStat old = positionRow("SS", 12, 1, 0, 2, true);
        old.setId(9001L);
        stubExistingRows(List.of(old), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID, List.of());

        gameService.saveResult(GAME_ID, dtoWith(part));

        verify(gameFieldingPosStatRepository)
                .deleteByGameIdAndTeamIdAndPlayerId(GAME_ID, TEAM_ID, PLAYER_ID);
        verify(gameFieldingPosStatRepository, never()).saveAll(any());
        GamePlayerStat stat = gamePlayerStatRepository.findByGameId(GAME_ID).get(0);
        assertEquals(Integer.valueOf(0), stat.getPo());
        assertEquals(Integer.valueOf(0), stat.getA());
        assertEquals(Integer.valueOf(0), stat.getE());
        assertEquals(Double.valueOf(0.0), stat.getDefInn());
    }

    @Test
    @DisplayName("非 final：拒绝逐位置编辑")
    void saveResult_nonFinal_rejected() {
        game.setStatus("live");
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 1, 0, 0)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dtoWith(part)));

        assertEquals(400, error.getCode());
        assertEquals("比赛结束后才能编辑逐位置守备", error.getMessage());
        verify(gameFieldingPosStatRepository, never())
                .deleteByGameIdAndTeamIdAndPlayerId(any(), any(), any());
    }

    @Test
    @DisplayName("重复守位：400")
    void saveResult_duplicatePosition_rejected() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID, List.of(
                rowPart("SS", 3, 0, 0, 0),
                rowPart("SS", 6, 0, 0, 0)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dtoWith(part)));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("球员不在本场统计行：400")
    void saveResult_playerNotInGame_rejected() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(
                TEAM_ID, Long.valueOf(999999L), List.of(rowPart("SS", 3, 0, 0, 0)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dtoWith(part)));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("不在本场统计行"));
    }

    @Test
    @DisplayName("逐位置提交优先：已被替换的球员不再被 stats 差额回写")
    void saveResult_replacementRunsAfterAllStatsWrites() {
        GameFieldingPosStat old = positionRow("SS", 9, 0, 0, 0, false);
        old.setId(9001L);
        stubExistingRows(List.of(old), List.of(existingStatRow()));
        SaveGameResultDTO.StatPart statPart = new SaveGameResultDTO.StatPart();
        statPart.setTeamId(Long.valueOf(TEAM_ID));
        statPart.setPlayerId(Long.valueOf(PLAYER_ID));
        statPart.setIsPitcher(Integer.valueOf(0));
        statPart.setPo(Integer.valueOf(5));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID,
                List.of(rowPart("SS", 9, 2, 0, 0)));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(statPart)));
        dto.setFieldingPlayerParts(List.of(part));

        gameService.saveResult(GAME_ID, dto);

        ArgumentCaptor<List<GameFieldingPosStat>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameFieldingPosStatRepository, times(1)).saveAll(captor.capture());
        List<GameFieldingPosStat> lastSaved = captor.getAllValues().get(0);
        assertEquals(1, lastSaved.size());
        assertEquals(Integer.valueOf(2), lastSaved.get(0).getPo());
        assertEquals(Integer.valueOf(0), old.getPo(), "旧行不再被 stats 差额改写");
        GamePlayerStat stat = gamePlayerStatRepository.findByGameId(GAME_ID).get(0);
        assertEquals(Integer.valueOf(2), stat.getPo());
    }

    @Test
    @DisplayName("非 final 且未送 stats：仍拒绝逐位置编辑")
    void saveResult_nonFinalWithoutStats_rejected() {
        game.setStatus("live");
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 1, 0, 0)));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setFieldingPlayerParts(List.of(part));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dto));

        assertEquals(400, error.getCode());
        assertEquals("比赛结束后才能编辑逐位置守备", error.getMessage());
        verify(gameFieldingPosStatRepository, never())
                .deleteByGameIdAndTeamIdAndPlayerId(any(), any(), any());
    }

    @Test
    @DisplayName("二刀流：守备合计只重算打者行，投手行不变")
    void saveResult_twoWayPlayer_updatesBatterRowOnly() {
        GamePlayerStat batter = existingStatRow();
        GamePlayerStat pitcher = new GamePlayerStat();
        pitcher.setId(Long.valueOf(2L));
        pitcher.setTenantId(Long.valueOf(TENANT_ID));
        pitcher.setGameId(Long.valueOf(GAME_ID));
        pitcher.setTeamId(Long.valueOf(TEAM_ID));
        pitcher.setPlayerId(Long.valueOf(PLAYER_ID));
        pitcher.setIsPitcher(Integer.valueOf(1));
        pitcher.setPo(Integer.valueOf(5));
        pitcher.setA(Integer.valueOf(4));
        pitcher.setE(Integer.valueOf(1));
        pitcher.setDefInn(Double.valueOf(6.0));
        pitcher.setTc(Integer.valueOf(10));
        stubExistingRows(List.of(), List.of(batter, pitcher));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID,
                List.of(rowPart("SS", 9, 1, 2, 0), rowPart("P", 3, 0, 0, 0)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        ArgumentCaptor<GamePlayerStat> saved = ArgumentCaptor.forClass(GamePlayerStat.class);
        verify(gamePlayerStatRepository).save(saved.capture());
        assertSame(batter, saved.getValue());
        assertEquals(Integer.valueOf(1), batter.getPo());
        assertEquals(Integer.valueOf(2), batter.getA());
        assertEquals(Integer.valueOf(0), batter.getE());
        assertEquals(Integer.valueOf(3), batter.getTc());
        assertEquals(Integer.valueOf(5), pitcher.getPo());
        assertEquals(Integer.valueOf(4), pitcher.getA());
        assertEquals(Integer.valueOf(1), pitcher.getE());
        assertEquals(Double.valueOf(6.0), pitcher.getDefInn());
        assertEquals(Integer.valueOf(10), pitcher.getTc());
    }

    @Test
    @DisplayName("rows 缺失（null）：400，不能当作清空")
    void saveResult_nullRows_rejected() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID, null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dtoWith(part)));

        assertEquals(400, error.getCode());
        assertEquals("缺少逐位置行", error.getMessage());
        verify(gameFieldingPosStatRepository, never())
                .deleteByGameIdAndTeamIdAndPlayerId(any(), any(), any());
    }

    @Test
    @DisplayName("非法守位 DH：400")
    void saveResult_invalidPosition_rejected() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID,
                List.of(rowPart("DH", 3, 0, 0, 0)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dtoWith(part)));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("逐位置守备位置无效"));
        verify(gameFieldingPosStatRepository, never())
                .deleteByGameIdAndTeamIdAndPlayerId(any(), any(), any());
    }

    @Test
    @DisplayName("接杀数 999 合法：落库并重算球员合计")
    void saveResult_poUpperBoundary_accepted() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID,
                List.of(rowPart("SS", 3, 999, 0, 0)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        ArgumentCaptor<List<GameFieldingPosStat>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameFieldingPosStatRepository).saveAll(captor.capture());
        assertEquals(Integer.valueOf(999), captor.getValue().get(0).getPo());
        GamePlayerStat stat = gamePlayerStatRepository.findByGameId(GAME_ID).get(0);
        assertEquals(Integer.valueOf(999), stat.getPo());
        assertEquals(Integer.valueOf(999), stat.getTc());
    }

    @Test
    @DisplayName("接杀数 1000 越界：400")
    void saveResult_poOverLimit_rejected() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part = playerPart(TEAM_ID, PLAYER_ID,
                List.of(rowPart("SS", 3, 1000, 0, 0)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dtoWith(part)));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("接杀数无效"));
        verify(gameFieldingPosStatRepository, never())
                .deleteByGameIdAndTeamIdAndPlayerId(any(), any(), any());
    }

    @Test
    @DisplayName("逐位置替换写台账：删除/新增/修改字段均记录")
    void saveResult_recordsFieldingPositionLedger() {
        GameFieldingPosStat old = positionRow("SS", 12, 0, 0, 2, true);
        old.setId(9001L);
        stubExistingRows(List.of(old), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 1, 0, 1)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        verify(revisionAuditService, atLeastOnce()).recordAdjustment(
                eq(TENANT_ID), eq(GAME_ID), eq("FIELDPOS_STAT"), eq("UPDATE_FIELD"),
                eq("RESULT_SAVE"), any(), eq(TEAM_ID), eq(PLAYER_ID), isNull(),
                eq("po"), eq("PO"), eq("0"), eq("1"), eq("ACTIVE"), isNull(), eq(USER_ID));
        verify(revisionAuditService, atLeastOnce()).recordAdjustment(
                eq(TENANT_ID), eq(GAME_ID), eq("FIELDPOS_STAT"), eq("UPDATE_FIELD"),
                eq("RESULT_SAVE"), any(), eq(TEAM_ID), eq(PLAYER_ID), isNull(),
                eq("e"), eq("E"), eq("2"), eq("1"), eq("ACTIVE"), isNull(), eq(USER_ID));
    }

    @Test
    @DisplayName("逐位置替换写台账：新增守位记 ADD_ROW，移除守位记 DELETE_ROW")
    void saveResult_recordsAddedAndDeletedPositionRows() {
        GameFieldingPosStat old = positionRow("SS", 12, 1, 0, 0, true);
        old.setId(9001L);
        stubExistingRows(List.of(old), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("P", 3, 0, 0, 0)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        verify(revisionAuditService, atLeastOnce()).recordAdjustment(
                eq(TENANT_ID), eq(GAME_ID), eq("FIELDPOS_STAT"), eq("ADD_ROW"),
                eq("RESULT_SAVE"), any(), eq(TEAM_ID), eq(PLAYER_ID), isNull(),
                eq("position"), eq("守位"), eq(""), eq("P"), eq("ACTIVE"), isNull(), eq(USER_ID));
        verify(revisionAuditService, atLeastOnce()).recordAdjustment(
                eq(TENANT_ID), eq(GAME_ID), eq("FIELDPOS_STAT"), eq("DELETE_ROW"),
                eq("RESULT_SAVE"), any(), eq(TEAM_ID), eq(PLAYER_ID), isNull(),
                eq("position"), eq("守位"), eq("SS"), eq(""), eq("ACTIVE"), isNull(), eq(USER_ID));
        verify(revisionAuditService, atLeastOnce()).recordAdjustment(
                eq(TENANT_ID), eq(GAME_ID), eq("FIELDPOS_STAT"), eq("DELETE_ROW"),
                eq("RESULT_SAVE"), any(), eq(TEAM_ID), eq(PLAYER_ID), isNull(),
                eq("po"), eq("PO"), eq("1"), eq(""), eq("ACTIVE"), isNull(), eq(USER_ID));
    }

    @Test
    @DisplayName("逐位置替换：删除后先 flush 再插入，合计重算写入 PLAYER_STAT 台账")
    void saveResult_recomputeRecordsPlayerStatLedgerAfterFlush() {
        GameFieldingPosStat old = positionRow("SS", 12, 0, 0, 0, false);
        old.setId(9001L);
        stubExistingRows(List.of(old), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 2, 1, 1)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        InOrder inOrder = inOrder(gameFieldingPosStatRepository);
        inOrder.verify(gameFieldingPosStatRepository)
                .deleteByGameIdAndTeamIdAndPlayerId(GAME_ID, TEAM_ID, PLAYER_ID);
        inOrder.verify(gameFieldingPosStatRepository).flush();
        inOrder.verify(gameFieldingPosStatRepository).saveAll(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameRevisionAuditService.StatChange>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(revisionAuditService, atLeastOnce())
                .recordStatChanges(eq(game), captor.capture(), eq(USER_ID));
        List<GameRevisionAuditService.StatChange> changes = captor.getAllValues().stream()
                .flatMap(List::stream)
                .toList();
        assertTrue(changes.stream().anyMatch(change ->
                "po".equals(change.fieldKey())
                        && change.beforeValue() == 0d
                        && change.afterValue() == 2d), "PO 0→2 应入账");
        assertTrue(changes.stream().anyMatch(change ->
                "defInn".equals(change.fieldKey())
                        && change.beforeValue() == 0d
                        && change.afterValue() == 4d), "守备局数 0→4 应入账");
    }

    @Test
    @DisplayName("逐位置保存不重放守备补记 A")
    void saveResult_doesNotReplayFieldingAdjustments() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 1, 0, 0)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        verify(fieldingAdjustmentService, never()).reapplyPositionStats(any());
        verify(fieldingAdjustmentService, never()).reapplyPlayerStats(any(), any());
    }

    @Test
    @DisplayName("第一位球员合法、第二位非法：400 且不落库、不写任何台账（台账延迟到全部校验通过）")
    void saveResult_invalidLaterPlayer_writesNoLedger() {
        stubExistingRows(List.of(), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart legal =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 1, 0, 0)));
        SaveGameResultDTO.FieldingPlayerPart invalid = playerPart(
                TEAM_ID, Long.valueOf(888888L), List.of(rowPart("SS", 12, 1, 0, 0)));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setFieldingPlayerParts(List.of(legal, invalid));

        BusinessException error = assertThrows(BusinessException.class,
                () -> gameService.saveResult(GAME_ID, dto));

        assertEquals(400, error.getCode());
        assertTrue(error.getMessage().contains("不在本场统计行"));
        verify(revisionAuditService, never()).recordAdjustment(
                anyLong(), anyLong(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any());
        verify(revisionAuditService, never()).recordStatChanges(any(), any(), any());
        verify(gameFieldingPosStatRepository, never()).saveAll(any());
        verify(gameFieldingPosStatRepository, never()).flush();
        verify(gameFieldingPosStatRepository, never())
                .deleteByGameIdAndTeamIdAndPlayerId(any(), any(), any());
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("台账记录 GS：before/after 用 1/0")
    void saveResult_recordsFieldingGsLedger() {
        GameFieldingPosStat old = positionRow("SS", 12, 1, 0, 0, false);
        old.setId(9001L);
        stubExistingRows(List.of(old), List.of(existingStatRow()));
        SaveGameResultDTO.FieldingPlayerPart part =
                playerPart(TEAM_ID, PLAYER_ID, List.of(rowPart("SS", 12, 1, 0, 0)));

        gameService.saveResult(GAME_ID, dtoWith(part));

        verify(revisionAuditService, atLeastOnce()).recordAdjustment(
                eq(TENANT_ID), eq(GAME_ID), eq("FIELDPOS_STAT"), eq("UPDATE_FIELD"),
                eq("RESULT_SAVE"), any(), eq(TEAM_ID), eq(PLAYER_ID), isNull(),
                eq("fieldingGs"), eq("GS"), eq("0"), eq("1"), eq("ACTIVE"), isNull(), eq(USER_ID));
    }

    private void stubExistingRows(
            List<GameFieldingPosStat> positions, List<GamePlayerStat> stats) {
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(GAME_ID))
                .thenReturn(new ArrayList<>(positions));
        when(gamePlayerStatRepository.findByGameId(GAME_ID))
                .thenReturn(new ArrayList<>(stats));
    }

    private SaveGameResultDTO.FieldingPlayerPart playerPart(
            Long teamId, Long playerId, List<SaveGameResultDTO.FieldingPositionRowPart> rows) {
        SaveGameResultDTO.FieldingPlayerPart part = new SaveGameResultDTO.FieldingPlayerPart();
        part.setTeamId(teamId);
        part.setPlayerId(playerId);
        part.setRows(rows);
        return part;
    }

    private SaveGameResultDTO dtoWith(SaveGameResultDTO.FieldingPlayerPart part) {
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setFieldingPlayerParts(List.of(part));
        dto.setStats(new ArrayList<>());
        return dto;
    }

    private GamePlayerStat existingStatRow() {
        GamePlayerStat stat = new GamePlayerStat();
        stat.setId(Long.valueOf(1L));
        stat.setTenantId(Long.valueOf(TENANT_ID));
        stat.setGameId(Long.valueOf(GAME_ID));
        stat.setTeamId(Long.valueOf(TEAM_ID));
        stat.setPlayerId(Long.valueOf(PLAYER_ID));
        stat.setIsPitcher(Integer.valueOf(0));
        stat.setFirstPosition("SS");
        stat.setPosition("SS");
        stat.setFieldingGs(Integer.valueOf(1));
        return stat;
    }

    private SaveGameResultDTO.FieldingPositionRowPart rowPart(
            String position, int outs, int po, int a, int e) {
        SaveGameResultDTO.FieldingPositionRowPart row = new SaveGameResultDTO.FieldingPositionRowPart();
        row.setPosition(position);
        row.setOuts(Integer.valueOf(outs));
        row.setPo(Integer.valueOf(po));
        row.setA(Integer.valueOf(a));
        row.setE(Integer.valueOf(e));
        return row;
    }

    private GameFieldingPosStat positionRow(
            String position, int outs, int po, int a, int e, boolean fieldingGs) {
        GameFieldingPosStat row = new GameFieldingPosStat();
        row.setId(Long.valueOf(1000L + Math.abs(position.hashCode() % 100)));
        row.setTenantId(Long.valueOf(TENANT_ID));
        row.setGameId(Long.valueOf(GAME_ID));
        row.setTeamId(Long.valueOf(TEAM_ID));
        row.setPlayerId(Long.valueOf(PLAYER_ID));
        row.setPosition(position);
        row.setOuts(Integer.valueOf(outs));
        row.setPo(Integer.valueOf(po));
        row.setA(Integer.valueOf(a));
        row.setE(Integer.valueOf(e));
        row.setDp(Integer.valueOf(0));
        row.setPb(Integer.valueOf(0));
        row.setCatcherCs(Integer.valueOf(0));
        row.setCatcherSb(Integer.valueOf(0));
        row.setFieldingGs(Boolean.valueOf(fieldingGs));
        return row;
    }
}
