package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.TournamentViewDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.TeamRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentService：赛事积分与对阵视图装配")
class TournamentServiceTest {

    @Mock
    private EventService eventService;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private EventTeamRepository eventTeamRepository;
    @Mock
    private TeamRepository teamRepository;

    private TournamentService service;

    @BeforeEach
    void setUp() {
        service = new TournamentService(
                eventService, gameRepository, eventTeamRepository, teamRepository,
                new TournamentStandingsCalculator());
    }

    @Test
    @DisplayName("赛事不存在：返回 null 且不查询比赛/球队")
    void getView_notFound_returnsNull() {
        when(eventService.get(1L)).thenReturn(null);

        assertNull(service.getView(1L));
        verifyNoInteractions(gameRepository, eventTeamRepository, teamRepository);
    }

    @Test
    @DisplayName("单循环赛事：装配积分榜、积分规则与球队名")
    void getView_singleRoundRobin_returnsStandings() {
        Event event = new Event();
        event.setId(1L);
        event.setName("春季联赛");
        event.setSeason("2026");
        event.setFormatType(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN);
        event.setPointsWin(2);
        event.setPointsDraw(1);
        event.setPointsLoss(0);
        Game game = new Game();
        game.setId(10L);
        game.setEventId(1L);
        game.setHomeTeamId(1L);
        game.setAwayTeamId(2L);
        game.setHomeScore(5);
        game.setAwayScore(3);
        game.setStatus("final");
        Team home = new Team();
        home.setId(1L);
        home.setName("甲队");
        Team away = new Team();
        away.setId(2L);
        away.setName("乙队");

        when(eventService.get(1L)).thenReturn(event);
        when(gameRepository.findByEventIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(game));
        when(eventTeamRepository.findByEventIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of());
        when(teamRepository.findByIdInIncludingDissolved(anyCollection())).thenReturn(List.of(home, away));

        TournamentViewDTO view = service.getView(1L);

        assertEquals("春季联赛", view.eventName());
        assertEquals(TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN, view.formatType());
        assertEquals(2, view.pointsRule().win());
        assertEquals(1, view.standings().size());
        assertEquals(2, view.standings().get(0).rows().size());
        assertEquals(2, view.standings().get(0).rows().get(0).points());
        assertEquals("甲队", view.standings().get(0).rows().get(0).teamName());
        assertEquals(1, view.standings().get(0).matches().size());
        assertNull(view.bracket());
        assertTrue(view.warnings().isEmpty());
        verify(teamRepository).findByIdInIncludingDissolved(anyCollection());
    }
}
