/*
 * 账号权限重构（批次 3b 收敛）：比赛（Game）业务服务。
 * 批 2 沉淀⑥ Minor 收敛：清理反编译残留头注释（无行为变化）。
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.dto.EarnedRunDecisionOverrideRequest;
import com.bsball.model.dto.EarnedRunHalfInningRequest;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.GameSubstitutionEvent;
import com.bsball.model.entity.Stadium;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.service.query.ScopeQuerySupport;
import com.bsball.utils.GamePlayerStatZeros;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GameService {
    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ALLOWED_SUBSTITUTION_TYPES = Set.of("SUBSTITUTION", "POSITION_CHANGE", "PITCHING_CHANGE", "DH_CANCEL");
    private final GameRepository gameRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final GameSubstitutionEventRepository gameSubstitutionEventRepository;
    private final EventRepository eventRepository;
    private final StadiumRepository stadiumRepository;
    private final AccountScopeService accountScopeService;
    private final ScopeQuerySupport scopeQuerySupport;
    private final ResourceGuard resourceGuard;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final EarnedRunReconstructionService earnedRunReconstructionService;
    private final EventRegistrationService eventRegistrationService;

    public PageResult<Game> list(Integer page, Integer pageSize, String sortProp, String sortOrder, Long eventId, List<Long> eventIds, List<Integer> years, Long teamId) {
        boolean hasFilter;
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        long scopeTid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleEventIds = this.scopeQuerySupport.visibleEventIds(scope, scopeTid);
        if (visibleEventIds != null && visibleEventIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        hasFilter = eventId != null || eventIds != null && !eventIds.isEmpty() || years != null && !years.isEmpty();
        if (!hasFilter) {
            Specification spec = (root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                preds.add(cb.isNull((Expression)root.get("deletedAt")));
                if (tid != null) {
                    preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (visibleEventIds != null) {
                    preds.add(root.get("eventId").in((Collection)visibleEventIds));
                }
                if (teamId != null) {
                    preds.add(cb.or((Expression)cb.equal((Expression)root.get("homeTeamId"), (Object)teamId), (Expression)cb.equal((Expression)root.get("awayTeamId"), (Object)teamId)));
                }
                return cb.and(preds.toArray(new Predicate[0]));
            };
            Page result = this.gameRepository.findAll((Specification)spec, p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        Specification spec = (root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tid != null) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (visibleEventIds != null) {
                preds.add(root.get("eventId").in((Collection)visibleEventIds));
            }
            if (teamId != null) {
                preds.add(cb.or((Expression)cb.equal((Expression)root.get("homeTeamId"), (Object)teamId), (Expression)cb.equal((Expression)root.get("awayTeamId"), (Object)teamId)));
            }
            if (eventId != null) {
                preds.add(cb.equal((Expression)root.get("eventId"), (Object)eventId));
            }
            if (eventIds != null && !eventIds.isEmpty()) {
                preds.add(root.get("eventId").in((Collection)eventIds));
            }
            if (years != null && !years.isEmpty()) {
                Predicate[] yearPreds = (Predicate[])years.stream().map(y -> cb.like((Expression)root.get("gameday"), y + "%")).toArray(Predicate[]::new);
                preds.add(cb.or(yearPreds));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.gameRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public Game get(Long id) {
        Game g = this.gameRepository.findById(id).orElse(null);
        if (g == null || g.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!this.tenantQueryPolicyService.isGlobalQueryMode() && !Objects.equals(g.getTenantId(), tid)) {
            return null;
        }
        if (g.getEventId() == null) {
            return null;
        }
        Event ev = this.eventRepository.findById(g.getEventId()).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !this.tenantQueryPolicyService.isGlobalQueryMode() && !Objects.equals(ev.getTenantId(), tid)) {
            return null;
        }
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleLeagueIds = this.scopeQuerySupport.visibleLeagueIds(scope);
        // 批次 3b（前置⑤）：管理上下文 + 受限身份下，leagueId 为 null 亦拒绝（与 list 收窄口径一致）；非管理上下文（宽读）不变。
        if (visibleLeagueIds != null && (ev.getLeagueId() == null || !visibleLeagueIds.contains(ev.getLeagueId()))) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u6bd4\u8d5b");
        }
        return g;
    }

    public String getLiveSnapshot(Long gameId) {
        Game g = this.get(gameId);
        return g == null ? null : g.getLiveSnapshotJson();
    }

    @Transactional
    public void saveLiveSnapshot(Long gameId, String json) {
        Game g = this.gameRepository.findById(gameId).orElse(null);
        if (g == null) {
            return;
        }
        this.assertGameWritable(g);
        g.setLiveSnapshotJson(json);
        this.gameRepository.save(g);
    }

    /**
     * 阵容专用保存（门①，Issue #310）：受限赛事校验两队列/替补/守备投手 ⊆ 对应球队名单后落快照。
     * 阵容板 save()/startEntry() 走此端点；比赛进行中的高频自动保存仍走 {@link #saveLiveSnapshot}（不解析校验）。
     */
    @Transactional
    public void saveLineupSnapshot(Long gameId, String json) {
        Game g = this.gameRepository.findById(gameId).orElse(null);
        if (g == null) {
            return;
        }
        this.assertGameWritable(g);
        if (g.getEventId() != null) {
            this.eventRegistrationService.assertLineupSnapshotInRoster(
                    g.getEventId(), g.getHomeTeamId(), g.getAwayTeamId(), json);
        }
        g.setLiveSnapshotJson(json);
        this.gameRepository.save(g);
    }

    public Game create(Game entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Event ev = this.eventRepository.findById(entity.getEventId()).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(400, "\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        this.resourceGuard.assertCanManageLeague(ev.getLeagueId());
        // 报名制门槛（Issue #318 决策 11）：新建赛事须至少两支已完成报名（确认+锁定名单）的球队方可排赛
        this.eventRegistrationService.assertGameTeamsEligible(entity.getEventId(), entity.getHomeTeamId(), entity.getAwayTeamId());
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(entity.getStadiumId(), tid);
        }
        return (Game)this.gameRepository.save(entity);
    }

    public Game update(Long id, Game entity) {
        Game existing = this.gameRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6bd4\u8d5b");
        }
        long eid = entity.getEventId() != null ? entity.getEventId() : existing.getEventId();
        Event ev = this.eventRepository.findById(eid).orElse(null);
        if (ev == null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(400, "\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        this.resourceGuard.assertCanManageGame(id);
        if (entity.getEventId() != null && !entity.getEventId().equals(existing.getEventId())) {
            this.resourceGuard.assertCanManageEvent(entity.getEventId());
        }
        // 报名制门槛（Issue #318 决策 11）：改比赛主客队/迁移赛事同样校验；按合并后最终值判定（缺省沿用既有，防绕过）
        this.eventRegistrationService.assertGameTeamsEligible(eid,
                entity.getHomeTeamId() != null ? entity.getHomeTeamId() : existing.getHomeTeamId(),
                entity.getAwayTeamId() != null ? entity.getAwayTeamId() : existing.getAwayTeamId());
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(entity.getStadiumId(), tid);
        }
        return (Game)this.gameRepository.save(entity);
    }

    private void assertStadiumBelongsToTenant(Long stadiumId, long tenantId) {
        Stadium s = this.stadiumRepository.findById(stadiumId).orElse(null);
        if (s == null || s.getDeletedAt() != null || !Objects.equals(s.getTenantId(), tenantId)) {
            throw new BusinessException(400, "\u7403\u573a\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Game existing = this.gameRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        this.assertGameWritable(existing);
        if (!this.resourceGuard.isCurrentUserSuperAdmin()) {
            // 历史数据处置权（spec §6.10）：非超管删除 = 归还（软删 + 平台资产标记），不改 tenant_id
            existing.setPlatformOwned(Boolean.TRUE);
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.gameRepository.save(existing);
    }

    @Transactional
    public void saveLive(Long gameId, GameSaveLiveDTO dto) {
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return;
        }
        this.assertGameWritable(game);
        if (dto.getGame() != null) {
            GameSaveLiveDTO.GameUpdatePart g = dto.getGame();
            if (g.getHomeScore() != null) {
                game.setHomeScore(g.getHomeScore());
            }
            if (g.getAwayScore() != null) {
                game.setAwayScore(g.getAwayScore());
            }
            if (g.getHomeScoreByInning() != null) {
                game.setHomeScoreByInning((Object)this.toInningJson(g.getHomeScoreByInning()));
            }
            if (g.getAwayScoreByInning() != null) {
                game.setAwayScoreByInning((Object)this.toInningJson(g.getAwayScoreByInning()));
            }
            if (g.getHomeH() != null) {
                game.setHomeH(g.getHomeH());
            }
            if (g.getAwayH() != null) {
                game.setAwayH(g.getAwayH());
            }
            if (g.getHomeE() != null) {
                game.setHomeE(g.getHomeE());
            }
            if (g.getAwayE() != null) {
                game.setAwayE(g.getAwayE());
            }
            if (g.getStatus() != null) {
                game.setStatus(g.getStatus());
            }
            if (g.getGameEndTime() != null) {
                game.setGameEndTime(g.getGameEndTime());
            }
            if (g.getInning() != null) {
                game.setInning(g.getInning());
            }
            if (g.getTopBottom() != null) {
                game.setTopBottom(g.getTopBottom());
            }
            this.gameRepository.save(game);
        }
        if (dto.getStats() != null && !dto.getStats().isEmpty()) {
            this.eventRegistrationService.assertStatsPartsInRoster(game.getEventId(), gameId,
                    dto.getStats().stream()
                            .map(s -> new EventRegistrationService.StatsRef(s.getTeamId(), s.getPlayerId()))
                            .toList());
            boolean engineManaged = this.isEngineManaged(game);
            List<GamePlayerStat> existing = this.gamePlayerStatRepository.findByGameId(gameId);
            Map<String, GamePlayerStat> existingMap = existing.stream().collect(Collectors.toMap(s -> s.getTeamId() + "_" + s.getPlayerId() + "_" + (s.getIsPitcher() == null ? 0 : s.getIsPitcher()), s -> s, (a, b) -> statRichness(a) >= statRichness(b) ? a : b));
            for (GameSaveLiveDTO.GamePlayerStatPart part : dto.getStats()) {
                if (part.getTeamId() == null || part.getPlayerId() == null) continue;
                String key = part.getTeamId() + "_" + part.getPlayerId() + "_" + (part.getIsPitcher() == null ? 0 : part.getIsPitcher());
                GamePlayerStat stat = existingMap.get(key);
                Long statTid = game.getTenantId();
                if (stat != null) {
                    this.applyPartToStat(stat, part, gameId, statTid, engineManaged);
                    this.gamePlayerStatRepository.save(stat);
                    continue;
                }
                GamePlayerStat newStat = new GamePlayerStat();
                newStat.setGameId(gameId);
                this.applyPartToStat(newStat, part, gameId, statTid, engineManaged);
                this.gamePlayerStatRepository.save(newStat);
            }
        }
        this.saveSubstitutionEvents(game, dto.getSubstitutions());
    }

    /** 换人事务事件：与统计同一事务落库；同一 transactionId 重试幂等。 */
    private void saveSubstitutionEvents(Game game, List<GameSaveLiveDTO.SubstitutionPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        if (parts.size() > 24) {
            throw new BusinessException(400, "换人事务变更过多");
        }
        String transactionId = parts.get(0) == null ? null : parts.get(0).getTransactionId();
        if (transactionId == null || transactionId.isBlank()) {
            throw new BusinessException(400, "换人事务缺少 transactionId");
        }
        if (this.gameSubstitutionEventRepository.existsByGameIdAndTransactionId(game.getId(), transactionId)) {
            return;
        }
        List<EventRegistrationService.StatsRef> refs = new ArrayList<>();
        List<GameSubstitutionEvent> events = new ArrayList<>();
        int sequence = this.gameSubstitutionEventRepository.findMaxSequence(game.getId());
        int changeSeq = 0;
        for (GameSaveLiveDTO.SubstitutionPart part : parts) {
            if (part == null) {
                continue;
            }
            this.validateSubstitutionPart(game, part, transactionId, refs);
            events.add(this.toSubstitutionEvent(game, part, transactionId, ++sequence, ++changeSeq));
        }
        this.eventRegistrationService.assertStatsPartsInRoster(game.getEventId(), game.getId(), refs);
        this.gameSubstitutionEventRepository.saveAll(events);
    }

    private void validateSubstitutionPart(Game game, GameSaveLiveDTO.SubstitutionPart part,
            String transactionId, List<EventRegistrationService.StatsRef> refs) {
        if (!transactionId.equals(part.getTransactionId())) {
            throw new BusinessException(400, "换人事务 ID 不一致");
        }
        Long teamId = part.getTeamId();
        if (!Objects.equals(teamId, game.getHomeTeamId()) && !Objects.equals(teamId, game.getAwayTeamId())) {
            throw new BusinessException(400, "换人球队不属于本场比赛");
        }
        String changeType = part.getChangeType() == null ? "" : part.getChangeType().trim().toUpperCase();
        if (!ALLOWED_SUBSTITUTION_TYPES.contains(changeType)) {
            throw new BusinessException(400, "未知换人类型：" + part.getChangeType());
        }
        if (part.getSlot() != null && (part.getSlot() < 1 || part.getSlot() > 12)) {
            throw new BusinessException(400, "换人槽位越界");
        }
        if (part.getOutPlayerId() == null && part.getInPlayerId() == null) {
            throw new BusinessException(400, "换人事件缺少球员");
        }
        if ("SUBSTITUTION".equals(changeType) && Objects.equals(part.getOutPlayerId(), part.getInPlayerId())) {
            throw new BusinessException(400, "换下与换上球员不能相同");
        }
        if ("POSITION_CHANGE".equals(changeType) && !Objects.equals(part.getOutPlayerId(), part.getInPlayerId())) {
            throw new BusinessException(400, "守位调整的球员必须一致");
        }
        if (part.getOutPlayerId() != null) {
            refs.add(new EventRegistrationService.StatsRef(teamId, part.getOutPlayerId()));
        }
        if (part.getInPlayerId() != null) {
            refs.add(new EventRegistrationService.StatsRef(teamId, part.getInPlayerId()));
        }
    }

    private GameSubstitutionEvent toSubstitutionEvent(Game game, GameSaveLiveDTO.SubstitutionPart part,
            String transactionId, int sequence, int changeSeq) {
        GameSubstitutionEvent event = new GameSubstitutionEvent();
        event.setGameId(game.getId());
        event.setTenantId(game.getTenantId());
        event.setTeamId(part.getTeamId());
        event.setTransactionId(transactionId);
        event.setInning(part.getInning());
        event.setTopBottom(part.getTopBottom());
        event.setSequence(sequence);
        event.setChangeSeq(changeSeq);
        event.setChangeType(part.getChangeType().trim().toUpperCase());
        event.setSlot(part.getSlot());
        event.setOutPlayerId(part.getOutPlayerId());
        event.setInPlayerId(part.getInPlayerId());
        event.setOutPosition(normalizePosition(part.getOutPosition()));
        event.setInPosition(normalizePosition(part.getInPosition()));
        return event;
    }

    private static String normalizePosition(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 8 ? trimmed.substring(0, 8) : trimmed;
    }

    /** 换人事务事件（按全场顺序）；读取沿用比赛详情的数据范围校验。 */
    public List<GameSubstitutionEvent> listSubstitutions(Long gameId) {
        if (this.get(gameId) == null) {
            throw new BusinessException(403, "无权查看该比赛");
        }
        return this.gameSubstitutionEventRepository.findByGameIdOrderBySequenceAscIdAsc(gameId);
    }

    /** 该场是否已有 ER 引擎事件；供前端把 ER/R 设为只读。 */
    public boolean isEngineManaged(Game game) {
        return game != null && game.getId() != null
                && this.earnedRunReconstructionService.hasEngineData(game.getId());
    }

    /** Reconstructs and stores all earned-run decisions for one half-inning. */
    @Transactional
    public EarnedRunReconstructionResult reconstructEarnedRuns(
            Long gameId, EarnedRunHalfInningRequest request) {
        if (request == null || request.getInning() == null || request.getPlays() == null) {
            throw new BusinessException(400, "缺少自责分重建资料");
        }
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            throw new BusinessException(404, "比赛不存在");
        }
        this.assertGameWritable(game);
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        Long fieldingTeamId = "top".equals(request.getHalf())
                ? game.getHomeTeamId()
                : game.getAwayTeamId();
        return this.earnedRunReconstructionService.replaceHalfInning(
                game.getTenantId(),
                gameId,
                request.getInning(),
                request.getHalf(),
                operatorId,
                request.getPlays(),
                fieldingTeamId);
    }

    /** Resolves a pending earned-run decision and records the reviewing operator. */
    @Transactional
    public EarnedRunDecisionEntity overrideEarnedRunDecision(
            Long gameId, Long decisionId, EarnedRunDecisionOverrideRequest request) {
        if (request == null || request.getEarnedStatus() == null) {
            throw new BusinessException(400, "缺少自責分覆核結果");
        }
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            throw new BusinessException(404, "比賽不存在");
        }
        this.assertGameWritable(game);
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登入");
        }
        try {
            return this.earnedRunReconstructionService.overrideDecision(
                    game.getTenantId(), gameId, decisionId, operatorId,
                    request.getEarnedStatus(), request.getUnearnedReason());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    @Transactional(rollbackFor={Exception.class})

    public java.util.List<com.bsball.model.dto.EarnedRunDecisionListDTO> listEarnedRunDecisions(Long gameId) {
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getTenantId() == null) {
            return java.util.List.of();
        }
        return this.earnedRunReconstructionService.listDecisions(game.getTenantId(), gameId);
    }

    public Long importGameResult(Long eventId, SaveGameResultDTO dto) {
        if (dto == null || dto.getGame() == null || dto.getStats() == null) {
            throw new IllegalArgumentException("\u5bfc\u5165\u6570\u636e\u7f3a\u5c11 game \u6216 stats");
        }
        if (dto.getGame() != null && !Boolean.TRUE.equals(dto.getGame().getIsSpecialResult())) {
            this.assertStarterFieldingPositions(dto.getStats(), "\u5bfc\u5165\u6bd4\u8d5b");
            // 导入缺省落 final（历史数据补录）；与 save-result 同口径，仅进行中场次做 P/DH 拦截
            String importStatus = dto.getGame().getStatus() != null ? dto.getGame().getStatus() : "final";
            if (!"final".equalsIgnoreCase(importStatus)) {
                this.assertPitcherDhRule(dto.getStats(), "\u5bfc\u5165\u6bd4\u8d5b");
            }
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Event ev = this.eventRepository.findById(eventId).orElse(null);
        if (ev == null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(400, "\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        this.resourceGuard.assertCanManageEvent(eventId);
        SaveGameResultDTO.GamePart g = dto.getGame();
        Game game = new Game();
        game.setEventId(eventId);
        game.setTenantId(Long.valueOf(tid));
        game.setHomeTeamId(g.getHomeTeamId());
        game.setAwayTeamId(g.getAwayTeamId());
        // 报名制门槛（Issue #318 决策 11）：导入建比赛同口径校验（require_roster=false 历史赛事自动跳过）
        this.eventRegistrationService.assertGameTeamsEligible(eventId, g.getHomeTeamId(), g.getAwayTeamId());
        game.setGameTime(g.getGameTime());
        game.setGameEndTime(g.getGameEndTime());
        game.setGameday(g.getGameday());
        game.setGameNumber(g.getGameNumber());
        if (g.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(g.getStadiumId(), tid);
        }
        game.setStadiumId(g.getStadiumId());
        game.setVenue(g.getVenue());
        game.setStatus(g.getStatus() != null ? g.getStatus() : "final");
        game.setTotalInnings(g.getTotalInnings());
        game.setHomeScore(Integer.valueOf(g.getHomeScore() != null ? g.getHomeScore() : 0));
        game.setAwayScore(Integer.valueOf(g.getAwayScore() != null ? g.getAwayScore() : 0));
        game.setHomeScoreByInning((Object)this.toInningJson(g.getHomeScoreByInning()));
        game.setAwayScoreByInning((Object)this.toInningJson(g.getAwayScoreByInning()));
        game.setHomeH(g.getHomeH());
        game.setAwayH(g.getAwayH());
        game.setHomeE(g.getHomeE());
        game.setAwayE(g.getAwayE());
        game.setSpectatorCount(g.getSpectatorCount());
        game.setUmpireHp(g.getUmpireHp());
        game.setUmpire1b(g.getUmpire1b());
        game.setUmpire2b(g.getUmpire2b());
        game.setUmpire3b(g.getUmpire3b());
        game.setRecorders(g.getRecorders());
        if (g.getGameTag() != null) {
            game.setGameTag(g.getGameTag());
        }
        if (g.getWeatherSummary() != null) {
            game.setWeatherSummary(g.getWeatherSummary());
        }
        if (g.getWeatherTempC() != null) {
            game.setWeatherTempC(g.getWeatherTempC());
        }
        if (g.getWeatherWind() != null) {
            game.setWeatherWind(g.getWeatherWind());
        }
        if (g.getWeatherRainProbPct() != null) {
            game.setWeatherRainProbPct(g.getWeatherRainProbPct());
        }
        if (g.getRemark() != null) {
            game.setRemark(g.getRemark());
        }
        game = (Game)this.gameRepository.save(game);
        Long gameId = game.getId();
        for (SaveGameResultDTO.StatPart part : dto.getStats()) {
            if (part.getTeamId() == null || part.getPlayerId() == null) continue;
            GamePlayerStat stat = new GamePlayerStat();
            this.applyResultPartToStat(stat, part, gameId, Long.valueOf(tid), false);
            this.gamePlayerStatRepository.save(stat);
        }
        return gameId;
    }

    @Transactional
    public void saveResult(Long gameId, SaveGameResultDTO dto) {
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return;
        }
        // 批次 3b（前置④）：守卫前移——位内任何 set/save 之前，基于「原域」判定（防跳域搬运）。
        this.assertGameWritable(game);
        if (dto != null && dto.getStats() != null && !Boolean.TRUE.equals(game.getIsSpecialResult())) {
            // 先发 9 人/守位校验只约束「进行中开赛」；已完赛（final）允许赛后补录，
            // 不再强制首发打序齐全（生产比赛 250：换人后补录被误拦）。
            String effectiveStatus = dto.getGame() != null && dto.getGame().getStatus() != null
                    ? dto.getGame().getStatus()
                    : game.getStatus();
            if (!"final".equalsIgnoreCase(effectiveStatus)) {
                this.assertStarterFieldingPositions(dto.getStats(), "\u4fdd\u5b58\u6bd4\u8d5b");
            }
            // P/DH 校验按「库内状态 + 本次请求状态」取非 final 门控：同请求置 final 不能绕过，
            // final 重开为 live 也照常校验；仅两侧都已完赛（纯赛后补录）不拦截
            if (!"final".equalsIgnoreCase(game.getStatus()) || !"final".equalsIgnoreCase(effectiveStatus)) {
                this.assertPitcherDhRule(dto.getStats(), "\u4fdd\u5b58\u6bd4\u8d5b");
            }
            // 门②（Issue #310）：统计写入门；若本请求同时迁移 eventId，按目标赛事口径校验
            Long rosterEventId = dto.getGame() != null && dto.getGame().getEventId() != null
                    ? dto.getGame().getEventId()
                    : game.getEventId();
            this.eventRegistrationService.assertStatsPartsInRoster(rosterEventId, gameId,
                    dto.getStats().stream()
                            .map(s -> new EventRegistrationService.StatsRef(s.getTeamId(), s.getPlayerId()))
                            .toList());
        }
        if (dto.getGame() != null) {
            SaveGameResultDTO.GamePart g = dto.getGame();
            if (g.getEventId() != null) {
                if (!g.getEventId().equals(game.getEventId())) {
                    // 改 eventId：原域已在 assertGameWritable 校验；此处再校验目标域可管理，禁跨域搬运。
                    this.resourceGuard.assertCanManageEvent(g.getEventId());
                }
                game.setEventId(g.getEventId());
            }
            if (g.getHomeTeamId() != null) {
                game.setHomeTeamId(g.getHomeTeamId());
            }
            if (g.getAwayTeamId() != null) {
                game.setAwayTeamId(g.getAwayTeamId());
            }
            // 报名制门槛（Issue #318 决策 11）：保存结果改主客队同样校验
            if (g.getHomeTeamId() != null || g.getAwayTeamId() != null) {
                this.eventRegistrationService.assertGameTeamsEligible(game.getEventId(), game.getHomeTeamId(), game.getAwayTeamId());
            }
            if (g.getGameTime() != null) {
                game.setGameTime(g.getGameTime());
            }
            if (g.getGameEndTime() != null) {
                game.setGameEndTime(g.getGameEndTime());
            }
            if (g.getGameday() != null) {
                game.setGameday(g.getGameday());
            }
            // 場次/場地允許清空（issue #28）：與同方法內的 stadiumId 一致採無條件寫入；
            // 前端在清空下拉/輸入時會省略該鍵，後端收到 null 即視為清空。
            game.setGameNumber(g.getGameNumber());
            game.setVenue(g.getVenue());
            if (g.getStadiumId() != null) {
                this.assertStadiumBelongsToTenant(g.getStadiumId(), game.getTenantId().longValue());
            }
            game.setStadiumId(g.getStadiumId());
            if (g.getStatus() != null) {
                game.setStatus(g.getStatus());
            }
            if (g.getGameMode() != null) {
                game.setGameMode(g.getGameMode());
            }
            if (g.getTotalInnings() != null) {
                game.setTotalInnings(g.getTotalInnings());
            }
            if (g.getHomeScore() != null) {
                game.setHomeScore(g.getHomeScore());
            }
            if (g.getAwayScore() != null) {
                game.setAwayScore(g.getAwayScore());
            }
            if (g.getHomeScoreByInning() != null) {
                game.setHomeScoreByInning((Object)this.toInningJson(g.getHomeScoreByInning()));
            }
            if (g.getAwayScoreByInning() != null) {
                game.setAwayScoreByInning((Object)this.toInningJson(g.getAwayScoreByInning()));
            }
            if (g.getHomeH() != null) {
                game.setHomeH(g.getHomeH());
            }
            if (g.getAwayH() != null) {
                game.setAwayH(g.getAwayH());
            }
            if (g.getHomeE() != null) {
                game.setHomeE(g.getHomeE());
            }
            if (g.getAwayE() != null) {
                game.setAwayE(g.getAwayE());
            }
            game.setSpectatorCount(g.getSpectatorCount());
            game.setUmpireHp(g.getUmpireHp());
            game.setUmpire1b(g.getUmpire1b());
            game.setUmpire2b(g.getUmpire2b());
            game.setUmpire3b(g.getUmpire3b());
            game.setRecorders(g.getRecorders());
            if (g.getGameTag() != null) {
                game.setGameTag(g.getGameTag());
            }
            if (g.getWeatherSummary() != null) {
                game.setWeatherSummary(g.getWeatherSummary());
            }
            if (g.getWeatherTempC() != null) {
                game.setWeatherTempC(g.getWeatherTempC());
            }
            if (g.getWeatherWind() != null) {
                game.setWeatherWind(g.getWeatherWind());
            }
            if (g.getWeatherRainProbPct() != null) {
                game.setWeatherRainProbPct(g.getWeatherRainProbPct());
            }
            if (g.getRemark() != null) {
                game.setRemark(g.getRemark());
            }
            game.setIsSpecialResult(g.getIsSpecialResult());
            game.setShowRemarkInCard(g.getShowRemarkInCard());
            game.setIncludeStatsInRanking(g.getIncludeStatsInRanking());
            Event evSync = this.eventRepository.findById(game.getEventId()).orElse(null);
            if (evSync != null) {
                game.setTenantId(evSync.getTenantId());
            }
            this.gameRepository.save(game);
        }
        Long statTenantId = game.getTenantId();
        if (dto.getStats() != null) {
            boolean engineManaged = this.isEngineManaged(game);
            List<GamePlayerStat> existing = this.gamePlayerStatRepository.findByGameId(gameId);
            LinkedHashSet<Long> declaredIds = new LinkedHashSet<>();
            for (SaveGameResultDTO.StatPart part : dto.getStats()) {
                if (part.getId() != null) {
                    declaredIds.add(part.getId());
                }
            }
            // 行身分複合鍵與 saveLive 保持一致：teamId_playerId_isPitcher。
            // 同一球員可有「打者列 + 投手列」兩列並存，所以不能只靠 id 決定去留。
            Map<Long, GamePlayerStat> existingById = new HashMap<>();
            Map<String, GamePlayerStat> existingByKey = new LinkedHashMap<>();
            for (GamePlayerStat s : existing) {
                existingById.put(s.getId(), s);
                String key = resultStatKey(s.getTeamId(), s.getPlayerId(), s.getIsPitcher());
                GamePlayerStat prev = existingByKey.get(key);
                if (prev != null && (declaredIds.contains(prev.getId()) || !declaredIds.contains(s.getId()))) continue;
                existingByKey.put(key, s);
            }
            LinkedHashSet<Long> keepIds = new LinkedHashSet<>();
            for (SaveGameResultDTO.StatPart part : dto.getStats()) {
                GamePlayerStat stat;
                if (part.getTeamId() == null || part.getPlayerId() == null) continue;
                int isPitcher = part.getIsPitcher() == null ? 0 : part.getIsPitcher().intValue();
                if (part.getId() != null) {
                    GamePlayerStat byId = existingById.get(part.getId());
                    // id 指向的列必須與該筆身分一致才認，否則視為前端 id 錯位（例如投手筆帶了打者列 id），
                    // 改由複合鍵決定落點，避免把既有列的 isPitcher 就地翻轉。
                    if (byId != null && isPitcher == (byId.getIsPitcher() == null ? 0 : byId.getIsPitcher().intValue())) {
                        stat = byId;
                        this.applyResultPartToStat(stat, part, gameId, statTenantId, engineManaged);
                        this.gamePlayerStatRepository.save(stat);
                        keepIds.add(stat.getId());
                        continue;
                    }
                }
                String key = resultStatKey(part.getTeamId(), part.getPlayerId(), Integer.valueOf(isPitcher));
                stat = existingByKey.get(key);
                if (stat == null) {
                    stat = new GamePlayerStat();
                }
                this.applyResultPartToStat(stat, part, gameId, statTenantId, engineManaged);
                this.gamePlayerStatRepository.save(stat);
                keepIds.add(stat.getId());
                existingByKey.put(key, stat);
            }
            // 刪除改為「本次沒有被任何一筆落點命中的列」，而不是「id 沒出現在 payload 裡」。
            // 兩者對正常 payload 等價，但可避免前端少帶一根 id 時把同一球員的另一列物理刪除。
            for (GamePlayerStat s : existing) {
                if (keepIds.contains(s.getId())) continue;
                // 引擎场次的投手行由 ER 引擎维护：仍有数据时不得因 payload 漏带被物理删除；
                // 全零且没有自责分判定的行允许清理（换人错记后的转记/删除流程）。
                if (engineManaged && Integer.valueOf(1).equals(s.getIsPitcher())
                        && !this.isDeletableEnginePitcherRow(gameId, s)) {
                    continue;
                }
                this.gamePlayerStatRepository.delete(s);
            }
        }
        // §11 結算驗證（非阻擋，僅記錄警告供人工複核）
        if (statTenantId != null && this.earnedRunReconstructionService.hasEngineData(gameId)) {
            List<String> vs = this.earnedRunReconstructionService.validateSettlement(statTenantId, gameId);
            for (String v : vs) {
                log.warn("[settlement-validation] gameId={} tenant={}: {}", gameId, statTenantId, v);
            }
        }
    }

    /** 比賽統計列身分複合鍵：與 saveLive 的 existingMap key 同構（isPitcher 為 null 時視為 0） */
    private static String resultStatKey(Long teamId, Long playerId, Integer isPitcher) {
        return String.valueOf(teamId) + "_" + playerId + "_" + (isPitcher == null ? 0 : isPitcher.intValue());
    }

    /** 引擎场次的投手行能否在 payload 漏带时清理：全零且没有以该投手为责任投手的自责分判定。 */
    private boolean isDeletableEnginePitcherRow(Long gameId, GamePlayerStat stat) {
        return GamePlayerStatZeros.isZero(stat)
                && (stat.getPlayerId() == null
                    || !this.earnedRunReconstructionService.hasResponsiblePitcherDecisions(
                            gameId, stat.getPlayerId()));
    }

    private void applyResultPartToStat(GamePlayerStat stat, SaveGameResultDTO.StatPart part,
            Long gameId, Long tenantId, boolean preserveEngineTotals) {
        stat.setTenantId(tenantId);
        stat.setGameId(gameId);
        stat.setTeamId(part.getTeamId());
        stat.setPlayerId(part.getPlayerId());
        stat.setBattingOrder(part.getBattingOrder());
        stat.setListOrder(part.getListOrder());
        stat.setPosition(part.getPosition());
        stat.setNumber(part.getNumber());
        stat.setBatHand(part.getBatHand());
        stat.setThrowHand(part.getThrowHand());
        stat.setPa(Integer.valueOf(part.getPa() != null ? part.getPa() : 0));
        stat.setAb(Integer.valueOf(part.getAb() != null ? part.getAb() : 0));
        stat.setR(Integer.valueOf(part.getR() != null ? part.getR() : 0));
        stat.setH(Integer.valueOf(part.getH() != null ? part.getH() : 0));
        stat.setE(Integer.valueOf(part.getE() != null ? part.getE() : 0));
        stat.setBbHp(Integer.valueOf(part.getBbHp() != null ? part.getBbHp() : 0));
        stat.setSb(Integer.valueOf(part.getSb() != null ? part.getSb() : 0));
        stat.setSo(Integer.valueOf(part.getSo() != null ? part.getSo() : 0));
        stat.setSoSwing(Integer.valueOf(part.getSoSwing() != null ? part.getSoSwing() : 0));
        stat.setSoLooking(Integer.valueOf(part.getSoLooking() != null ? part.getSoLooking() : 0));
        stat.setRbi(Integer.valueOf(part.getRbi() != null ? part.getRbi() : 0));
        stat.setHr(Integer.valueOf(part.getHr() != null ? part.getHr() : 0));
        stat.setInsideParkHr(part.getInsideParkHr());
        stat.setDoubles(part.getDoubles());
        stat.setTriples(part.getTriples());
        stat.setGdp(part.getGdp());
        stat.setSh(part.getSh());
        stat.setSf(part.getSf());
        stat.setBb(part.getBb());
        stat.setIbb(part.getIbb());
        stat.setHbp(part.getHbp());
        if (part.getIo() != null) {
            stat.setIo(part.getIo());
        }
        stat.setCs(part.getCs());
        stat.setIsPitcher(Integer.valueOf(part.getIsPitcher() != null ? part.getIsPitcher() : 0));
        stat.setPitcherOrder(part.getPitcherOrder());
        stat.setIp(part.getIp());
        if (preserveEngineTotals) {
            if (stat.getId() == null) {
                zeroEngineTotals(stat);
            }
        } else {
            // 旧客户端/未携带字段时保留既有值，避免把 legacy 场次的 ER/R 清空
            if (part.getEr() != null) {
                stat.setEr(part.getEr());
            }
            if (part.getPitchR() != null) {
                stat.setPitchR(part.getPitchR());
            }
        }
        stat.setPitchH(part.getPitchH());
        if (part.getPitchDoubles() != null) {
            stat.setPitchDoubles(part.getPitchDoubles());
        }
        if (part.getPitchTriples() != null) {
            stat.setPitchTriples(part.getPitchTriples());
        }
        // 四死合计栏缺栏即保留，与下方拆分栏及 saveLive 同口径。结果页 payload 已保证四栏同送数字
        // （bsball-frontend/tests/gameResultPitchBbHpConsistency387.test.mjs），不会出现「合计被覆写、拆分被保留」的矛盾行。
        if (part.getPitchBbHp() != null) {
            stat.setPitchBbHp(part.getPitchBbHp());
        }
        stat.setPitchSo(part.getPitchSo());
        stat.setPitchHr(part.getPitchHr());
        stat.setPitchInsideParkHr(Integer.valueOf(part.getPitchInsideParkHr() != null ? part.getPitchInsideParkHr() : 0));
        if (part.getPitchSh() != null) {
            stat.setPitchSh(part.getPitchSh());
        }
        if (part.getPitchSf() != null) {
            stat.setPitchSf(part.getPitchSf());
        }
        stat.setPitchPa(part.getPitchPa());
        stat.setPitchBf(part.getPitchBf());
        stat.setPitchAb(part.getPitchAb());
        stat.setNp(part.getNp());
        // 四死球拆分栏（四坏/故意四坏/触身）与 saveLive 同口径：缺栏时保留既有值，
        // 避免旧客户端或部分 payload 把已记录的投手四死静默抹成 NULL→显示 0（issue #387）。
        if (part.getPitchBb() != null) {
            stat.setPitchBb(part.getPitchBb());
        }
        if (part.getPitchIbb() != null) {
            stat.setPitchIbb(part.getPitchIbb());
        }
        if (part.getPitchHbp() != null) {
            stat.setPitchHbp(part.getPitchHbp());
        }
        if (part.getPitchIo() != null) {
            stat.setPitchIo(part.getPitchIo());
        }
        stat.setWp(part.getWp());
        stat.setBk(part.getBk());
        stat.setGo(part.getGo());
        stat.setFo(part.getFo());
        stat.setGs(part.getGs());
        stat.setSvo(part.getSvo());
        stat.setCg(part.getCg());
        stat.setPg(part.getPg());
        stat.setW(part.getW());
        stat.setL(part.getL());
        stat.setSv(part.getSv());
        stat.setHld(part.getHld());
        stat.setPo(part.getPo());
        stat.setA(part.getA());
        stat.setTc(part.getTc());
        stat.setFieldingGs(part.getFieldingGs());
        stat.setDefInn(part.getDefInn());
        stat.setDp(part.getDp());
        stat.setPb(Integer.valueOf(part.getPb() != null ? part.getPb() : 0));
        stat.setCatcherSb(Integer.valueOf(part.getCatcherSb() != null ? part.getCatcherSb() : 0));
        stat.setCatcherCs(Integer.valueOf(part.getCatcherCs() != null ? part.getCatcherCs() : 0));
    }

    private void assertStarterFieldingPositions(List<SaveGameResultDTO.StatPart> stats, String sceneLabel) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        Map<Long, List<SaveGameResultDTO.StatPart>> rowsByTeam = stats.stream().filter(Objects::nonNull).filter(s -> s.getTeamId() != null).collect(Collectors.groupingBy(SaveGameResultDTO.StatPart::getTeamId));
        for (Map.Entry<Long, List<SaveGameResultDTO.StatPart>> entry : rowsByTeam.entrySet()) {
            Long teamId = entry.getKey();
            // 二刀流球员会同时提交「打者行 + 投手行」（打序/守位都在打者行上），
            // 不能把投手行整行剔除，否则 9 人打序会被算成 8 人；这里按 playerId 去重后统计。
            List<SaveGameResultDTO.StatPart> battingOrderRows = entry.getValue().stream()
                    .filter(s -> s.getBattingOrder() != null && s.getBattingOrder() > 0)
                    .sorted(Comparator.comparingInt((SaveGameResultDTO.StatPart s) -> s.getBattingOrder())
                            .thenComparingInt(s -> isPitcherStatPart(s) ? 1 : 0)
                            .thenComparingInt(s -> isBlankPosition(s.getPosition()) ? 1 : 0))
                    .toList();
            // 只提交投手行的球队不参与先发校验，保持原有「投手行不受起手守位校验影响」语义；
            // 一旦出现打者行（即使漏带打序），仍按原来的「不足 9 人」拦截，避免半套 payload 清空已有统计。
            boolean hasBatterRow = entry.getValue().stream().anyMatch(s -> !isPitcherStatPart(s));
            if (!hasBatterRow) {
                continue;
            }
            Map<Long, SaveGameResultDTO.StatPart> startersByPlayer = new LinkedHashMap<>();
            List<SaveGameResultDTO.StatPart> unkeyedRows = new ArrayList<>();
            for (SaveGameResultDTO.StatPart row : battingOrderRows) {
                if (row.getPlayerId() == null) {
                    unkeyedRows.add(row);
                } else {
                    startersByPlayer.putIfAbsent(row.getPlayerId(), row);
                }
            }
            List<SaveGameResultDTO.StatPart> starters = new ArrayList<>(startersByPlayer.values());
            starters.addAll(unkeyedRows);
            starters.sort(Comparator.comparingInt(SaveGameResultDTO.StatPart::getBattingOrder));
            if (starters.size() < 9) {
                throw new BusinessException(400, sceneLabel + "\u5931\u8d25\uff1a\u7403\u961f(" + teamId + ")\u6709\u6253\u5e8f\u7684\u5148\u53d1\u4e0d\u8db3 9 \u4eba\uff0c\u65e0\u6cd5\u6821\u9a8c\u5b88\u5907\u4f4d\u7f6e");
            }
            List<SaveGameResultDTO.StatPart> firstNine = starters.stream().limit(9L).toList();
            List<String> positions = firstNine.stream().map(s -> s.getPosition() == null ? "" : s.getPosition().trim().toUpperCase()).filter(p -> !p.isBlank()).filter(p -> !"PH".equals(p) && !"DH".equals(p) && !"EP".equals(p)).toList();
            LinkedHashSet<String> unique = new LinkedHashSet<>(positions);
            if (unique.size() == positions.size()) continue;
            throw new BusinessException(400, sceneLabel + "\u5931\u8d25\uff1a\u7403\u961f(" + teamId + ")\u6709\u6253\u5e8f\u7684\u5148\u53d1\u5b88\u5907\u4f4d\u7f6e\u91cd\u590d");
        }
    }

    /**
     * P/DH 强校验：同一球队的打击行不可同时出现 DH 与打击投手（P）。
     * 只按 battingOrder>0 判定打击行（isPitcher=1 也可能带打序，如二刀流合并行）；
     * 官方规则 5.11(b) 允许同一球员既列 DH 又列投手，故仅当两侧球员集合不一致时拦截。
     */
    private void assertPitcherDhRule(List<SaveGameResultDTO.StatPart> stats, String sceneLabel) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        Map<Long, List<SaveGameResultDTO.StatPart>> rowsByTeam = stats.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getTeamId() != null)
                .collect(Collectors.groupingBy(SaveGameResultDTO.StatPart::getTeamId));
        for (List<SaveGameResultDTO.StatPart> teamRows : rowsByTeam.values()) {
            Set<Long> dhPlayerIds = new LinkedHashSet<>();
            Set<Long> pitcherPlayerIds = new LinkedHashSet<>();
            for (SaveGameResultDTO.StatPart row : teamRows) {
                if (row.getBattingOrder() == null || row.getBattingOrder() <= 0) {
                    continue;
                }
                String position = row.getPosition() == null ? "" : row.getPosition().trim().toUpperCase();
                if ("DH".equals(position)) {
                    dhPlayerIds.add(row.getPlayerId());
                } else if ("P".equals(position)) {
                    pitcherPlayerIds.add(row.getPlayerId());
                }
            }
            if (dhPlayerIds.isEmpty() || pitcherPlayerIds.isEmpty()) {
                continue;
            }
            // 仅同一名非空球员既列 DH 又列投手时放行；多 DH 或含空球员 id 均按冲突拦截
            if (dhPlayerIds.size() == 1 && dhPlayerIds.equals(pitcherPlayerIds) && !dhPlayerIds.contains(null)) {
                continue;
            }
            throw new BusinessException(400, sceneLabel + "失败：指定打击制下 DH 与打击投手（P）不能同时进入打序");
        }
    }

    private static boolean isPitcherStatPart(SaveGameResultDTO.StatPart part) {
        return part.getIsPitcher() != null && part.getIsPitcher().intValue() == 1;
    }

    private static boolean isBlankPosition(String position) {
        return position == null || position.trim().isBlank();
    }

    private String toInningJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }

    private static void zeroEngineTotals(GamePlayerStat stat) {
        stat.setPitchR(0);
        stat.setEr(0);
        stat.setUnearnedR(0);
        stat.setPendingR(0);
    }

    /** 統計欄位充實度：同 key 重複筆時優先保留資料較完整的列（與 GamePlayerStatService#statRichness 保持一致） */
    private static int statRichness(GamePlayerStat r) {
        int score = 0;
        if (r.getAb() != null) {
            ++score;
        }
        if (r.getR() != null) {
            ++score;
        }
        if (r.getH() != null) {
            ++score;
        }
        if (r.getRbi() != null) {
            ++score;
        }
        if (r.getIp() != null) {
            ++score;
        }
        if (r.getPitchPa() != null) {
            ++score;
        }
        if (r.getNp() != null) {
            ++score;
        }
        if (r.getPitchH() != null) {
            ++score;
        }
        if (r.getPitchSo() != null) {
            ++score;
        }
        if (r.getPitchR() != null) {
            ++score;
        }
        return score;
    }

    private void applyPartToStat(GamePlayerStat stat, GameSaveLiveDTO.GamePlayerStatPart part,
            Long gameId, Long tenantId, boolean preserveEngineTotals) {
        stat.setTenantId(tenantId);
        stat.setGameId(gameId);
        stat.setTeamId(part.getTeamId());
        stat.setPlayerId(part.getPlayerId());
        stat.setBattingOrder(part.getBattingOrder());
        stat.setListOrder(part.getListOrder());
        stat.setPosition(part.getPosition());
        stat.setPa(part.getPa());
        stat.setAb(part.getAb());
        stat.setR(part.getR());
        stat.setH(part.getH());
        stat.setBbHp(part.getBbHp());
        stat.setSb(part.getSb());
        stat.setSo(part.getSo());
        stat.setSoSwing(part.getSoSwing());
        stat.setSoLooking(part.getSoLooking());
        stat.setE(part.getE());
        stat.setRbi(part.getRbi());
        stat.setDoubles(part.getDoubles());
        stat.setTriples(part.getTriples());
        stat.setHr(part.getHr());
        stat.setInsideParkHr(part.getInsideParkHr());
        if (part.getGdp() != null) {
            stat.setGdp(part.getGdp());
        }
        stat.setSh(part.getSh());
        stat.setSf(part.getSf());
        stat.setBb(part.getBb());
        if (part.getIbb() != null) {
            stat.setIbb(part.getIbb());
        }
        stat.setHbp(part.getHbp());
        if (part.getIo() != null) {
            stat.setIo(part.getIo());
        }
        stat.setCs(part.getCs());
        stat.setIsPitcher(Integer.valueOf(part.getIsPitcher() != null ? part.getIsPitcher() : 0));
        stat.setPitcherOrder(part.getPitcherOrder());
        stat.setIp(part.getIp());
        if (preserveEngineTotals) {
            if (stat.getId() == null) {
                zeroEngineTotals(stat);
            }
        } else if (part.getEr() != null) {
            stat.setEr(part.getEr());
        }
        stat.setPitchH(part.getPitchH());
        if (part.getPitchDoubles() != null) {
            stat.setPitchDoubles(part.getPitchDoubles());
        }
        if (part.getPitchTriples() != null) {
            stat.setPitchTriples(part.getPitchTriples());
        }
        // 四死合计栏同样缺栏即保留：拆分栏（pitchBb/pitchIbb/pitchHbp）已有守卫，
        // 合计栏漏守则「只送拆分的载荷」会把四死合计写成 NULL（issue #387）。
        if (part.getPitchBbHp() != null) {
            stat.setPitchBbHp(part.getPitchBbHp());
        }
        stat.setPitchSo(part.getPitchSo());
        stat.setPitchHr(part.getPitchHr());
        stat.setPitchInsideParkHr(part.getPitchInsideParkHr());
        if (part.getPitchSh() != null) {
            stat.setPitchSh(part.getPitchSh());
        }
        if (part.getPitchSf() != null) {
            stat.setPitchSf(part.getPitchSf());
        }
        stat.setPitchPa(part.getPitchPa());
        stat.setPitchBf(part.getPitchBf());
        stat.setNp(part.getNp());
        if (part.getPitchBb() != null) {
            stat.setPitchBb(part.getPitchBb());
        }
        if (part.getPitchIbb() != null) {
            stat.setPitchIbb(part.getPitchIbb());
        }
        if (part.getPitchHbp() != null) {
            stat.setPitchHbp(part.getPitchHbp());
        }
        if (part.getPitchIo() != null) {
            stat.setPitchIo(part.getPitchIo());
        }
        stat.setWp(part.getWp());
        stat.setBk(part.getBk());
        if (part.getGo() != null) {
            stat.setGo(part.getGo());
        }
        if (part.getFo() != null) {
            stat.setFo(part.getFo());
        }
        if (part.getGs() != null) {
            stat.setGs(part.getGs());
        }
        stat.setPo(part.getPo());
        stat.setA(part.getA());
        stat.setTc(part.getTc());
        if (part.getFieldingGs() != null) {
            stat.setFieldingGs(part.getFieldingGs());
        }
        if (part.getDefInn() != null) {
            stat.setDefInn(part.getDefInn());
        }
        if (part.getDp() != null) {
            stat.setDp(part.getDp());
        }
        if (part.getPb() != null) {
            stat.setPb(part.getPb());
        }
        if (part.getCatcherSb() != null) {
            stat.setCatcherSb(part.getCatcherSb());
        }
        if (part.getCatcherCs() != null) {
            stat.setCatcherCs(part.getCatcherCs());
        }
    }

    private void assertGameWritable(Game game) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(game.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u6bd4\u8d5b");
        }
        if (game.getDeletedAt() != null) {
            throw new BusinessException(400, "\u6bd4\u8d5b\u5df2\u5220\u9664");
        }
        if (game.getEventId() == null) {
            throw new BusinessException(400, "\u6bd4\u8d5b\u672a\u5173\u8054\u8d5b\u4e8b");
        }
        Event ev = this.eventRepository.findById(game.getEventId()).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u6bd4\u8d5b");
        }
        this.resourceGuard.assertCanManageGame(game.getId());
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"gameday", "gameTime", "id"}));
    }

    @Generated
    public GameService(GameRepository gameRepository, GamePlayerStatRepository gamePlayerStatRepository, GameSubstitutionEventRepository gameSubstitutionEventRepository, EventRepository eventRepository, StadiumRepository stadiumRepository, AccountScopeService accountScopeService, ScopeQuerySupport scopeQuerySupport, ResourceGuard resourceGuard, TenantQueryPolicyService tenantQueryPolicyService, EarnedRunReconstructionService earnedRunReconstructionService, EventRegistrationService eventRegistrationService) {
        this.gameRepository = gameRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.gameSubstitutionEventRepository = gameSubstitutionEventRepository;
        this.eventRepository = eventRepository;
        this.stadiumRepository = stadiumRepository;
        this.accountScopeService = accountScopeService;
        this.scopeQuerySupport = scopeQuerySupport;
        this.resourceGuard = resourceGuard;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.earnedRunReconstructionService = earnedRunReconstructionService;
        this.eventRegistrationService = eventRegistrationService;
    }
}

