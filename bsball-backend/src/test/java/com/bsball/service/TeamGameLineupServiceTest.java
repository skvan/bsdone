package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.GameLineupSubmissionDto;
import com.bsball.model.dto.LineupSubmissionSaveRequest;
import com.bsball.model.dto.TeamLineupContextDto;
import com.bsball.model.dto.TeamLineupGameListDto;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameLineupSubmission;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventRepository;
import com.bsball.repository.EventRosterPlayerRepository;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GameLineupSubmissionRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 赛前阵容提交服务测试：球队域守卫、比赛未开始、参赛方、名单校验、upsert 与办赛方读取。
 * 纯单元测试（Mockito，不启动 Spring）。
 */
@ExtendWith(MockitoExtension.class)
class TeamGameLineupServiceTest {
    private static final long TENANT_ID = 1L;
    private static final long EVENT_ID = 700L;
    private static final long GAME_ID = 900L;
    private static final long TEAM_ID = 100L;
    private static final long OPPONENT_ID = 200L;

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventTeamRepository eventTeamRepository;
    @Mock
    private EventRosterPlayerRepository eventRosterPlayerRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameLineupSubmissionRepository submissionRepository;
    @Mock
    private ResourceGuard resourceGuard;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock
    private EventSpecialRuleService eventSpecialRuleService;
    @Mock
    private LeagueSpecialRuleService leagueSpecialRuleService;
    @Mock
    private EventRegistrationService eventRegistrationService;

    private TeamGameLineupService service;

    @BeforeEach
    void setUp() {
        service = new TeamGameLineupService(teamRepository, eventRepository, eventTeamRepository,
                eventRosterPlayerRepository, gameRepository, submissionRepository, resourceGuard,
                tenantQueryPolicyService, eventSpecialRuleService, leagueSpecialRuleService,
                eventRegistrationService);
    }

    private static Team team(long id, String name) {
        Team team = new Team();
        team.setId(Long.valueOf(id));
        team.setTenantId(Long.valueOf(TENANT_ID));
        team.setName(name);
        team.setLogo("logo-" + id + ".png");
        return team;
    }

    private static Event event() {
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));
        event.setName("春季联赛");
        event.setSeason("2026");
        event.setGameMode("BASEBALL");
        return event;
    }

    private static EventTeam registration(String status) {
        EventTeam reg = new EventTeam();
        reg.setTenantId(Long.valueOf(TENANT_ID));
        reg.setEventId(Long.valueOf(EVENT_ID));
        reg.setTeamId(Long.valueOf(TEAM_ID));
        reg.setStatus(status);
        return reg;
    }

    private static Game game(String status) {
        Game game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        game.setHomeTeamId(Long.valueOf(TEAM_ID));
        game.setAwayTeamId(Long.valueOf(OPPONENT_ID));
        game.setStatus(status);
        game.setGameday("2026-11-15");
        game.setGameTime("2026-11-15 14:00:00");
        game.setGameNumber(Integer.valueOf(3));
        game.setVenue("天母棒球场");
        return game;
    }

    private void stubTeamAndEvent() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, "雷霆队")));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event()));
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(registration(EventTeam.STATUS_CONFIRMED)));
    }

    private void stubGame(Game game) {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    private void stubTenant() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    @DisplayName("球队列表：返回本队未开始比赛与已保存阵容状态")
    void listGames_returnsUpcomingGamesWithSubmission() {
        stubTenant();
        stubTeamAndEvent();
        Game game = game("scheduled");
        when(gameRepository.findUpcomingForTeamInEvent(TENANT_ID, EVENT_ID, TEAM_ID)).thenReturn(List.of(game));
        GameLineupSubmission submission = new GameLineupSubmission();
        submission.setGameId(Long.valueOf(GAME_ID));
        submission.setComplete(Boolean.TRUE);
        submission.setUpdatedAt(LocalDateTime.of(2026, 11, 11, 18, 20));
        when(submissionRepository.findByGameIdInAndTeamIdAndDeletedAtIsNull(any(), eq(TEAM_ID)))
                .thenReturn(List.of(submission));
        when(teamRepository.findAllById(any())).thenReturn(List.of(team(OPPONENT_ID, "猛虎队")));

        TeamLineupGameListDto result = service.listGames(TEAM_ID, EVENT_ID);

        assertEquals(EVENT_ID, result.eventId());
        assertEquals("春季联赛", result.eventName());
        assertEquals(1, result.games().size());
        assertEquals("home", result.games().get(0).side());
        assertEquals("猛虎队", result.games().get(0).opponentName());
        assertTrue(result.games().get(0).hasSubmission());
        assertTrue(result.games().get(0).complete());
        verify(resourceGuard).assertCanManageTeam(TEAM_ID);
    }

    @Test
    @DisplayName("上下文：比赛已开始（live）→ 400，不读取赛事/名单")
    void context_liveGame_rejected() {
        stubTenant();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, "雷霆队")));
        stubGame(game("live"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getContext(TEAM_ID, GAME_ID));

        assertEquals(400, ex.getCode());
        verify(eventRepository, never()).findById(any());
        verify(eventRosterPlayerRepository, never()).countByEventIdAndTeamIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    @DisplayName("保存：守备投手/DH 池引用打线行不算重复，校验名单后 upsert")
    void save_validatesRosterAndUpserts() {
        stubTenant();
        stubTeamAndEvent();
        stubGame(game("scheduled"));
        when(submissionRepository.findForUpdate(GAME_ID, TEAM_ID)).thenReturn(Optional.empty());
        String json = "{\"v\":1,\"gameId\":900,\"teamId\":100,"
                + "\"lineup\":[{\"id\":5,\"position\":\"P\"},{\"id\":8,\"position\":\"DH\"}],"
                + "\"bench\":[{\"id\":6}],\"fieldingPitcherId\":5,\"lastDhAddedFromPoolId\":8}";

        service.save(TEAM_ID, GAME_ID, new LineupSubmissionSaveRequest(json, Boolean.TRUE));

        verify(eventRegistrationService).assertTeamLineupInRoster(eq(EVENT_ID), eq(TEAM_ID), any());
        ArgumentCaptor<GameLineupSubmission> captor = ArgumentCaptor.forClass(GameLineupSubmission.class);
        verify(submissionRepository).saveAndFlush(captor.capture());
        GameLineupSubmission saved = captor.getValue();
        assertEquals(Long.valueOf(TENANT_ID), saved.getTenantId());
        assertEquals(Long.valueOf(EVENT_ID), saved.getEventId());
        assertEquals(Long.valueOf(GAME_ID), saved.getGameId());
        assertEquals(Long.valueOf(TEAM_ID), saved.getTeamId());
        assertEquals(json, saved.getLineupJson());
        assertTrue(saved.getComplete());
    }

    @Test
    @DisplayName("保存：首次并发唯一键冲突 → 409，不抛 500")
    void save_firstSaveConflict_returns409() {
        stubTenant();
        stubTeamAndEvent();
        stubGame(game("scheduled"));
        when(submissionRepository.findForUpdate(GAME_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(submissionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        String json = "{\"v\":1,\"gameId\":900,\"teamId\":100,\"lineup\":[{\"id\":5}]}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.save(TEAM_ID, GAME_ID, new LineupSubmissionSaveRequest(json, Boolean.FALSE)));

        assertEquals(409, ex.getCode());
    }

    @Test
    @DisplayName("保存：阵容重复球员 → 400，不触发名单校验/落库")
    void save_duplicatePlayer_rejected() {
        stubTenant();
        stubTeamAndEvent();
        stubGame(game("scheduled"));
        String json = "{\"v\":1,\"gameId\":900,\"teamId\":100,"
                + "\"lineup\":[{\"id\":5},{\"id\":5}]}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.save(TEAM_ID, GAME_ID, new LineupSubmissionSaveRequest(json, Boolean.FALSE)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("重复"));
        verify(eventRegistrationService, never()).assertTeamLineupInRoster(any(), any(), any());
        verify(submissionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("保存：比赛不是本队参加 → 403")
    void save_nonParticipant_rejected() {
        stubTenant();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, "雷霆队")));
        Game game = game("scheduled");
        game.setHomeTeamId(Long.valueOf(10L));
        game.setAwayTeamId(Long.valueOf(20L));
        stubGame(game);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.save(TEAM_ID, GAME_ID, new LineupSubmissionSaveRequest("{}", Boolean.FALSE)));

        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("办赛方读取：只返回仍在比赛双方中的提交，并带 side")
    void listSubmissionsForOrganizer_filtersParticipants() {
        stubTenant();
        Game game = game("scheduled");
        stubGame(game);
        GameLineupSubmission home = new GameLineupSubmission();
        home.setGameId(Long.valueOf(GAME_ID));
        home.setTeamId(Long.valueOf(TEAM_ID));
        home.setLineupJson("{\"v\":1}");
        GameLineupSubmission removed = new GameLineupSubmission();
        removed.setGameId(Long.valueOf(GAME_ID));
        removed.setTeamId(Long.valueOf(999L));
        removed.setLineupJson("{\"v\":1}");
        when(submissionRepository.findByGameIdAndDeletedAtIsNullOrderByIdAsc(GAME_ID))
                .thenReturn(List.of(home, removed));
        when(teamRepository.findAllById(any())).thenReturn(List.of(
                team(TEAM_ID, "雷霆队"), team(999L, "已换下队")));

        List<GameLineupSubmissionDto> result = service.listSubmissionsForOrganizer(GAME_ID);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(TEAM_ID), result.get(0).teamId());
        assertEquals("home", result.get(0).side());
        verify(resourceGuard).assertCanManageGame(GAME_ID);
    }
}
