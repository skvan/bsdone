package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameSubstitutionEvent;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
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
@DisplayName("GameService.saveLive：换人事务事件")
class GameServiceSaveLiveSubstitutionTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 237L;
    private static final long EVENT_ID = 17L;
    private static final long HOME_TEAM_ID = 5L;
    private static final long AWAY_TEAM_ID = 1L;
    private static final long OUT_PLAYER_ID = 100L;
    private static final long IN_PLAYER_ID = 122L;

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

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository,
                eventRepository, stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService, gameFieldingPosStatRepository);
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));

        Game game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        game.setHomeTeamId(Long.valueOf(HOME_TEAM_ID));
        game.setAwayTeamId(Long.valueOf(AWAY_TEAM_ID));
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));

        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));
        lenient().when(gameSubstitutionEventRepository.findMaxSequence(Long.valueOf(GAME_ID))).thenReturn(0);
        lenient().when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("合法换人事务：字段与顺序写入事件表")
    void saveLive_validSubstitution_persistsEvent() {
        GameSaveLiveDTO dto = dtoWith(substitution("tx-1", HOME_TEAM_ID, "SUBSTITUTION", 8));

        gameService.saveLive(Long.valueOf(GAME_ID), dto);

        ArgumentCaptor<List<GameSubstitutionEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameSubstitutionEventRepository).saveAll(captor.capture());
        List<GameSubstitutionEvent> events = captor.getValue();
        assertEquals(1, events.size());
        GameSubstitutionEvent event = events.get(0);
        assertEquals("tx-1", event.getTransactionId());
        assertEquals(Long.valueOf(GAME_ID), event.getGameId());
        assertEquals(Long.valueOf(HOME_TEAM_ID), event.getTeamId());
        assertEquals(Integer.valueOf(3), event.getInning());
        assertEquals("bottom", event.getTopBottom());
        assertEquals(Integer.valueOf(1), event.getSequence());
        assertEquals(Integer.valueOf(1), event.getChangeSeq());
        assertEquals("SUBSTITUTION", event.getChangeType());
        assertEquals(Integer.valueOf(8), event.getSlot());
        assertEquals(Long.valueOf(OUT_PLAYER_ID), event.getOutPlayerId());
        assertEquals(Long.valueOf(IN_PLAYER_ID), event.getInPlayerId());
        assertEquals("C", event.getOutPosition());
        assertEquals("LF", event.getInPosition());
    }

    @Test
    @DisplayName("同一 transactionId 重试：幂等跳过，不重复写事件")
    void saveLive_duplicateTransaction_isIdempotent() {
        when(gameSubstitutionEventRepository.existsByGameIdAndTransactionId(Long.valueOf(GAME_ID), "tx-1"))
                .thenReturn(Boolean.TRUE);

        gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(substitution("tx-1", HOME_TEAM_ID, "SUBSTITUTION", 8)));

        verify(gameSubstitutionEventRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("换人球队不属于本场：400")
    void saveLive_foreignTeam_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLive(Long.valueOf(GAME_ID),
                        dtoWith(substitution("tx-1", 999L, "SUBSTITUTION", 8))));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("缺少 transactionId：400")
    void saveLive_missingTransaction_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLive(Long.valueOf(GAME_ID),
                        dtoWith(substitution(null, HOME_TEAM_ID, "SUBSTITUTION", 8))));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("事务内 transactionId 不一致：400")
    void saveLive_mixedTransaction_rejected() {
        GameSaveLiveDTO.SubstitutionPart second = substitution("tx-2", HOME_TEAM_ID, "SUBSTITUTION", 9);
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setSubstitutions(List.of(substitution("tx-1", HOME_TEAM_ID, "SUBSTITUTION", 8), second));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLive(Long.valueOf(GAME_ID), dto));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("SUBSTITUTION 的 out/in 相同：400")
    void saveLive_sameOutIn_rejected() {
        GameSaveLiveDTO.SubstitutionPart part = substitution("tx-1", HOME_TEAM_ID, "SUBSTITUTION", 8);
        part.setInPlayerId(part.getOutPlayerId());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLive(Long.valueOf(GAME_ID), dtoWith(part)));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("未知 changeType：400")
    void saveLive_unknownType_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLive(Long.valueOf(GAME_ID),
                        dtoWith(substitution("tx-1", HOME_TEAM_ID, "BOGUS", 8))));
        assertEquals(400, ex.getCode());
    }

    private GameSaveLiveDTO dtoWith(GameSaveLiveDTO.SubstitutionPart part) {
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setSubstitutions(List.of(part));
        return dto;
    }

    private GameSaveLiveDTO.SubstitutionPart substitution(String transactionId, Long teamId, String type, int slot) {
        GameSaveLiveDTO.SubstitutionPart part = new GameSaveLiveDTO.SubstitutionPart();
        part.setTransactionId(transactionId);
        part.setTeamId(Long.valueOf(teamId));
        part.setInning(Integer.valueOf(3));
        part.setTopBottom("bottom");
        part.setChangeType(type);
        part.setSlot(Integer.valueOf(slot));
        part.setOutPlayerId(Long.valueOf(OUT_PLAYER_ID));
        part.setInPlayerId(Long.valueOf(IN_PLAYER_ID));
        part.setOutPosition("C");
        part.setInPosition("LF");
        return part;
    }
}
