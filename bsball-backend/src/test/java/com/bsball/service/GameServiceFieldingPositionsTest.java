package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService 逐位置守备：全量替换、def_inn 汇总与读取")
class GameServiceFieldingPositionsTest {
    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 500L;
    private static final long EVENT_ID = 700L;
    private static final long HOME_ID = 1L;
    private static final long AWAY_ID = 2L;

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

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository,
                eventRepository, stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService,
                gameFieldingPosStatRepository);
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));
        Game game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        game.setHomeTeamId(Long.valueOf(HOME_ID));
        game.setAwayTeamId(Long.valueOf(AWAY_ID));
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    private static GameSaveLiveDTO.FieldingPositionPart part(long teamId, long playerId, String position, int outs, boolean gs) {
        GameSaveLiveDTO.FieldingPositionPart part = new GameSaveLiveDTO.FieldingPositionPart();
        part.setTeamId(Long.valueOf(teamId));
        part.setPlayerId(Long.valueOf(playerId));
        part.setPosition(position);
        part.setOuts(Integer.valueOf(outs));
        part.setFieldingGs(Boolean.valueOf(gs));
        return part;
    }

    private static GameSaveLiveDTO dtoWith(List<GameSaveLiveDTO.FieldingPositionPart> parts) {
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setFieldingPositions(parts);
        return dto;
    }

    private static GamePlayerStat stat(long teamId, long playerId) {
        GamePlayerStat stat = new GamePlayerStat();
        stat.setGameId(Long.valueOf(GAME_ID));
        stat.setTeamId(Long.valueOf(teamId));
        stat.setPlayerId(Long.valueOf(playerId));
        return stat;
    }

    @Test
    @DisplayName("带 fieldingPositions：全量替换并回写 def_inn（10 outs → 3.1）")
    void saveLive_replacesRowsAndWritesDefInn() {
        GamePlayerStat pitcher = stat(HOME_ID, 101L);
        GamePlayerStat center = stat(HOME_ID, 108L);
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of(pitcher, center));
        List<GameSaveLiveDTO.FieldingPositionPart> parts = new ArrayList<>();
        parts.add(part(HOME_ID, 101L, "P", 10, true));
        parts.add(part(HOME_ID, 108L, "CF", 4, true));

        gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(parts));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameFieldingPosStat>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(gameFieldingPosStatRepository);
        inOrder.verify(gameFieldingPosStatRepository).deleteByGameId(Long.valueOf(GAME_ID));
        inOrder.verify(gameFieldingPosStatRepository).flush();
        inOrder.verify(gameFieldingPosStatRepository).saveAll(captor.capture());
        List<GameFieldingPosStat> rows = captor.getValue();
        assertEquals(2, rows.size());
        GameFieldingPosStat row = rows.get(0);
        assertEquals(HOME_ID, row.getTeamId().longValue());
        assertEquals(101L, row.getPlayerId().longValue());
        assertEquals("P", row.getPosition());
        assertEquals(10, row.getOuts().intValue());
        assertEquals(Boolean.TRUE, row.getFieldingGs());
        assertEquals("LIVE", row.getSource());
        assertEquals(TENANT_ID, row.getTenantId().longValue());
        assertEquals(GAME_ID, row.getGameId().longValue());
        assertEquals(3.1D, pitcher.getDefInn().doubleValue(), 0.0001D);
        assertEquals(1.1D, center.getDefInn().doubleValue(), 0.0001D);
        verify(eventRegistrationService).assertStatsPartsInRoster(eq(Long.valueOf(EVENT_ID)), eq(Long.valueOf(GAME_ID)), anyCollection());
    }

    @Test
    @DisplayName("无 fieldingPositions 键：不触碰逐位置表")
    void saveLive_withoutKey_doesNothing() {
        gameService.saveLive(Long.valueOf(GAME_ID), new GameSaveLiveDTO());
        verifyNoInteractions(gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("空数组：清空该场逐位置行")
    void saveLive_emptyList_clearsRows() {
        gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(List.of()));
        verify(gameFieldingPosStatRepository).deleteByGameId(Long.valueOf(GAME_ID));
        verify(gameFieldingPosStatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("球队不属于本场：400 且不写库")
    void saveLive_foreignTeam_rejects() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(List.of(part(999L, 101L, "P", 1, false)))));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("DH 位置：400 且不写库")
    void saveLive_dhPosition_rejects() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(List.of(part(HOME_ID, 101L, "DH", 1, false)))));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("重复行：400 且不写库")
    void saveLive_duplicateRows_rejects() {
        List<GameSaveLiveDTO.FieldingPositionPart> parts = List.of(
                part(HOME_ID, 101L, "P", 1, false), part(HOME_ID, 101L, "P", 2, false));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(parts)));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("逐位置 PO/A/E/DP/捕手专项：保存到对应行，缺省为 0")
    void saveLive_persistsFieldingCredits() {
        GameSaveLiveDTO.FieldingPositionPart credited = part(HOME_ID, 102L, "C", 4, true);
        credited.setPo(Integer.valueOf(2));
        credited.setA(Integer.valueOf(1));
        credited.setE(Integer.valueOf(1));
        credited.setDp(Integer.valueOf(1));
        credited.setPb(Integer.valueOf(2));
        credited.setCatcherCs(Integer.valueOf(3));
        credited.setCatcherSb(Integer.valueOf(4));
        GameSaveLiveDTO.FieldingPositionPart plain = part(HOME_ID, 101L, "P", 3, true);

        gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(List.of(credited, plain)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameFieldingPosStat>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameFieldingPosStatRepository).saveAll(captor.capture());
        GameFieldingPosStat catcherRow = captor.getValue().get(0);
        assertEquals(2, catcherRow.getPo().intValue());
        assertEquals(1, catcherRow.getA().intValue());
        assertEquals(1, catcherRow.getE().intValue());
        assertEquals(1, catcherRow.getDp().intValue());
        assertEquals(2, catcherRow.getPb().intValue());
        assertEquals(3, catcherRow.getCatcherCs().intValue());
        assertEquals(4, catcherRow.getCatcherSb().intValue());
        GameFieldingPosStat p = captor.getValue().get(1);
        assertEquals(0, p.getPo().intValue());
        assertEquals(0, p.getA().intValue());
        assertEquals(0, p.getE().intValue());
        assertEquals(0, p.getDp().intValue());
        assertEquals(0, p.getPb().intValue());
        assertEquals(0, p.getCatcherCs().intValue());
        assertEquals(0, p.getCatcherSb().intValue());
    }

    @Test
    @DisplayName("逐位置 PO/A/E：负数 400 且不写库")
    void saveLive_negativeCredit_rejects() {
        GameSaveLiveDTO.FieldingPositionPart part = part(HOME_ID, 108L, "CF", 4, true);
        part.setPo(Integer.valueOf(-1));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(List.of(part))));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("逐位置 DP/捕手专项：负数 400 且不写库")
    void saveLive_negativeCatcherCredit_rejects() {
        GameSaveLiveDTO.FieldingPositionPart dp = part(HOME_ID, 102L, "C", 4, true);
        dp.setDp(Integer.valueOf(-1));
        assertEquals(400, assertThrows(BusinessException.class, () ->
                gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(List.of(dp)))).getCode());
        verifyNoInteractions(gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("读取：无权比赛 403；有权返回仓库顺序结果")
    void listFieldingPositions_scoped() {
        GameFieldingPosStat row = new GameFieldingPosStat();
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscPositionAsc(Long.valueOf(GAME_ID)))
                .thenReturn(List.of(row));
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(Boolean.FALSE);
        when(accountScopeService.resolveCurrent()).thenReturn(null);
        when(scopeQuerySupport.visibleLeagueIds(null)).thenReturn(null);

        assertEquals(List.of(row), gameService.listFieldingPositions(Long.valueOf(GAME_ID)));

        when(gameRepository.findById(Long.valueOf(999L))).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> gameService.listFieldingPositions(Long.valueOf(999L)));
        assertEquals(403, ex.getCode());
    }
}
