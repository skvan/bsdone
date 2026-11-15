package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSnapshotRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 比赛快照读写（2026-11-15 生产 OOM 修复，Issue #686）：
 * 快照列不再随 Game 实体加载，读写改走同表轻量实体 GameSnapshot 的仓储。
 */
@ExtendWith(MockitoExtension.class)
class GameSnapshotServiceTest {
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
    @Mock
    private GameSnapshotRepository gameSnapshotRepository;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository,
                eventRepository, stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService,
                gameFieldingPosStatRepository, mock(GameFieldingAdjustmentService.class),
                mock(GameRevisionAuditService.class), gameSnapshotRepository);
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

    private void stubVisibleGame() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event()));
    }

    @Test
    @DisplayName("读取快照：从轻量快照仓储取 JSON，不经过 Game 实体")
    void getLiveSnapshot_readsFromSnapshotRepository() {
        stubVisibleGame();
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(null);
        String json = "{\"v\":1,\"eventLog\":[]}";
        when(gameSnapshotRepository.findSnapshotJsonById(GAME_ID)).thenReturn(Optional.of(json));

        assertEquals(json, gameService.getLiveSnapshot(GAME_ID));
    }

    @Test
    @DisplayName("读取快照：比赛不存在时返回 null 且不查快照表")
    void getLiveSnapshot_unknownGame_returnsNull() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertNull(gameService.getLiveSnapshot(GAME_ID));
        verifyNoInteractions(gameSnapshotRepository);
    }

    @Test
    @DisplayName("读取快照：已有比赛但无快照行时返回 null")
    void getLiveSnapshot_noSnapshot_returnsNull() {
        stubVisibleGame();
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(null);
        when(gameSnapshotRepository.findSnapshotJsonById(GAME_ID)).thenReturn(Optional.empty());

        assertNull(gameService.getLiveSnapshot(GAME_ID));
    }

    @Test
    @DisplayName("保存快照：只更新快照列，不回写 Game 实体")
    void saveLiveSnapshot_updatesSnapshotColumnOnly() {
        stubVisibleGame();
        String json = "{\"v\":1}";
        when(gameSnapshotRepository.updateSnapshotJson(eq(GAME_ID), eq(json), any())).thenReturn(1);

        gameService.saveLiveSnapshot(GAME_ID, json);

        verify(gameSnapshotRepository).updateSnapshotJson(eq(GAME_ID), eq(json), any());
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("保存快照：空白内容统一存 NULL（保持无快照语义）")
    void saveLiveSnapshot_blankJson_storesNull() {
        stubVisibleGame();
        when(gameSnapshotRepository.updateSnapshotJson(eq(GAME_ID), isNull(), any())).thenReturn(1);

        gameService.saveLiveSnapshot(GAME_ID, "   ");

        verify(gameSnapshotRepository).updateSnapshotJson(eq(GAME_ID), isNull(), any());
    }

    @Test
    @DisplayName("保存快照：比赛不存在时直接返回，不写快照表")
    void saveLiveSnapshot_unknownGame_skipsWrite() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        gameService.saveLiveSnapshot(GAME_ID, "{\"v\":1}");

        verifyNoInteractions(gameSnapshotRepository);
    }

    @Test
    @DisplayName("阵容保存：报名校验通过后只更新快照列")
    void saveLineupSnapshot_validatesThenUpdatesSnapshotColumn() {
        stubVisibleGame();
        String json = "{\"v\":1,\"homeLineup\":[{\"id\":5}]}";
        when(gameSnapshotRepository.updateSnapshotJson(eq(GAME_ID), eq(json), any())).thenReturn(1);

        gameService.saveLineupSnapshot(GAME_ID, json);

        verify(eventRegistrationService).assertLineupSnapshotInRoster(EVENT_ID, HOME_ID, AWAY_ID, json);
        verify(gameSnapshotRepository).updateSnapshotJson(eq(GAME_ID), eq(json), any());
        verify(gameRepository, never()).save(any(Game.class));
    }
}
