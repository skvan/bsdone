package com.bsball.service;

import com.bsball.model.dto.TournamentViewDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.TeamRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 赛事积分榜与淘汰赛对阵视图服务（Issue #665）。
 * <p>只读聚合：赛事可见性沿用 {@link EventService#get(Long)}；积分与对阵计算委托
 * {@link TournamentStandingsCalculator}，便于纯单元测试。
 */
@Service
@RequiredArgsConstructor
public class TournamentService {
    private final EventService eventService;
    private final GameRepository gameRepository;
    private final EventTeamRepository eventTeamRepository;
    private final TeamRepository teamRepository;
    private final TournamentStandingsCalculator calculator;

    /**
     * 获取赛事积分榜与对阵视图。
     *
     * @param eventId 赛事 ID
     * @return 视图；赛事不存在或无权查看时返回 null
     */
    public TournamentViewDTO getView(Long eventId) {
        Event event = this.eventService.get(eventId);
        if (event == null) {
            return null;
        }
        List<Game> games = this.gameRepository
                .findByEventIdAndDeletedAtIsNullOrderByIdAsc(eventId);
        List<EventTeam> registrations = this.eventTeamRepository
                .findByEventIdAndDeletedAtIsNullOrderByIdAsc(eventId);
        Map<Long, Team> teams = this.loadTeams(games, registrations);
        List<String> warnings = new ArrayList<>();
        List<TournamentViewDTO.StandingGroup> standings =
                this.calculator.calculateStandings(event, games, registrations, teams, warnings);
        TournamentViewDTO.Bracket bracket =
                this.calculator.calculateBracket(event, games, teams, warnings);
        return new TournamentViewDTO(
                event.getId(),
                event.getName(),
                event.getSeason(),
                this.formatType(event),
                this.calculator.pointsRule(event),
                standings,
                bracket,
                warnings);
    }

    private Map<Long, Team> loadTeams(List<Game> games, List<EventTeam> registrations) {
        Set<Long> teamIds = new LinkedHashSet<>();
        for (EventTeam registration : registrations) {
            if (registration.getTeamId() != null) {
                teamIds.add(registration.getTeamId());
            }
        }
        for (Game game : games) {
            if (game.getHomeTeamId() != null) {
                teamIds.add(game.getHomeTeamId());
            }
            if (game.getAwayTeamId() != null) {
                teamIds.add(game.getAwayTeamId());
            }
        }
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return this.teamRepository.findByIdInIncludingDissolved(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, team -> team, (a, b) -> a));
    }

    private String formatType(Event event) {
        String value = event.getFormatType();
        if (value == null || value.isBlank()) {
            return TournamentStandingsCalculator.FORMAT_SINGLE_ROUND_ROBIN;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
