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

    @Test
    @DisplayName("saveLive：纸质表单新增字段全部落库")
    void saveLive_paperFormFields_written() {
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.FALSE);

        GameSaveLiveDTO.GamePlayerStatPart part = new GameSaveLiveDTO.GamePlayerStatPart();
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(Long.valueOf(PLAYER_ID));
        part.setIsPitcher(Integer.valueOf(1));
        part.setGdp(Integer.valueOf(1));
        part.setIbb(Integer.valueOf(2));
        part.setIo(Integer.valueOf(1));
        part.setPitchDoubles(Integer.valueOf(2));
        part.setPitchTriples(Integer.valueOf(1));
        part.setPitchSh(Integer.valueOf(1));
        part.setPitchSf(Integer.valueOf(1));
        part.setPitchBb(Integer.valueOf(3));
        part.setPitchIbb(Integer.valueOf(2));
        part.setPitchHbp(Integer.valueOf(1));
        part.setPitchIo(Integer.valueOf(1));
        part.setGo(Integer.valueOf(4));
        part.setFo(Integer.valueOf(5));
        part.setGs(Integer.valueOf(1));
        part.setFieldingGs(Integer.valueOf(1));
        part.setDefInn(Double.valueOf(6.1D));
        part.setDp(Integer.valueOf(1));
        part.setPb(Integer.valueOf(1));
        part.setCatcherSb(Integer.valueOf(2));
        part.setCatcherCs(Integer.valueOf(1));
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setStats(List.of(part));

        gameService.saveLive(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(1), pitcher.getGdp());
        assertEquals(Integer.valueOf(2), pitcher.getIbb());
        assertEquals(Integer.valueOf(1), pitcher.getIo());
        assertEquals(Integer.valueOf(2), pitcher.getPitchDoubles());
        assertEquals(Integer.valueOf(1), pitcher.getPitchTriples());
        assertEquals(Integer.valueOf(1), pitcher.getPitchSh());
        assertEquals(Integer.valueOf(1), pitcher.getPitchSf());
        assertEquals(Integer.valueOf(3), pitcher.getPitchBb());
        assertEquals(Integer.valueOf(2), pitcher.getPitchIbb());
        assertEquals(Integer.valueOf(1), pitcher.getPitchHbp());
        assertEquals(Integer.valueOf(1), pitcher.getPitchIo());
        assertEquals(Integer.valueOf(4), pitcher.getGo());
        assertEquals(Integer.valueOf(5), pitcher.getFo());
        assertEquals(Integer.valueOf(1), pitcher.getGs());
        assertEquals(Integer.valueOf(1), pitcher.getFieldingGs());
        assertEquals(Double.valueOf(6.1D), pitcher.getDefInn());
        assertEquals(Integer.valueOf(1), pitcher.getDp());
        assertEquals(Integer.valueOf(1), pitcher.getPb());
        assertEquals(Integer.valueOf(2), pitcher.getCatcherSb());
        assertEquals(Integer.valueOf(1), pitcher.getCatcherCs());
    }

    @Test
    @DisplayName("saveLive：载荷缺投手四死栏时保留既有值，不写成 NULL（#387）")
    void saveLive_missingPitchingFourDeath_preservesExisting() {
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.FALSE);
        pitcher.setPitchBbHp(Integer.valueOf(3));
        pitcher.setPitchBb(Integer.valueOf(2));
        pitcher.setPitchIbb(Integer.valueOf(0));
        pitcher.setPitchHbp(Integer.valueOf(1));

        GameSaveLiveDTO.GamePlayerStatPart part = new GameSaveLiveDTO.GamePlayerStatPart();
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(Long.valueOf(PLAYER_ID));
        part.setIsPitcher(Integer.valueOf(1));
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setStats(List.of(part));

        gameService.saveLive(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(3), pitcher.getPitchBbHp(), "四死合计缺栏不得被抹成 NULL");
        assertEquals(Integer.valueOf(2), pitcher.getPitchBb());
        assertEquals(Integer.valueOf(0), pitcher.getPitchIbb());
        assertEquals(Integer.valueOf(1), pitcher.getPitchHbp(), "触身球缺栏不得被抹成 NULL");
    }

    @Test
    @DisplayName("saveLive：显式送 0 仍能清零（缺栏保留不等于禁止归零，撤销语义不变）")
    void saveLive_explicitZero_stillClears() {
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.FALSE);
        pitcher.setPitchBbHp(Integer.valueOf(3));
        pitcher.setPitchBb(Integer.valueOf(2));
        pitcher.setPitchHbp(Integer.valueOf(1));

        GameSaveLiveDTO.GamePlayerStatPart part = new GameSaveLiveDTO.GamePlayerStatPart();
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(Long.valueOf(PLAYER_ID));
        part.setIsPitcher(Integer.valueOf(1));
        part.setPitchBbHp(Integer.valueOf(0));
        part.setPitchBb(Integer.valueOf(0));
        part.setPitchHbp(Integer.valueOf(0));
        GameSaveLiveDTO dto = new GameSaveLiveDTO();
        dto.setStats(List.of(part));

        gameService.saveLive(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(0), pitcher.getPitchBbHp());
        assertEquals(Integer.valueOf(0), pitcher.getPitchBb());
        assertEquals(Integer.valueOf(0), pitcher.getPitchHbp());
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
