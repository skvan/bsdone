package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 电子秩序册（Issue #310）阵容/统计门禁测试：
 * GameService 门①（saveLineupSnapshot）/门②（saveLive、saveResult）与 GamePlayerStatService 单条写入校验。
 * 纯单元测试（Mockito mock，不启动 Spring）。
 */
@ExtendWith(MockitoExtension.class)
class GameRosterValidationTest {
    private static final long TENANT_ID = 1L;
    private static final long EVENT_ID = 700L;
    private static final long GAME_ID = 900L;
    private static final long HOME_ID = 100L;
    private static final long AWAY_ID = 200L;

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private GameSubstitutionEventRepository gameSubstitutionEventRepository;
    @Mock
    private GameFieldingPosStatRepository gameFieldingPosStatRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private AccountScopeService accountScopeService;
    @Mock
    private ScopeQuerySupport scopeQuerySupport;
    @Mock
    private ResourceGuard resourceGuard;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock
    private EarnedRunReconstructionService earnedRunReconstructionService;
    @Mock
    private EventRegistrationService eventRegistrationService;

    private GameService gameService;
    private GamePlayerStatService statService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository, eventRepository,
                stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService, gameFieldingPosStatRepository);
        statService = new GamePlayerStatService(gamePlayerStatRepository, gameRepository,
                eventRegistrationService, earnedRunReconstructionService);
    }

    private static Event event() {
        Event e = new Event();
        e.setId(Long.valueOf(EVENT_ID));
        e.setTenantId(Long.valueOf(TENANT_ID));
        return e;
    }

    private static Game game() {
        Game g = new Game();
        g.setId(Long.valueOf(GAME_ID));
        g.setTenantId(Long.valueOf(TENANT_ID));
        g.setEventId(Long.valueOf(EVENT_ID));
        g.setHomeTeamId(Long.valueOf(HOME_ID));
        g.setAwayTeamId(Long.valueOf(AWAY_ID));
        g.setStatus("scheduled");
        return g;
    }

    private void stubWritableContext() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event()));
    }

    @Test
    @DisplayName("门①：阵容保存端点按（赛事,主队,客队,快照）委托校验后落库")
    void saveLineupSnapshot_validatesAndSaves() {
        Game g = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(g));
        stubWritableContext();
        String json = "{\"v\":1,\"homeLineup\":[{\"id\":5}]}";

        gameService.saveLineupSnapshot(GAME_ID, json);

        verify(eventRegistrationService).assertLineupSnapshotInRoster(EVENT_ID, HOME_ID, AWAY_ID, json);
        assertEquals(json, g.getLiveSnapshotJson());
        verify(gameRepository).save(g);
    }

    @Test
    @DisplayName("门①：校验失败（400）阻断落库")
    void saveLineupSnapshot_validationFailure_blocksSave() {
        Game g = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(g));
        stubWritableContext();
        doThrow(new BusinessException(400, "球员「X」不在本赛事秩序册名单中"))
                .when(eventRegistrationService).assertLineupSnapshotInRoster(any(), any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLineupSnapshot(GAME_ID, "{\"v\":1}"));

        assertEquals(400, ex.getCode());
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("门②：saveLive 统计行逐条映射为 StatsRef 校验后落库")
    void saveLive_statsValidation_calledWithMappedRefs() {
        Game g = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(g));
        stubWritableContext();
        when(gamePlayerStatRepository.findByGameId(GAME_ID)).thenReturn(List.of());
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        GameSaveLiveDTO.GamePlayerStatPart part = new GameSaveLiveDTO.GamePlayerStatPart();
        part.setTeamId(Long.valueOf(HOME_ID));
        part.setPlayerId(Long.valueOf(5L));
        dto.setStats(List.of(part));

        gameService.saveLive(GAME_ID, dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EventRegistrationService.StatsRef>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(eventRegistrationService).assertStatsPartsInRoster(eq(EVENT_ID), eq(GAME_ID), captor.capture());
        assertEquals(new EventRegistrationService.StatsRef(HOME_ID, 5L), captor.getValue().iterator().next());
        verify(gamePlayerStatRepository).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("门②：saveResult 按目标赛事口径校验统计行（isPitcher 行不受起手守位校验影响）")
    void saveResult_statsValidation_called() {
        Game g = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(g));
        stubWritableContext();
        when(gamePlayerStatRepository.findByGameId(GAME_ID)).thenReturn(List.of());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
        part.setTeamId(Long.valueOf(HOME_ID));
        part.setPlayerId(Long.valueOf(5L));
        part.setIsPitcher(Integer.valueOf(1));
        dto.setStats(List.of(part));

        gameService.saveResult(GAME_ID, dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EventRegistrationService.StatsRef>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(eventRegistrationService).assertStatsPartsInRoster(eq(EVENT_ID), eq(GAME_ID), captor.capture());
        assertEquals(new EventRegistrationService.StatsRef(HOME_ID, 5L), captor.getValue().iterator().next());
        verify(gamePlayerStatRepository).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("门②：saveResult 同请求迁移 eventId 时按目标赛事口径校验")
    void saveResult_migratesEventId_validatesAgainstTargetEvent() {
        Game g = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(g));
        stubWritableContext();
        when(gamePlayerStatRepository.findByGameId(GAME_ID)).thenReturn(List.of());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setEventId(Long.valueOf(701L));
        dto.setGame(gamePart);
        SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
        part.setTeamId(Long.valueOf(HOME_ID));
        part.setPlayerId(Long.valueOf(5L));
        part.setIsPitcher(Integer.valueOf(1));
        dto.setStats(List.of(part));

        gameService.saveResult(GAME_ID, dto);

        verify(eventRegistrationService).assertStatsPartsInRoster(eq(701L), eq(GAME_ID), anyCollection());
    }

    @Test
    @DisplayName("门②：统计单条创建走校验（游戏→赛事解析）")
    void statCreate_validatesRoster() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        GamePlayerStat stat = new GamePlayerStat();
        stat.setGameId(Long.valueOf(GAME_ID));
        stat.setTeamId(Long.valueOf(HOME_ID));
        stat.setPlayerId(Long.valueOf(5L));

        statService.create(stat);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EventRegistrationService.StatsRef>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(eventRegistrationService).assertStatsPartsInRoster(eq(EVENT_ID), eq(GAME_ID), captor.capture());
        assertEquals(new EventRegistrationService.StatsRef(HOME_ID, 5L), captor.getValue().iterator().next());
        verify(gamePlayerStatRepository).save(stat);
    }

    @Test
    @DisplayName("门②：统计单条更新按合并后最终 team/player 校验（载荷缺省沿用既有值）")
    void statUpdate_validatesMergedIdentity() {
        GamePlayerStat existing = new GamePlayerStat();
        existing.setId(Long.valueOf(1L));
        existing.setGameId(Long.valueOf(GAME_ID));
        existing.setTeamId(Long.valueOf(HOME_ID));
        existing.setPlayerId(Long.valueOf(5L));
        when(gamePlayerStatRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        GamePlayerStat payload = new GamePlayerStat();
        payload.setPlayerId(Long.valueOf(8L));

        statService.update(1L, payload);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EventRegistrationService.StatsRef>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(eventRegistrationService).assertStatsPartsInRoster(eq(EVENT_ID), eq(GAME_ID), captor.capture());
        assertEquals(new EventRegistrationService.StatsRef(HOME_ID, 8L), captor.getValue().iterator().next());
        verify(gamePlayerStatRepository).save(payload);
    }

    // ------------------------------------------------------------------ 报名制门槛接线（Issue #318 决策 11）

    @Test
    @DisplayName("排赛门槛接线：create 建比赛按（赛事,主,客）委托 assertGameTeamsEligible")
    void create_validatesGameTeamsEligible() {
        stubWritableContext();
        Game entity = new Game();
        entity.setEventId(Long.valueOf(EVENT_ID));
        entity.setHomeTeamId(Long.valueOf(HOME_ID));
        entity.setAwayTeamId(Long.valueOf(AWAY_ID));

        gameService.create(entity);

        verify(eventRegistrationService).assertGameTeamsEligible(EVENT_ID, HOME_ID, AWAY_ID);
        verify(gameRepository).save(entity);
    }

    @Test
    @DisplayName("排赛门槛接线：update 改主客队按目标赛事委托 assertGameTeamsEligible")
    void update_validatesGameTeamsEligible() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        stubWritableContext();
        Game entity = new Game();
        entity.setHomeTeamId(Long.valueOf(HOME_ID));
        entity.setAwayTeamId(Long.valueOf(AWAY_ID));

        gameService.update(GAME_ID, entity);

        verify(eventRegistrationService).assertGameTeamsEligible(EVENT_ID, HOME_ID, AWAY_ID);
        verify(gameRepository).save(entity);
    }

    @Test
    @DisplayName("排赛门槛接线：update 迁移赛事且未带球队时，按合并后既有主客队校验（防绕过）")
    void update_moveEventWithoutTeams_validatesMergedTeams() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event target = new Event();
        target.setId(Long.valueOf(701L));
        target.setTenantId(Long.valueOf(TENANT_ID));
        when(eventRepository.findById(701L)).thenReturn(Optional.of(target));
        Game entity = new Game();
        entity.setEventId(Long.valueOf(701L));

        gameService.update(GAME_ID, entity);

        verify(eventRegistrationService).assertGameTeamsEligible(701L, HOME_ID, AWAY_ID);
    }

    @Test
    @DisplayName("排赛门槛接线：saveResult 改主客队时按合并后（eventId,主,客）委托 assertGameTeamsEligible")
    void saveResult_changeTeams_validatesGameTeamsEligible() {
        Game g = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(g));
        stubWritableContext();
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setHomeTeamId(Long.valueOf(HOME_ID));
        dto.setGame(gamePart);

        gameService.saveResult(GAME_ID, dto);

        verify(eventRegistrationService).assertGameTeamsEligible(EVENT_ID, HOME_ID, AWAY_ID);
    }

    @Test
    @DisplayName("排赛门槛接线：importGameResult 建比赛同口径委托 assertGameTeamsEligible")
    void importGameResult_validatesGameTeamsEligible() {
        stubWritableContext();
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setHomeTeamId(Long.valueOf(HOME_ID));
        gamePart.setAwayTeamId(Long.valueOf(AWAY_ID));
        gamePart.setIsSpecialResult(Boolean.TRUE);
        dto.setGame(gamePart);
        dto.setStats(List.of());
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        gameService.importGameResult(EVENT_ID, dto);

        verify(eventRegistrationService).assertGameTeamsEligible(EVENT_ID, HOME_ID, AWAY_ID);
        verify(gameRepository).save(any(Game.class));
    }

    // ------------------------------------------------------------------ P/DH 强校验（#478）

    @Test
    @DisplayName("P/DH 强校验：导入进行中比赛时 DH 与打击投手同队返回 400")
    void importGameResult_live_rejectsDhAndBattingPitcher() {
        SaveGameResultDTO dto = pitcherDhImportPayload();
        dto.getGame().setStatus("live");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.importGameResult(Long.valueOf(EVENT_ID), dto));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("DH 与打击投手"));
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("P/DH 强校验：导入缺省 final（历史补录）不拦截并落库 10 行")
    void importGameResult_defaultFinal_skipsPitcherDh() {
        stubWritableContext();
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        gameService.importGameResult(Long.valueOf(EVENT_ID), pitcherDhImportPayload());

        verify(gameRepository).save(any(Game.class));
        verify(gamePlayerStatRepository, times(10)).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("P/DH 强校验：特殊赛果（弃权等）不拦截")
    void importGameResult_specialResult_skipsPitcherDh() {
        stubWritableContext();
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        SaveGameResultDTO dto = pitcherDhImportPayload();
        dto.getGame().setIsSpecialResult(Boolean.TRUE);
        dto.getGame().setStatus("live");

        gameService.importGameResult(Long.valueOf(EVENT_ID), dto);

        verify(gamePlayerStatRepository, times(10)).save(any(GamePlayerStat.class));
    }

    /** 主队 8 野手 + 第 9 棒投手 + 第 10 棒 DH（P 与 DH 分属不同球员，属冲突阵容） */
    private SaveGameResultDTO pitcherDhImportPayload() {
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setHomeTeamId(Long.valueOf(HOME_ID));
        gamePart.setAwayTeamId(Long.valueOf(AWAY_ID));
        gamePart.setIsSpecialResult(Boolean.FALSE);
        dto.setGame(gamePart);
        List<SaveGameResultDTO.StatPart> rows = new ArrayList<>();
        String[] positions = {"C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "P", "DH"};
        for (int i = 0; i < positions.length; i++) {
            SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
            part.setTeamId(Long.valueOf(HOME_ID));
            part.setPlayerId(Long.valueOf(3000L + i));
            part.setIsPitcher(Integer.valueOf(0));
            part.setBattingOrder(Integer.valueOf(i + 1));
            part.setPosition(positions[i]);
            rows.add(part);
        }
        dto.setStats(rows);
        return dto;
    }
}
