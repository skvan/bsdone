package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService：比赛赛制阶段字段（Issue #665）")
class GameServiceTournamentFieldsTest {

    private static final long TENANT_ID = 1L;
    private static final long EVENT_ID = 7L;
    private static final long GAME_ID = 500L;

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
    private Event event;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository,
                eventRepository, stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService,
                gameFieldingPosStatRepository,
                mock(GameFieldingAdjustmentService.class),
                mock(GameRevisionAuditService.class));
        event = new Event();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    }

    @Test
    @DisplayName("小组+淘汰赛：小组赛阶段与组别保留")
    void create_groupStage_keepsStageAndGroup() {
        event.setFormatType("GROUP_KNOCKOUT");
        Game game = baseGame();
        game.setStageCode("group");
        game.setGroupCode("a");
        game.setRoundNo(1);
        game.setBracketSlot(2);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game saved = gameService.create(game);

        assertEquals("GROUP", saved.getStageCode());
        assertEquals("A", saved.getGroupCode());
        assertEquals(1, saved.getRoundNo());
        assertEquals(2, saved.getBracketSlot());
    }

    @Test
    @DisplayName("单循环：阶段与组别被清空，回合字段保留")
    void create_singleRoundRobin_clearsStageAndGroup() {
        event.setFormatType("SINGLE_ROUND_ROBIN");
        Game game = baseGame();
        game.setStageCode("GROUP");
        game.setGroupCode("A");
        game.setRoundNo(2);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game saved = gameService.create(game);

        assertNull(saved.getStageCode());
        assertNull(saved.getGroupCode());
        assertEquals(2, saved.getRoundNo());
    }

    @Test
    @DisplayName("保存比赛结果：阶段/组别/轮次/对阵位落库")
    void saveResult_persistsStageFields() {
        event.setFormatType("GROUP_KNOCKOUT");
        Game existing = baseGame();
        existing.setId(GAME_ID);
        existing.setTenantId(TENANT_ID);
        existing.setStatus("final");
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart part = new SaveGameResultDTO.GamePart();
        part.setStageCode("group");
        part.setGroupCode("a");
        part.setRoundNo(2);
        part.setBracketSlot(3);
        dto.setGame(part);

        gameService.saveResult(GAME_ID, dto);

        assertEquals("GROUP", existing.getStageCode());
        assertEquals("A", existing.getGroupCode());
        assertEquals(2, existing.getRoundNo());
        assertEquals(3, existing.getBracketSlot());
    }

    @Test
    @DisplayName("单败淘汰：填写小组赛阶段返回 400")
    void create_knockoutWithGroupStage_throws400() {
        event.setFormatType("KNOCKOUT");
        Game game = baseGame();
        game.setStageCode("GROUP");

        BusinessException error = assertThrows(BusinessException.class, () -> gameService.create(game));

        assertEquals(400, error.getCode());
    }

    private Game baseGame() {
        Game game = new Game();
        game.setEventId(EVENT_ID);
        game.setHomeTeamId(1L);
        game.setAwayTeamId(2L);
        return game;
    }
}
