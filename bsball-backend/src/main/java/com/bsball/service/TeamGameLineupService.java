package com.bsball.service;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.GameLineupSubmissionDto;
import com.bsball.model.dto.LineupSubmissionSaveRequest;
import com.bsball.model.dto.TeamGameLineupItemDto;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 赛前阵容提交（电子打序单）服务。
 * <p>球队侧（列表/上下文/保存）走 {@link ResourceGuard#assertCanManageTeam(Long)}；
 * 办赛方读取走 {@link ResourceGuard#assertCanManageGame(Long)}。只有比赛未开始
 * （scheduled/postponed）且球队已确认报名时才能保存；球员必须属于该赛事报名名单。</p>
 */
@Service
public class TeamGameLineupService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_BENCH = 60;

    private final TeamRepository teamRepository;
    private final EventRepository eventRepository;
    private final EventTeamRepository eventTeamRepository;
    private final EventRosterPlayerRepository eventRosterPlayerRepository;
    private final GameRepository gameRepository;
    private final GameLineupSubmissionRepository submissionRepository;
    private final ResourceGuard resourceGuard;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final EventSpecialRuleService eventSpecialRuleService;
    private final LeagueSpecialRuleService leagueSpecialRuleService;
    private final EventRegistrationService eventRegistrationService;

    public TeamGameLineupService(TeamRepository teamRepository,
            EventRepository eventRepository,
            EventTeamRepository eventTeamRepository,
            EventRosterPlayerRepository eventRosterPlayerRepository,
            GameRepository gameRepository,
            GameLineupSubmissionRepository submissionRepository,
            ResourceGuard resourceGuard,
            TenantQueryPolicyService tenantQueryPolicyService,
            EventSpecialRuleService eventSpecialRuleService,
            LeagueSpecialRuleService leagueSpecialRuleService,
            EventRegistrationService eventRegistrationService) {
        this.teamRepository = teamRepository;
        this.eventRepository = eventRepository;
        this.eventTeamRepository = eventTeamRepository;
        this.eventRosterPlayerRepository = eventRosterPlayerRepository;
        this.gameRepository = gameRepository;
        this.submissionRepository = submissionRepository;
        this.resourceGuard = resourceGuard;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.eventSpecialRuleService = eventSpecialRuleService;
        this.leagueSpecialRuleService = leagueSpecialRuleService;
        this.eventRegistrationService = eventRegistrationService;
    }

    /** 球队端：该赛事下本队所有未开始比赛及阵容保存状态。 */
    public TeamLineupGameListDto listGames(Long teamId, Long eventId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageTeam(teamId);
        Team team = this.requireTeam(teamId, tid);
        Event event = this.requireEvent(eventId, tid);
        this.requireConfirmedRegistration(eventId, teamId, tid);
        List<Game> games = this.gameRepository.findUpcomingForTeamInEvent(Long.valueOf(tid), eventId, teamId);
        if (games.isEmpty()) {
            return new TeamLineupGameListDto(eventId, event.getName(), event.getSeason(), event.getGameMode(), List.of());
        }
        Set<Long> gameIds = games.stream().map(Game::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GameLineupSubmission> submissions = this.submissionRepository
                .findByGameIdInAndTeamIdAndDeletedAtIsNull(gameIds, teamId).stream()
                .collect(Collectors.toMap(GameLineupSubmission::getGameId, x -> x, (a, b) -> a));
        Set<Long> opponentIds = new LinkedHashSet<>();
        for (Game game : games) {
            Long opponentId = this.opponentId(game, teamId);
            if (opponentId != null) {
                opponentIds.add(opponentId);
            }
        }
        Map<Long, Team> opponents = opponentIds.isEmpty() ? Map.of() : this.teamRepository.findAllById(opponentIds).stream()
                .collect(Collectors.toMap(Team::getId, x -> x, (a, b) -> a));
        List<TeamGameLineupItemDto> items = new ArrayList<>(games.size());
        for (Game game : games) {
            Long opponentId = this.opponentId(game, teamId);
            Team opponent = opponentId == null ? null : opponents.get(opponentId);
            GameLineupSubmission submission = submissions.get(game.getId());
            items.add(new TeamGameLineupItemDto(
                    game.getId(),
                    game.getGameNumber(),
                    game.getGameday(),
                    game.getGameTime(),
                    game.getVenue(),
                    game.getStatus(),
                    this.sideOf(game, teamId),
                    opponentId,
                    opponent != null ? opponent.getName() : null,
                    Boolean.valueOf(submission != null),
                    submission != null ? submission.getUpdatedAt() : null,
                    submission != null ? Boolean.TRUE.equals(submission.getComplete()) : Boolean.FALSE));
        }
        return new TeamLineupGameListDto(eventId, event.getName(), event.getSeason(), event.getGameMode(), items);
    }

    /** 球队端：单场赛前阵容编辑上下文（只返回本队提交，不返回对手阵容）。 */
    public TeamLineupContextDto getContext(Long teamId, Long gameId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageTeam(teamId);
        Team team = this.requireTeam(teamId, tid);
        Game game = this.requireGame(gameId, tid);
        this.assertParticipant(game, teamId);
        this.assertEditable(game);
        Event event = this.requireEvent(game.getEventId(), tid);
        this.requireConfirmedRegistration(event.getId(), teamId, tid);
        Long opponentId = this.opponentId(game, teamId);
        Team opponent = opponentId == null ? null : this.teamRepository.findById(opponentId).orElse(null);
        GameLineupSubmission submission = this.submissionRepository
                .findByGameIdAndTeamIdAndDeletedAtIsNull(gameId, teamId).orElse(null);
        boolean restricted = this.eventRegistrationService.eventRestricted(event.getId());
        long rosterCount = restricted
                ? this.eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(event.getId(), teamId)
                : 0L;
        return new TeamLineupContextDto(
                game.getId(),
                event.getId(),
                event.getName(),
                event.getSeason(),
                event.getGameMode(),
                this.buildSpecialRules(event),
                teamId,
                team.getName(),
                team.getLogo(),
                this.sideOf(game, teamId),
                opponentId,
                opponent != null ? opponent.getName() : null,
                game.getGameNumber(),
                game.getGameday(),
                game.getGameTime(),
                game.getVenue(),
                game.getStatus(),
                Boolean.valueOf(restricted),
                rosterCount,
                submission != null ? submission.getLineupJson() : null,
                submission != null ? submission.getUpdatedAt() : null,
                submission != null ? Boolean.TRUE.equals(submission.getComplete()) : Boolean.FALSE);
    }

    /** 球队端：保存本队赛前阵容（草稿允许不完整；开赛后拒绝）。 */
    @Transactional(rollbackFor = Exception.class)
    public void save(Long teamId, Long gameId, LineupSubmissionSaveRequest request) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageTeam(teamId);
        this.requireTeam(teamId, tid);
        Game game = this.requireGame(gameId, tid);
        this.assertParticipant(game, teamId);
        this.assertEditable(game);
        Event event = this.requireEvent(game.getEventId(), tid);
        this.requireConfirmedRegistration(event.getId(), teamId, tid);
        if (request == null || request.lineupJson() == null || request.lineupJson().isBlank()) {
            throw new BusinessException(400, "阵容数据不能为空");
        }
        JsonNode root = this.parseAndValidate(request.lineupJson(), gameId, teamId, event.getGameMode());
        this.eventRegistrationService.assertTeamLineupInRoster(event.getId(), teamId, root);
        GameLineupSubmission submission = this.submissionRepository.findForUpdate(gameId, teamId).orElse(null);
        if (submission == null) {
            submission = new GameLineupSubmission();
            submission.setTenantId(Long.valueOf(tid));
            submission.setEventId(event.getId());
            submission.setGameId(gameId);
            submission.setTeamId(teamId);
        }
        submission.setLineupJson(request.lineupJson());
        submission.setComplete(Boolean.TRUE.equals(request.complete()));
        try {
            // saveAndFlush 使唯一键冲突在本事务内暴露；首次并发保存提示重试，不返回 500
            this.submissionRepository.saveAndFlush(submission);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(409, "阵容保存冲突，请重试");
        }
    }

    /** 办赛方：读取一场比赛双方已提交的赛前阵容（仅返回仍在比赛中的球队）。 */
    public List<GameLineupSubmissionDto> listSubmissionsForOrganizer(Long gameId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageGame(gameId);
        Game game = this.requireGame(gameId, tid);
        List<GameLineupSubmission> submissions = this.submissionRepository
                .findByGameIdAndDeletedAtIsNullOrderByIdAsc(gameId);
        if (submissions.isEmpty()) {
            return List.of();
        }
        Set<Long> participantIds = new LinkedHashSet<>();
        if (game.getHomeTeamId() != null) {
            participantIds.add(game.getHomeTeamId());
        }
        if (game.getAwayTeamId() != null) {
            participantIds.add(game.getAwayTeamId());
        }
        List<GameLineupSubmission> active = submissions.stream()
                .filter(s -> participantIds.contains(s.getTeamId()))
                .toList();
        if (active.isEmpty()) {
            return List.of();
        }
        Map<Long, Team> teams = this.teamRepository.findAllById(active.stream().map(GameLineupSubmission::getTeamId).toList())
                .stream().collect(Collectors.toMap(Team::getId, x -> x, (a, b) -> a));
        List<GameLineupSubmissionDto> out = new ArrayList<>(active.size());
        for (GameLineupSubmission submission : active) {
            Team team = teams.get(submission.getTeamId());
            out.add(new GameLineupSubmissionDto(
                    submission.getTeamId(),
                    team != null ? team.getName() : null,
                    team != null ? team.getLogo() : null,
                    this.sideOf(game, submission.getTeamId()),
                    submission.getLineupJson(),
                    submission.getUpdatedAt(),
                    Boolean.TRUE.equals(submission.getComplete())));
        }
        return out;
    }

    private JsonNode parseAndValidate(String json, Long gameId, Long teamId, String gameMode) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(400, "阵容数据格式错误");
        }
        if (root == null || !root.isObject()) {
            throw new BusinessException(400, "阵容数据格式错误");
        }
        if (root.path("v").asInt(0) != 1) {
            throw new BusinessException(400, "阵容数据版本不支持");
        }
        if (root.path("gameId").asLong(0L) != gameId.longValue()
                || root.path("teamId").asLong(0L) != teamId.longValue()) {
            throw new BusinessException(400, "阵容数据与当前比赛/球队不匹配");
        }
        JsonNode lineup = root.path("lineup");
        if (!lineup.isArray()) {
            throw new BusinessException(400, "先发名单格式错误");
        }
        int maxLineup = "SOFTBALL".equalsIgnoreCase(gameMode) ? 11 : 12;
        if (lineup.size() > maxLineup) {
            throw new BusinessException(400, "先发名单人数超过上限");
        }
        JsonNode bench = root.path("bench");
        if (!bench.isMissingNode() && !bench.isArray()) {
            throw new BusinessException(400, "替补名单格式错误");
        }
        if (bench.isArray() && bench.size() > MAX_BENCH) {
            throw new BusinessException(400, "替补名单最多 60 人");
        }
        Set<Long> ids = new HashSet<>();
        for (JsonNode row : lineup) {
            this.addUniqueId(ids, row);
        }
        if (bench.isArray()) {
            for (JsonNode row : bench) {
                this.addUniqueId(ids, row);
            }
        }
        // fieldingPitcherId / lastDhAddedFromPoolId 是对打线/替补行的引用（投手或 DH 本身就在行内），
        // 不参与重复行判定；名单校验时按引用球员检查是否在报名名单。
        return root;
    }

    private void addUniqueId(Set<Long> ids, JsonNode node) {
        long id = node.isObject() ? node.path("id").asLong(0L) : node.asLong(0L);
        if (id <= 0L) {
            return;
        }
        if (!ids.add(Long.valueOf(id))) {
            throw new BusinessException(400, "阵容中存在重复球员");
        }
    }

    private Map<String, Boolean> buildSpecialRules(Event event) {
        boolean hasEventRules = this.eventSpecialRuleService.hasRules(event.getId());
        Map<String, Boolean> eventRules = hasEventRules
                ? this.eventSpecialRuleService.getEffective(event.getId())
                : Map.of();
        Map<String, Boolean> out = new LinkedHashMap<>();
        out.put("specialRulesEnabled", Boolean.valueOf(this.leagueSpecialRuleService.isSpecialRulesEnabled(event.getLeagueId())));
        out.put("hasEventRules", Boolean.valueOf(hasEventRules));
        out.put("extraBatter", Boolean.TRUE.equals(eventRules.get("extraBatter")));
        out.put("forceHalf", Boolean.valueOf(!Boolean.FALSE.equals(eventRules.get("forceHalf"))));
        out.put("ohtaniRule", Boolean.TRUE.equals(eventRules.get("ohtaniRule")));
        out.put("softballReentry", Boolean.TRUE.equals(eventRules.get("softballReentry")));
        return out;
    }

    private Team requireTeam(Long teamId, long tid) {
        Team team = teamId == null ? null : this.teamRepository.findById(teamId).orElse(null);
        if (team == null || team.getDeletedAt() != null || !Objects.equals(team.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "球队不存在或无权访问");
        }
        return team;
    }

    private Event requireEvent(Long eventId, long tid) {
        Event event = eventId == null ? null : this.eventRepository.findById(eventId).orElse(null);
        if (event == null || event.getDeletedAt() != null || !Objects.equals(event.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "赛事不存在");
        }
        return event;
    }

    private Game requireGame(Long gameId, long tid) {
        Game game = gameId == null ? null : this.gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getDeletedAt() != null || !Objects.equals(game.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "比赛不存在");
        }
        if (game.getEventId() == null) {
            throw new BusinessException(400, "比赛未关联赛事");
        }
        return game;
    }

    private EventTeam requireConfirmedRegistration(Long eventId, Long teamId, long tid) {
        EventTeam registration = eventId == null || teamId == null ? null
                : this.eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(eventId, teamId).orElse(null);
        if (registration == null || !Objects.equals(registration.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "该球队未参加本赛事");
        }
        if (!EventTeam.STATUS_CONFIRMED.equals(registration.getStatus())) {
            throw new BusinessException(403, "球队尚未确认参加本赛事，不能编辑赛前阵容");
        }
        return registration;
    }

    private void assertParticipant(Game game, Long teamId) {
        if (!Objects.equals(game.getHomeTeamId(), teamId) && !Objects.equals(game.getAwayTeamId(), teamId)) {
            throw new BusinessException(403, "该比赛不是本队参加的比赛");
        }
    }

    private void assertEditable(Game game) {
        String status = game.getStatus();
        if (status != null && !"scheduled".equals(status) && !"postponed".equals(status)) {
            throw new BusinessException(400, "比赛已开始或已结束，不能编辑赛前阵容");
        }
    }

    private Long opponentId(Game game, Long teamId) {
        return Objects.equals(game.getHomeTeamId(), teamId) ? game.getAwayTeamId() : game.getHomeTeamId();
    }

    private String sideOf(Game game, Long teamId) {
        return Objects.equals(game.getHomeTeamId(), teamId) ? "home" : "away";
    }
}
