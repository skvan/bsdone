package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EventRosterPlayerDto;
import com.bsball.model.dto.EventTeamDto;
import com.bsball.model.dto.RosterPlayersDto;
import com.bsball.model.dto.TeamOptionDto;
import com.bsball.model.dto.TeamPlayerOptionDto;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventRosterPlayer;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventRepository;
import com.bsball.repository.EventRosterPlayerRepository;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 电子秩序册（Issue #310）核心服务：赛事参赛登记、报名名单、锁定与移除。
 * <p>写守卫：赛事侧（邀请/移除/锁定）= {@link ResourceGuard#assertCanManageEvent(Long)}；
 * 球队侧（确认/谢绝/提交名单）= {@link ResourceGuard#assertCanStewardOrManageTeam(Long)}
 * （有主球队联盟侧只读；代管无主球队的办赛方可用）。读端点=登录+同租户（读宽写窄）。
 * <p>并发：邀请对赛事行加悲观锁、名单提交对登记行加悲观锁，串行化同键写入防唯一索引冲突。
 */
@Service
public class EventRegistrationService {
    private static final int REMARK_MAX_LENGTH = 500;
    /** 快照 JSON 解析（兼容读取，不承载业务状态）。 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final EventTeamRepository eventTeamRepository;
    private final EventRosterPlayerRepository eventRosterPlayerRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final PlayerService playerService;
    private final ResourceGuard resourceGuard;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public EventRegistrationService(EventRepository eventRepository,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            PlayerTeamRepository playerTeamRepository,
            EventTeamRepository eventTeamRepository,
            EventRosterPlayerRepository eventRosterPlayerRepository,
            GamePlayerStatRepository gamePlayerStatRepository,
            PlayerService playerService,
            ResourceGuard resourceGuard,
            TenantQueryPolicyService tenantQueryPolicyService) {
        this.eventRepository = eventRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.eventTeamRepository = eventTeamRepository;
        this.eventRosterPlayerRepository = eventRosterPlayerRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.playerService = playerService;
        this.resourceGuard = resourceGuard;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }

    /**
     * 邀请球队参赛（幂等）：不存在→新建 invited；declined→重邀重置（清空确认/谢绝/提交时间）；
     * invited/confirmed→跳过不写。被邀球队须与本赛事同租户。赛事行悲观锁串行化并发邀请。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> inviteTeams(Long eventId, List<Long> teamIds, String remark) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageEvent(eventId);
        if (teamIds == null || teamIds.isEmpty()) {
            throw new BusinessException(400, "请选择要邀请的球队");
        }
        if (remark != null && remark.length() > REMARK_MAX_LENGTH) {
            throw new BusinessException(400, "备注不能超过 500 字");
        }
        this.requireEvent(eventId, tid, true);
        int invited = 0;
        int skipped = 0;
        for (Long teamId : teamIds) {
            if (teamId == null) {
                continue;
            }
            this.requireTeam(teamId, tid);
            EventTeam reg = this.eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(eventId, teamId).orElse(null);
            if (reg == null) {
                reg = new EventTeam();
                reg.setTenantId(Long.valueOf(tid));
                reg.setEventId(eventId);
                reg.setTeamId(teamId);
                reg.setStatus(EventTeam.STATUS_INVITED);
                reg.setInvitedBy(CurrentUserHolder.get());
                reg.setInvitedAt(LocalDateTime.now());
                reg.setRemark(remark);
                this.eventTeamRepository.save(reg);
                ++invited;
            } else if (EventTeam.STATUS_DECLINED.equals(reg.getStatus())) {
                reg.setStatus(EventTeam.STATUS_INVITED);
                reg.setInvitedBy(CurrentUserHolder.get());
                reg.setInvitedAt(LocalDateTime.now());
                reg.setDeclinedAt(null);
                reg.setConfirmedAt(null);
                reg.setRosterSubmittedAt(null);
                if (remark != null) {
                    reg.setRemark(remark);
                }
                this.eventTeamRepository.save(reg);
                ++invited;
            } else {
                ++skipped;
            }
        }
        return Map.of("invited", invited, "skipped", skipped);
    }

    /**
     * 赛事可邀请球队（Issue #315 修复）：返回本租户全部未删球队（id/name/shortName/logo）。
     * 守卫=该赛事管理权；不复用管理域过滤的 /team/select-options（其窄域会漏队）。
     */
    public List<TeamOptionDto> listInvitableTeams(Long eventId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageEvent(eventId);
        this.requireEvent(eventId, tid);
        return this.teamRepository.findForSelectByTenantId(Long.valueOf(tid));
    }

    /** 赛事侧参赛列表（含球队名/徽与名单人数）。 */
    public List<EventTeamDto> listEventTeams(Long eventId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.requireEvent(eventId, tid);
        List<EventTeam> regs = this.eventTeamRepository.findByEventIdAndDeletedAtIsNullOrderByIdAsc(eventId);
        if (regs.isEmpty()) {
            return List.of();
        }
        Map<Long, Team> teams = this.teamRepository.findAllById(regs.stream().map(EventTeam::getTeamId).toList())
                .stream().collect(Collectors.toMap(Team::getId, x -> x, (a, b) -> a));
        List<EventTeamDto> out = new ArrayList<EventTeamDto>(regs.size());
        for (EventTeam r : regs) {
            long count = this.eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(eventId, r.getTeamId());
            out.add(this.build(r, teams.get(r.getTeamId()), null, count));
        }
        return out;
    }

    /** 球队侧「我的赛事报名」列表（含赛事名/赛季/模式），按登记倒序；已软删赛事的登记不再展示。 */
    public List<EventTeamDto> listTeamRegistrations(Long teamId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Team team = this.requireTeam(teamId, tid);
        List<EventTeam> regs = this.eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(teamId);
        if (regs.isEmpty()) {
            return List.of();
        }
        Map<Long, Event> events = this.eventRepository.findAllById(regs.stream().map(EventTeam::getEventId).toList())
                .stream().collect(Collectors.toMap(Event::getId, x -> x, (a, b) -> a));
        List<EventTeamDto> out = new ArrayList<EventTeamDto>(regs.size());
        for (EventTeam r : regs) {
            Event ev = events.get(r.getEventId());
            if (ev == null || ev.getDeletedAt() != null) {
                continue;
            }
            long count = this.eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(r.getEventId(), teamId);
            out.add(this.build(r, team, ev, count));
        }
        return out;
    }

    /** 移除参赛（软删登记 + 全部名单行）。守卫：赛事管理；名单已锁定 → 403（须先解锁）。 */
    @Transactional(rollbackFor = Exception.class)
    public void removeEventTeam(Long eventId, Long teamId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageEvent(eventId);
        this.requireEvent(eventId, tid);
        EventTeam reg = this.requireRegistration(eventId, teamId, tid);
        if (Boolean.TRUE.equals(reg.getRosterLocked())) {
            throw new BusinessException(403, "名单已锁定，请先解锁再移除");
        }
        this.softDelete(reg);
        for (EventRosterPlayer row : this.eventRosterPlayerRepository
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(eventId, teamId)) {
            this.softDelete(row);
        }
    }

    /** 锁定/解锁名单（仅办赛方；解锁清空锁定留痕）。 */
    @Transactional(rollbackFor = Exception.class)
    public EventTeamDto lockRoster(Long eventId, Long teamId, boolean locked) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanManageEvent(eventId);
        this.requireEvent(eventId, tid);
        EventTeam reg = this.requireRegistration(eventId, teamId, tid);
        reg.setRosterLocked(Boolean.valueOf(locked));
        reg.setLockedBy(locked ? CurrentUserHolder.get() : null);
        reg.setLockedAt(locked ? LocalDateTime.now() : null);
        this.eventTeamRepository.save(reg);
        return this.toDto(reg);
    }

    /** 查看报名名单明细（含"是否仍在队"提示；赛事已归还/软删 → 404）。 */
    public List<EventRosterPlayerDto> roster(Long eventId, Long teamId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.requireEvent(eventId, tid);
        this.requireRegistration(eventId, teamId, tid);
        List<EventRosterPlayer> rows = this.eventRosterPlayerRepository
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(eventId, teamId);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(EventRosterPlayer::getPlayerId).toList();
        Map<Long, Player> players = this.playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getId, x -> x, (a, b) -> a));
        Set<Long> currentInTeam = this.playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullAndCurrentTrue(ids).stream()
                .filter(e -> Objects.equals(e.getTeamId(), teamId))
                .map(PlayerTeam::getPlayerId)
                .collect(Collectors.toSet());
        List<EventRosterPlayerDto> out = new ArrayList<EventRosterPlayerDto>(rows.size());
        for (EventRosterPlayer r : rows) {
            Player p = players.get(r.getPlayerId());
            out.add(new EventRosterPlayerDto(
                    r.getPlayerId(),
                    p != null ? p.getName() : null,
                    this.resolveNumber(r, p),
                    p != null ? p.getPositionsList() : List.of(),
                    p != null ? p.getStatus() : null,
                    Boolean.valueOf(currentInTeam.contains(r.getPlayerId()))));
        }
        return out;
    }

    /** 阵容限制启用判定：赛事存在任意有效参赛记录（含仅邀请）即启用（Issue #310 决策 5）。 */
    public boolean eventRestricted(Long eventId) {
        return eventId != null && this.eventTeamRepository.existsByEventIdAndDeletedAtIsNull(eventId);
    }

    /**
     * 阵容选人池：受限=该队有效报名名单球员；未受限=本队 current 全量在役
     * （与 {@code /player/list?teamId=} 同口径，前端单路径接入）。
     */
    public RosterPlayersDto rosterPlayers(Long eventId, Long teamId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.requireEvent(eventId, tid);
        if (!this.eventRestricted(eventId)) {
            // 未受限：复用球队球员选项目径（current 在役，单一口径来源）
            return new RosterPlayersDto(Boolean.FALSE, this.playerService.listTeamPlayerOptions(teamId));
        }
        List<EventRosterPlayer> rows = this.eventRosterPlayerRepository
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(eventId, teamId);
        Map<Long, Player> players = this.playerRepository
                .findAllById(rows.stream().map(EventRosterPlayer::getPlayerId).toList()).stream()
                .collect(Collectors.toMap(Player::getId, x -> x, (a, b) -> a));
        List<TeamPlayerOptionDto> out = rows.stream().map(r -> {
            Player p = players.get(r.getPlayerId());
            return new TeamPlayerOptionDto(r.getPlayerId(),
                    p != null ? p.getName() : null,
                    this.resolveNumber(r, p),
                    p != null ? p.getPositionsList() : List.of(),
                    p != null ? p.getBatHand() : null,
                    p != null ? p.getThrowHand() : null,
                    p != null ? p.getStatus() : null);
        }).toList();
        return new RosterPlayersDto(Boolean.TRUE, out);
    }

    /**
     * 门①（阵容保存）：赛事受限时校验快照中两队列/替补/DH 池/守备投手均 ∈ 对应球队名单；
     * 非法 JSON 或未受限赛事不拦截（兼容旧数据）。
     */
    public void assertLineupSnapshotInRoster(Long eventId, Long homeTeamId, Long awayTeamId, String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank() || !this.eventRestricted(eventId)) {
            return;
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(snapshotJson);
        } catch (Exception e) {
            return;
        }
        this.assertSideLineupInRoster(eventId, homeTeamId, root, "home");
        this.assertSideLineupInRoster(eventId, awayTeamId, root, "away");
    }

    private void assertSideLineupInRoster(Long eventId, Long teamId, JsonNode root, String side) {
        if (teamId == null) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<Long>();
        for (String field : List.of(side + "Lineup", side + "Bench")) {
            JsonNode arr = root.path(field);
            if (!arr.isArray()) {
                continue;
            }
            for (JsonNode node : arr) {
                long id = node.path("id").asLong(0L);
                if (id > 0L) {
                    ids.add(Long.valueOf(id));
                }
            }
        }
        for (String field : List.of(side + "FieldingPitcherId", side + "LastDhAddedFromPoolId")) {
            long id = root.path(field).asLong(0L);
            if (id > 0L) {
                ids.add(Long.valueOf(id));
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Set<Long> rosterIds = this.eventRosterPlayerRepository
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(eventId, teamId).stream()
                .map(EventRosterPlayer::getPlayerId)
                .collect(Collectors.toSet());
        for (Long pid : ids) {
            if (!rosterIds.contains(pid)) {
                throw new BusinessException(400, this.notInRosterMessage(teamId, pid));
            }
        }
    }

    private String notInRosterMessage(Long teamId, Long playerId) {
        Player p = this.playerRepository.findById(playerId).orElse(null);
        Team team = this.teamRepository.findById(teamId).orElse(null);
        String teamName = team != null && team.getName() != null ? team.getName() : String.valueOf(teamId);
        return "球员「" + this.playerDisplayName(p, playerId) + "」不在本赛事秩序册名单中（球队：" + teamName + "），请先在赛事报名中提交名单";
    }

    /** 展示名回落：无姓名时用 id。 */
    private String playerDisplayName(Player player, Long playerId) {
        return player != null && player.getName() != null ? player.getName() : String.valueOf(playerId);
    }

    /** 统计写入门（门②）引用的最小标识。 */
    public record StatsRef(Long teamId, Long playerId) {
    }

    /**
     * 门②（统计写入门）：仅拦截"为本场首次引入名单外球员"——
     * 已存在 (gameId, teamId, playerId) 有效统计行的更新不受限（保护进行中比赛与历史修正）；
     * 无赛事/未受限赛事跳过。
     */
    public void assertStatsPartsInRoster(Long eventId, Long gameId, Collection<StatsRef> parts) {
        if (gameId == null || !this.eventRestricted(eventId) || parts == null || parts.isEmpty()) {
            return;
        }
        Set<StatsRef> rosterKeys = this.eventRosterPlayerRepository.findByEventIdAndDeletedAtIsNull(eventId).stream()
                .map(r -> new StatsRef(r.getTeamId(), r.getPlayerId()))
                .collect(Collectors.toSet());
        Set<StatsRef> existingKeys = this.gamePlayerStatRepository.findByGameId(gameId).stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(s -> new StatsRef(s.getTeamId(), s.getPlayerId()))
                .collect(Collectors.toSet());
        for (StatsRef part : parts) {
            if (part == null || part.teamId() == null || part.playerId() == null) {
                continue;
            }
            if (!rosterKeys.contains(part) && !existingKeys.contains(part)) {
                Player p = this.playerRepository.findById(part.playerId()).orElse(null);
                throw new BusinessException(400,
                        "球员「" + this.playerDisplayName(p, part.playerId()) + "」未在本赛事秩序册名单中，无法记入比赛数据");
            }
        }
    }

    /** 确认参加（仅 invited 可操作；confirmed 幂等；declined → 400 引导重邀）。 */
    @Transactional(rollbackFor = Exception.class)
    public EventTeamDto confirm(Long eventId, Long teamId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanStewardOrManageTeam(teamId);
        this.requireEvent(eventId, tid);
        EventTeam reg = this.requireRegistration(eventId, teamId, tid);
        if (EventTeam.STATUS_CONFIRMED.equals(reg.getStatus())) {
            return this.toDto(reg);
        }
        if (EventTeam.STATUS_DECLINED.equals(reg.getStatus())) {
            throw new BusinessException(400, "已谢绝参赛，请联系办赛方重新邀请");
        }
        reg.setStatus(EventTeam.STATUS_CONFIRMED);
        reg.setConfirmedAt(LocalDateTime.now());
        this.eventTeamRepository.save(reg);
        return this.toDto(reg);
    }

    /** 谢绝参赛（仅 invited 可操作；declined 幂等；confirmed → 400 引导联系办赛方）。 */
    @Transactional(rollbackFor = Exception.class)
    public EventTeamDto decline(Long eventId, Long teamId) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanStewardOrManageTeam(teamId);
        this.requireEvent(eventId, tid);
        EventTeam reg = this.requireRegistration(eventId, teamId, tid);
        if (EventTeam.STATUS_DECLINED.equals(reg.getStatus())) {
            return this.toDto(reg);
        }
        if (EventTeam.STATUS_CONFIRMED.equals(reg.getStatus())) {
            throw new BusinessException(400, "已确认参赛，不能直接谢绝，请联系办赛方");
        }
        reg.setStatus(EventTeam.STATUS_DECLINED);
        reg.setDeclinedAt(LocalDateTime.now());
        this.eventTeamRepository.save(reg);
        return this.toDto(reg);
    }

    /**
     * 提交/修改报名名单：锁定 403；须已确认；至少 1 人；球员须为本队 current 在役；
     * 差量同步（新增/更新 sort 与背号快照/软删缺省行），并刷新 roster_submitted_at。
     * 登记行悲观锁串行化并发提交。
     */
    @Transactional(rollbackFor = Exception.class)
    public EventTeamDto submitRoster(Long eventId, Long teamId, List<Long> playerIds) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.resourceGuard.assertCanStewardOrManageTeam(teamId);
        this.requireEvent(eventId, tid);
        EventTeam reg = this.requireRegistrationForUpdate(eventId, teamId, tid);
        if (Boolean.TRUE.equals(reg.getRosterLocked())) {
            throw new BusinessException(403, "名单已锁定，无法修改");
        }
        if (!EventTeam.STATUS_CONFIRMED.equals(reg.getStatus())) {
            throw new BusinessException(400, "请先确认参加后再提交名单");
        }
        List<Long> incoming = (playerIds == null ? List.<Long>of() : playerIds).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (incoming.isEmpty()) {
            throw new BusinessException(400, "请至少选择 1 名球员");
        }
        List<PlayerTeam> entries = this.validateRosterPlayers(tid, teamId, incoming);
        this.syncRosterRows(tid, eventId, teamId, incoming, entries);
        reg.setRosterSubmittedAt(LocalDateTime.now());
        this.eventTeamRepository.save(reg);
        return this.toDto(reg);
    }

    /** 校验球员均为本队当前在役（批量取数，违规 400 并列明姓名）；返回该队经历行供背号快照复用。 */
    private List<PlayerTeam> validateRosterPlayers(long tid, Long teamId, List<Long> incoming) {
        Map<Long, Player> players = this.playerRepository.findAllById(incoming).stream()
                .collect(Collectors.toMap(Player::getId, x -> x, (a, b) -> a));
        List<PlayerTeam> entries = this.playerTeamRepository
                .findByTeamIdAndDeletedAtIsNullAndPlayerIdIn(teamId, incoming);
        Set<Long> currentIds = entries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getCurrent()))
                .map(PlayerTeam::getPlayerId)
                .collect(Collectors.toSet());
        List<String> invalid = new ArrayList<String>();
        for (Long pid : incoming) {
            Player p = players.get(pid);
            boolean bad = p == null || p.getDeletedAt() != null
                    || !Objects.equals(p.getTenantId(), Long.valueOf(tid))
                    || !currentIds.contains(pid);
            if (bad) {
                invalid.add(this.playerDisplayName(p, pid));
            }
        }
        if (!invalid.isEmpty()) {
            throw new BusinessException(400, "以下球员当前不在本队：" + String.join("、", invalid));
        }
        return entries;
    }

    /** 名单差量同步：新增/更新 sort 与背号快照/软删缺省行。 */
    private void syncRosterRows(long tid, Long eventId, Long teamId, List<Long> incoming, List<PlayerTeam> entries) {
        Map<Long, EventRosterPlayer> existing = this.eventRosterPlayerRepository
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(eventId, teamId).stream()
                .collect(Collectors.toMap(EventRosterPlayer::getPlayerId, x -> x, (a, b) -> a));
        Map<Long, PlayerTeam> byPlayer = entries.stream()
                .collect(Collectors.toMap(PlayerTeam::getPlayerId, x -> x, (a, b) -> a));
        int sort = 0;
        for (Long pid : incoming) {
            EventRosterPlayer row = existing.get(pid);
            if (row == null) {
                row = new EventRosterPlayer();
                row.setTenantId(Long.valueOf(tid));
                row.setEventId(eventId);
                row.setTeamId(teamId);
                row.setPlayerId(pid);
            }
            row.setSort(Integer.valueOf(sort++));
            PlayerTeam entry = byPlayer.get(pid);
            row.setNumber(entry != null ? entry.getNumber() : null);
            this.eventRosterPlayerRepository.save(row);
        }
        Set<Long> keep = new HashSet<Long>(incoming);
        for (EventRosterPlayer row : existing.values()) {
            if (!keep.contains(row.getPlayerId())) {
                this.softDelete(row);
            }
        }
    }

    private Event requireEvent(Long eventId, long tid) {
        return this.requireEvent(eventId, tid, false);
    }

    private Event requireEvent(Long eventId, long tid, boolean forUpdate) {
        Event ev = eventId == null
                ? null
                : (forUpdate
                        ? this.eventRepository.findByIdForUpdate(eventId)
                        : this.eventRepository.findById(eventId)).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !Objects.equals(ev.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "赛事不存在");
        }
        return ev;
    }

    private Team requireTeam(Long teamId, long tid) {
        Team team = teamId == null ? null : this.teamRepository.findById(teamId).orElse(null);
        if (team == null || team.getDeletedAt() != null || !Objects.equals(team.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(400, "球队不存在或不属于当前租户");
        }
        return team;
    }

    private EventTeam requireRegistration(Long eventId, Long teamId, long tid) {
        EventTeam reg = eventId == null || teamId == null
                ? null
                : this.eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(eventId, teamId).orElse(null);
        if (reg == null || !Objects.equals(reg.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "该球队未参加本赛事");
        }
        return reg;
    }

    private EventTeam requireRegistrationForUpdate(Long eventId, Long teamId, long tid) {
        EventTeam reg = eventId == null || teamId == null
                ? null
                : this.eventTeamRepository.findForUpdate(eventId, teamId).orElse(null);
        if (reg == null || !Objects.equals(reg.getTenantId(), Long.valueOf(tid))) {
            throw new BusinessException(404, "该球队未参加本赛事");
        }
        return reg;
    }

    private void softDelete(EventTeam reg) {
        reg.setDeletedAt(LocalDateTime.now());
        reg.setDeletedBy(CurrentUserHolder.get());
        this.eventTeamRepository.save(reg);
    }

    private void softDelete(EventRosterPlayer row) {
        row.setDeletedAt(LocalDateTime.now());
        row.setDeletedBy(CurrentUserHolder.get());
        this.eventRosterPlayerRepository.save(row);
    }

    /** 背号展示：报名快照优先，缺失回落球员档案背号。 */
    private String resolveNumber(EventRosterPlayer row, Player player) {
        if (row.getNumber() != null) {
            return row.getNumber();
        }
        return player != null ? player.getNumber() : null;
    }

    private EventTeamDto toDto(EventTeam reg) {
        Team team = this.teamRepository.findById(reg.getTeamId()).orElse(null);
        long count = this.eventRosterPlayerRepository
                .countByEventIdAndTeamIdAndDeletedAtIsNull(reg.getEventId(), reg.getTeamId());
        return this.build(reg, team, null, count);
    }

    /** 组装视图：{@code event} 仅球队侧填充、{@code team} 仅赛事侧填充（另一侧传 null）。 */
    private EventTeamDto build(EventTeam reg, Team team, Event event, long playerCount) {
        return new EventTeamDto(
                reg.getId(),
                reg.getEventId(),
                reg.getTeamId(),
                team != null ? team.getName() : null,
                team != null ? team.getLogo() : null,
                reg.getStatus(),
                reg.getRosterLocked(),
                playerCount,
                reg.getConfirmedAt(),
                reg.getDeclinedAt(),
                reg.getRosterSubmittedAt(),
                reg.getRemark(),
                event != null ? event.getName() : null,
                event != null ? event.getSeason() : null,
                event != null ? event.getGameMode() : null);
    }
}
