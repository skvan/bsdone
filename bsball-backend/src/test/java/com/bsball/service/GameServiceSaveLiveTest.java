package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService.saveLive：ER 引擎场次不接收客户端 er")
class GameServiceSaveLiveTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 245L;
    private static final long EVENT_ID = 17L;
    private static final long TEAM_ID = 57L;
    private static final long PLAYER_ID = 1324L;
    private static final long ROW_ID = 5184L;

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
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
    private GamePlayerStat pitcher;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, eventRepository,
                stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService);
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));

        Game game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));

        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));

        pitcher = new GamePlayerStat();
        pitcher.setId(Long.valueOf(ROW_ID));
        pitcher.setGameId(Long.valueOf(GAME_ID));
        pitcher.setTeamId(Long.valueOf(TEAM_ID));
        pitcher.setPlayerId(Long.valueOf(PLAYER_ID));
        pitcher.setIsPitcher(Integer.valueOf(1));
        pitcher.setEr(Integer.valueOf(3));
        pitcher.setPitchR(Integer.valueOf(5));
        lenient().when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID)))
                .thenReturn(List.of(pitcher));
        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("引擎场次 saveLive：客户端 er 不覆盖引擎累计")
    void saveLive_engineManaged_ignoresClientEr() {
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.TRUE);

        gameService.saveLive(Long.valueOf(GAME_ID), dtoWithEr(99));

        assertEquals(Integer.valueOf(3), pitcher.getEr());
    }

    @Test
    @DisplayName("无引擎数据的 legacy 场次：saveLive 仍可写入客户端 er")
    void saveLive_legacy_writesClientEr() {
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.FALSE);

        gameService.saveLive(Long.valueOf(GAME_ID), dtoWithEr(Integer.valueOf(4)));

        assertEquals(Integer.valueOf(4), pitcher.getEr());
    }

    @Test
    @DisplayName("legacy 场次未携带 er 时保留既有值，避免被 null 清空")
    void saveLive_legacyMissingEr_preservesExisting() {
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.FALSE);

        gameService.saveLive(Long.valueOf(GAME_ID), dtoWithEr(null));

        assertEquals(Integer.valueOf(3), pitcher.getEr());
    }

    private GameSaveLiveDTO dtoWithEr(Integer er) {
        GameSaveLiveDTO.GamePlayerStatPart part = new GameSaveLiveDTO.GamePlayerStatPart();
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(Long.valueOf(PLAYER_ID));
        part.setIsPitcher(Integer.valueOf(1));
        part.setEr(er);
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setStats(List.of(part));
        return dto;
    }
}
