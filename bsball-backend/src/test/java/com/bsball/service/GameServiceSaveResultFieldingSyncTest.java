package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.GameSnapshotRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Issue #701：结果录入页保存球员级守备合计后，必须把差额回写逐位置守备，
 * 否则下一次加载结果页会用旧的逐位置合计把人工修订值回滚。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService.saveResult：守备合计差额回写逐位置明细")
class GameServiceSaveResultFieldingSyncTest {
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
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("单守位：人工修订的 PO/E 差额回写到该位置行")
    void saveResult_singlePosition_appliesDeltas() {
        GameFieldingPosStat ss = positionRow("SS", 12, 0, 0, 2, true);
        stubPositionRows(ss);
        SaveGameResultDTO.StatPart part = part(1L, 0, 1, 1, 4.0);

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertEquals(Integer.valueOf(1), ss.getPo(), "PO 应由逐位置 0 补到人工值 1");
        assertEquals(Integer.valueOf(1), ss.getE(), "E 应由逐位置 2 扣到人工值 1");
        assertEquals(Integer.valueOf(12), ss.getOuts(), "INN 未变时出局数不动");
        verify(gameFieldingPosStatRepository).saveAll(List.of(ss));
    }

    @Test
    @DisplayName("多守位：正差额落到出场局数最多的位置，负差额从多到少扣减")
    void saveResult_multiPosition_distributesByOuts() {
        GameFieldingPosStat ss = positionRow("SS", 12, 1, 0, 2, true);
        GameFieldingPosStat p = positionRow("P", 3, 0, 0, 0, false);
        stubPositionRows(ss, p);
        SaveGameResultDTO.StatPart part = part(1L, 0, 3, 0, 5.0);

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertEquals(Integer.valueOf(3), ss.getPo(), "PO +2 应落在出场更多的 SS 行");
        assertEquals(Integer.valueOf(0), p.getPo(), "P 行不动");
        assertEquals(Integer.valueOf(0), ss.getE(), "E -2 先从出场更多的 SS 行扣");
        assertEquals(Integer.valueOf(0), p.getE());
        assertEquals(Integer.valueOf(12), ss.getOuts(), "INN 5.0 与逐位置合计 15 出局一致，SS 行不动");
        assertEquals(Integer.valueOf(3), p.getOuts());
        verify(gameFieldingPosStatRepository).saveAll(List.of(ss, p));
    }

    @Test
    @DisplayName("字段缺栏：不把逐位置值误清零")
    void saveResult_nullFieldingField_doesNotZero() {
        GameFieldingPosStat ss = positionRow("SS", 12, 1, 0, 2, true);
        stubPositionRows(ss);
        SaveGameResultDTO.StatPart part = part(1L, 0, null, null, null);

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertEquals(Integer.valueOf(1), ss.getPo());
        assertEquals(Integer.valueOf(2), ss.getE());
        verify(gameFieldingPosStatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("二刀流两段 payload：以打者段（聚合全部守位）为准")
    void saveResult_twoWayParts_prefersBatterPart() {
        GameFieldingPosStat ss = positionRow("SS", 12, 1, 0, 0, true);
        stubPositionRows(ss);
        SaveGameResultDTO.StatPart batter = part(0L, 0, 5, 0, null);
        SaveGameResultDTO.StatPart pitcher = part(1L, 1, null, null, null);

        gameService.saveResult(Long.valueOf(GAME_ID), dto(pitcher, batter));

        assertEquals(Integer.valueOf(5), ss.getPo(), "应采用打者段的守备合计");
    }

    @Test
    @DisplayName("无逐位置行：不回写也不保存")
    void saveResult_noPositionRows_noSync() {
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(Long.valueOf(GAME_ID)))
                .thenReturn(new ArrayList<>());
        SaveGameResultDTO.StatPart part = part(1L, 0, 1, 1, 4.0);

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        verify(gameFieldingPosStatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("捕手字段只落 C 行：多守位球员的 PB 不写进出场更多的其他位置")
    void saveResult_catcherFieldsLandOnCatcherRow() {
        GameFieldingPosStat firstBase = positionRow("1B", 15, 0, 0, 0, true);
        GameFieldingPosStat catcher = positionRow("C", 3, 0, 0, 0, false);
        catcher.setPb(Integer.valueOf(0));
        stubPositionRows(firstBase, catcher);
        SaveGameResultDTO.StatPart part = part(1L, 0, null, null, null);
        part.setPb(Integer.valueOf(2));

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertEquals(Integer.valueOf(2), catcher.getPb(), "PB 应落在 C 行");
        assertEquals(Integer.valueOf(0), firstBase.getPb(), "1B 行不应承接 PB");
        verify(gameFieldingPosStatRepository).saveAll(List.of(firstBase, catcher));
    }

    @Test
    @DisplayName("无 C 行时捕手字段跳过，不改其他守位")
    void saveResult_noCatcherRow_pbIgnored() {
        GameFieldingPosStat firstBase = positionRow("1B", 15, 0, 0, 0, true);
        stubPositionRows(firstBase);
        SaveGameResultDTO.StatPart part = part(1L, 0, null, null, null);
        part.setPb(Integer.valueOf(2));

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertEquals(Integer.valueOf(0), firstBase.getPb());
        verify(gameFieldingPosStatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("GS 归一：目标 1 只保留一行，目标 0 清除所有行")
    void saveResult_fieldingGsNormalized() {
        GameFieldingPosStat shortStop = positionRow("SS", 12, 0, 0, 0, true);
        GameFieldingPosStat pitcher = positionRow("P", 3, 0, 0, 0, true);
        stubPositionRows(shortStop, pitcher);
        SaveGameResultDTO.StatPart part = part(1L, 0, null, null, null);
        part.setFieldingGs(Integer.valueOf(1));

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertTrue(Boolean.TRUE.equals(shortStop.getFieldingGs()), "出场最多的 SS 行保留 GS=1");
        assertFalse(Boolean.TRUE.equals(pitcher.getFieldingGs()), "其他行的 GS 必须清除");

        SaveGameResultDTO.StatPart clear = part(1L, 0, null, null, null);
        clear.setFieldingGs(Integer.valueOf(0));
        gameService.saveResult(Long.valueOf(GAME_ID), dto(clear));
        assertFalse(Boolean.TRUE.equals(shortStop.getFieldingGs()), "目标 0 应清除全部 GS");
        assertFalse(Boolean.TRUE.equals(pitcher.getFieldingGs()));
    }

    @Test
    @DisplayName("INN/GS 也同步：出局数按出场最多的行调整，夺先发标记落到该行")
    void saveResult_syncsOutsAndFieldingGs() {
        GameFieldingPosStat ss = positionRow("SS", 12, 0, 0, 0, true);
        stubPositionRows(ss);
        SaveGameResultDTO.StatPart part = part(1L, 0, 0, 0, 4.1);
        part.setFieldingGs(Integer.valueOf(0));

        gameService.saveResult(Long.valueOf(GAME_ID), dto(part));

        assertEquals(Integer.valueOf(13), ss.getOuts(), "INN 4.1 应换算为 13 个出局");
        assertFalse(Boolean.TRUE.equals(ss.getFieldingGs()), "GS 人工改 0 应回写");
    }

    private void stubPositionRows(GameFieldingPosStat... rows) {
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(Long.valueOf(GAME_ID)))
                .thenReturn(new ArrayList<>(List.of(rows)));
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

    private SaveGameResultDTO dto(SaveGameResultDTO.StatPart... parts) {
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(parts)));
        return dto;
    }

    private SaveGameResultDTO.StatPart part(
            Long id, Integer isPitcher, Integer po, Integer e, Double defInn) {
        SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
        part.setId(id);
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(Long.valueOf(PLAYER_ID));
        part.setIsPitcher(isPitcher);
        part.setPosition("P");
        part.setPo(po);
        part.setE(e);
        part.setDefInn(defInn);
        return part;
    }
}
