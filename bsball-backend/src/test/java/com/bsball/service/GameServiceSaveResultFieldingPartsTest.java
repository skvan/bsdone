package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
