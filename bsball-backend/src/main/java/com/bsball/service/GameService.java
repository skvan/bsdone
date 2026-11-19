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
import com.bsball.model.dto.EarnedRunDecisionAdjustRequest;
import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.dto.EarnedRunDecisionOverrideRequest;
import com.bsball.model.dto.EarnedRunHalfInningRequest;
import com.bsball.model.dto.FieldingAdjustmentCandidateDTO;
import com.bsball.model.dto.FieldingAdjustmentCreateRequest;
import com.bsball.model.dto.FieldingAdjustmentListDTO;
import com.bsball.model.dto.GameRevisionAuditDTO;
import com.bsball.model.dto.IdentityTransferPreviewDTO;
import com.bsball.model.dto.IdentityTransferRequest;
import com.bsball.model.dto.IdentityTransferResultDTO;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.dto.PitcherRunOverrideRequest;
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.PitcherTransferRequest;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.dto.GameStatAdjustmentDTO;
import com.bsball.model.dto.StatBaseCompareDTO;
import com.bsball.model.dto.StatRowDeleteCheckDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.GameSubstitutionEvent;
import com.bsball.model.entity.Stadium;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSnapshotRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.repository.StadiumRepository;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.service.query.ScopeQuerySupport;
import com.bsball.utils.GamePlayerStatZeros;
import com.bsball.utils.GameStatDeleteDiagnostics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bsball.utils.StatsFormatUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GameService {
    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ALLOWED_SUBSTITUTION_TYPES = Set.of("SUBSTITUTION", "POSITION_CHANGE", "PITCHING_CHANGE", "DH_CANCEL");
    private static final Set<String> BRACKET_STAGE_CODES = Set.of("R16", "QF", "SF", "FINAL", "THIRD_PLACE");
    private static final String STAGE_GROUP = "GROUP";
    private static final String FORMAT_SINGLE_ROUND_ROBIN = "SINGLE_ROUND_ROBIN";
    /** 结果页逐位置守备允许的棒球守位（不含 DH/PH/EP）。 */
    private static final Set<String> BASEBALL_FIELDING_POSITIONS = Set.of(
            "P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF");
    /** 结果页逐位置守备允许的垒球守位（额外含 XF 系列）。 */
    private static final Set<String> SOFTBALL_FIELDING_POSITIONS = Set.of(
            "P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "XF", "XF1", "XF2");
    /** 球员级守备合计字段：任一变化都意味着逐位置明细需要跟随。 */
    private static final Set<String> FIELDING_TOTAL_KEYS = Set.of(
            "po", "a", "e", "dp", "pb", "catcherSb", "catcherCs", "defInn", "fieldingGs");
    private final GameRepository gameRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final GameSubstitutionEventRepository gameSubstitutionEventRepository;
    private final GameFieldingPosStatRepository gameFieldingPosStatRepository;
    private final EventRepository eventRepository;
    private final StadiumRepository stadiumRepository;
    private final AccountScopeService accountScopeService;
    private final ScopeQuerySupport scopeQuerySupport;
    private final ResourceGuard resourceGuard;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final EarnedRunReconstructionService earnedRunReconstructionService;
    private final GameFieldingAdjustmentService fieldingAdjustmentService;
    private final GameRevisionAuditService revisionAuditService;
    private final GameSnapshotRepository gameSnapshotRepository;
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
        // get() 负责存在性、租户与可见范围校验；快照正文按需单独读取，不随 Game 实体加载（Issue #686）。
        if (this.get(gameId) == null) {
            return null;
        }
        return this.gameSnapshotRepository.findSnapshotJsonById(gameId).orElse(null);
    }

    @Transactional
    public void saveLiveSnapshot(Long gameId, String json) {
        Game g = this.gameRepository.findById(gameId).orElse(null);
        if (g == null) {
            return;
        }
        this.assertGameWritable(g);
        if (this.gameSnapshotRepository.updateSnapshotJson(
                gameId, normalizeSnapshotJson(json), CurrentUserHolder.get()) == 0) {
            log.warn("快照更新未命中比赛 id={}", gameId);
        }
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
        if (this.gameSnapshotRepository.updateSnapshotJson(
                gameId, normalizeSnapshotJson(json), CurrentUserHolder.get()) == 0) {
            log.warn("阵容快照更新未命中比赛 id={}", gameId);
        }
    }

    /** 快照空白值统一存 NULL，配合 Game.hasLiveSnapshot 的“非空即存在”口径（Issue #686）。 */
    private static String normalizeSnapshotJson(String json) {
        return json == null || json.isBlank() ? null : json;
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
        this.normalizeTournamentFields(entity, ev);
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
        if (entity.getStageCode() == null) {
            entity.setStageCode(existing.getStageCode());
        }
        if (entity.getGroupCode() == null) {
            entity.setGroupCode(existing.getGroupCode());
        }
        if (entity.getRoundNo() == null) {
            entity.setRoundNo(existing.getRoundNo());
        }
        if (entity.getBracketSlot() == null) {
            entity.setBracketSlot(existing.getBracketSlot());
        }
        this.normalizeTournamentFields(entity, ev);
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(entity.getStadiumId(), tid);
        }
        return (Game)this.gameRepository.save(entity);
    }

    /**
     * 比赛赛制阶段字段归一化与校验（Issue #665）：
     * 单/双循环不使用阶段字段；单败淘汰只允许淘汰赛阶段；小组+淘汰赛区分 GROUP 与淘汰赛阶段。
     */
    private void normalizeTournamentFields(Game game, Event event) {
        String format = event.getFormatType() == null
                ? FORMAT_SINGLE_ROUND_ROBIN
                : event.getFormatType().trim().toUpperCase(Locale.ROOT);
        String stage = this.normalizeStageCode(game.getStageCode());
        String group = this.normalizeGroupCode(game.getGroupCode());
        if (stage != null && !STAGE_GROUP.equals(stage) && !BRACKET_STAGE_CODES.contains(stage)) {
            throw new BusinessException(400, "未知的赛制阶段：" + game.getStageCode());
        }
        boolean bracketStage = stage != null && BRACKET_STAGE_CODES.contains(stage);
        if ("KNOCKOUT".equals(format)) {
            if (STAGE_GROUP.equals(stage)) {
                throw new BusinessException(400, "单败淘汰赛事不能设置小组赛阶段");
            }
            group = null;
        } else if ("GROUP_KNOCKOUT".equals(format)) {
            if (bracketStage && group != null) {
                throw new BusinessException(400, "淘汰赛阶段不能设置小组组别");
            }
        } else {
            stage = null;
            group = null;
        }
        if (game.getRoundNo() != null && game.getRoundNo() < 1) {
            throw new BusinessException(400, "轮次必须大于 0");
        }
        if (game.getBracketSlot() != null && game.getBracketSlot() < 1) {
            throw new BusinessException(400, "对阵位必须大于 0");
        }
        game.setStageCode(stage);
        game.setGroupCode(group);
    }

    private String normalizeStageCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 32) {
            throw new BusinessException(400, "赛制阶段不能超过 32 个字符");
        }
        return normalized;
    }

    private String normalizeGroupCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 16) {
            throw new BusinessException(400, "组别不能超过 16 个字符");
        }
        return normalized;
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
            GameSaveLiveDTO.GameUpdatePart g = dto.getGame();            if (g.getHomeScore() != null) {
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
        // 已完赛保存前先复核源 Play；失效补记在统计覆盖前扣回，避免 save-live 重放已失效记录。
        if ("final".equalsIgnoreCase(game.getStatus())) {
            this.fieldingAdjustmentService.refreshApprovedValidity(game);
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
        this.saveFieldingPositions(game, dto.getFieldingPositions());
        // 实时录入以快照为基础绝对覆盖统计；赛后补记在覆盖后重放，避免保存把 A/TC 冲掉。
        // 只重放本次请求实际写入的统计行，部分载荷遗漏的行保留原值，避免重复 +1。
        // Do() 无快照重建时统计取自 /stats（已含补记），前端会标 statsIncludeFieldingAdjustments=true，跳过回放。
        if (dto.getStats() != null && !dto.getStats().isEmpty()
                && !Boolean.TRUE.equals(dto.getStatsIncludeFieldingAdjustments())) {
            Set<String> savedStatKeys = new HashSet<>();
            for (GameSaveLiveDTO.GamePlayerStatPart part : dto.getStats()) {
                if (part == null || part.getTeamId() == null || part.getPlayerId() == null) {
                    continue;
                }
                savedStatKeys.add(part.getTeamId() + "_" + part.getPlayerId() + "_"
                        + (part.getIsPitcher() == null ? 0 : part.getIsPitcher()));
            }
            this.fieldingAdjustmentService.reapplyPlayerStats(game, savedStatKeys);
        }
        if (dto.getFieldingPositions() != null) {
            this.fieldingAdjustmentService.reapplyPositionStats(game);
        }
        if ("final".equalsIgnoreCase(game.getStatus())) {
            this.revisionAuditService.captureBase(game, CurrentUserHolder.get(), "LIVE");
            this.logSettlementValidation(gameId, game.getTenantId());
        }
    }

    /** §11 结算验证：非阻挡，仅记录警告供人工复核；无引擎数据时跳过。 */
    private void logSettlementValidation(Long gameId, Long tenantId) {
        if (gameId == null || tenantId == null) {
            return;
        }
        try {
            if (!this.earnedRunReconstructionService.hasEngineData(gameId)) {
                return;
            }
            for (String violation : this.earnedRunReconstructionService.validateSettlement(tenantId, gameId)) {
                log.warn("[settlement-validation] gameId={} tenant={}: {}", gameId, tenantId, violation);
            }
        } catch (RuntimeException ex) {
            log.warn("[settlement-validation] gameId={} tenant={} failed: {}",
                    gameId, tenantId, ex.getMessage());
        }
    }

    /** 逐位置守备：请求带 fieldingPositions 时视为该场全量替换，并回写球员级 def_inn。 */
    private void saveFieldingPositions(Game game, List<GameSaveLiveDTO.FieldingPositionPart> parts) {
        if (parts == null) {
            return;
        }
        if (parts.size() > 200) {
            throw new BusinessException(400, "逐位置守备数据过多");
        }
        List<GameFieldingPosStat> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        List<EventRegistrationService.StatsRef> refs = new ArrayList<>();
        for (GameSaveLiveDTO.FieldingPositionPart part : parts) {
            if (part == null) continue;
            GameFieldingPosStat row = toFieldingPosStat(game, part, keys);
            rows.add(row);
            refs.add(new EventRegistrationService.StatsRef(row.getTeamId(), row.getPlayerId()));
        }
        if (!refs.isEmpty()) {
            this.eventRegistrationService.assertStatsPartsInRoster(game.getEventId(), game.getId(), refs);
        }
        this.gameFieldingPosStatRepository.deleteByGameId(game.getId());
        this.gameFieldingPosStatRepository.flush();
        if (rows.isEmpty()) {
            return;
        }
        this.gameFieldingPosStatRepository.saveAll(rows);
        this.applyFieldingInningsTotals(game.getId(), rows);
    }

    /** 校验单行并转实体；同队同球员同守位只允许一行。 */
    private static GameFieldingPosStat toFieldingPosStat(Game game, GameSaveLiveDTO.FieldingPositionPart part, Set<String> keys) {
        Long teamId = part.getTeamId();
        Long playerId = part.getPlayerId();
        if (!Objects.equals(teamId, game.getHomeTeamId()) && !Objects.equals(teamId, game.getAwayTeamId())) {
            throw new BusinessException(400, "守备球队不属于本场比赛");
        }
        if (playerId == null || playerId <= 0) {
            throw new BusinessException(400, "逐位置守备缺少球员");
        }
        String position = normalizePosition(part.getPosition());
        if (position == null || "DH".equals(position) || "PH".equals(position) || "EP".equals(position)) {
            throw new BusinessException(400, "逐位置守备位置无效：" + part.getPosition());
        }
        Integer outs = part.getOuts();
        if (outs == null || outs < 0 || outs > 999) {
            throw new BusinessException(400, "逐位置守备出局数无效");
        }
        if (!keys.add(teamId + "_" + playerId + "_" + position)) {
            throw new BusinessException(400, "逐位置守备存在重复行");
        }
        GameFieldingPosStat row = new GameFieldingPosStat();
        row.setTenantId(game.getTenantId());
        row.setGameId(game.getId());
        row.setTeamId(teamId);
        row.setPlayerId(playerId);
        row.setPosition(position);
        row.setOuts(outs);
        row.setPo(normalizeFieldingCredit(part.getPo(), "接杀数"));
        row.setA(normalizeFieldingCredit(part.getA(), "助杀数"));
        row.setE(normalizeFieldingCredit(part.getE(), "失误数"));
        row.setDp(normalizeFieldingCredit(part.getDp(), "双杀数"));
        row.setPb(normalizeFieldingCredit(part.getPb(), "捕逸数"));
        row.setCatcherCs(normalizeFieldingCredit(part.getCatcherCs(), "盗垒阻杀数"));
        row.setCatcherSb(normalizeFieldingCredit(part.getCatcherSb(), "被盗垒数"));
        row.setFieldingGs(Boolean.TRUE.equals(part.getFieldingGs()));
        row.setSource("LIVE");
        return row;
    }

    private static int normalizeFieldingCredit(Integer value, String label) {
        if (value == null) {
            return 0;
        }
        if (value < 0 || value > 999) {
            throw new BusinessException(400, "逐位置守备" + label + "无效");
        }
        return value;
    }

    /** 由逐位置出局数汇总回写球员级 def_inn（10 outs → 3.1）。 */
    private void applyFieldingInningsTotals(Long gameId, List<GameFieldingPosStat> rows) {
        Map<String, Integer> totals = new HashMap<>();
        for (GameFieldingPosStat row : rows) {
            totals.merge(row.getTeamId() + "_" + row.getPlayerId(), row.getOuts(), Integer::sum);
        }
        for (GamePlayerStat stat : this.gamePlayerStatRepository.findByGameId(gameId)) {
            Integer outs = totals.get(stat.getTeamId() + "_" + stat.getPlayerId());
            if (outs == null) continue;
            stat.setDefInn((outs / 3) + (outs % 3) / 10.0);
            this.gamePlayerStatRepository.save(stat);
        }
    }

    /** 比赛维度逐位置守备数据（读取沿用比赛数据范围校验）。 */
    public List<GameFieldingPosStat> listFieldingPositions(Long gameId) {
        if (this.get(gameId) == null) {
            throw new BusinessException(403, "无权查看该比赛");
        }
        return this.gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId);
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

    /** 首个守位只保留实际守备位；DH/PH/EP 等打击专用身份视为未上场防守。 */
    private static String normalizeFirstPosition(String value) {
        String pos = normalizePosition(value);
        if (pos == null || "DH".equals(pos) || "PH".equals(pos) || "EP".equals(pos)) {
            return null;
        }
        return pos;
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
    public List<EarnedRunDecisionListDTO> listEarnedRunDecisions(Long gameId) {
        Game game = this.get(gameId);
        if (game == null) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u6bd4\u8d5b");
        }
        return this.earnedRunReconstructionService.listDecisions(game.getTenantId(), gameId);
    }

    /** 赛后复核：把一条 ER 判定改派给同队另一名投手。 */
    public EarnedRunDecisionListDTO reassignEarnedRunDecision(
            Long gameId, Long decisionId, EarnedRunDecisionAdjustRequest request) {
        if (request == null || request.getTargetPitcherId() == null) {
            throw new BusinessException(400, "请选择改派目标投手");
        }
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能改派判定");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            EarnedRunDecisionListDTO result = this.earnedRunReconstructionService.reassignDecision(
                    game.getTenantId(), gameId, decisionId,
                    request.getTargetPitcherId(), request.getReason(), operatorId);
            if (result != null) {
                recordLedger(game, "ER_DECISION", "REASSIGN", "DECISION_ADJUSTMENT",
                        result.adjustmentId(), null, result.effectivePitcherId(), 1,
                        "responsiblePitcherId", "责任投手",
                        String.valueOf(result.responsiblePitcherId()),
                        String.valueOf(result.effectivePitcherId()),
                        "ACTIVE", result.adjustmentReason(), operatorId);
            }
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(400, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(409, "该判定已有其他调整，请刷新后重试");
        }
    }

    /** 赛后复核：作废一条误录 ER 判定，原始判定保留。 */
    public EarnedRunDecisionListDTO voidEarnedRunDecision(
            Long gameId, Long decisionId, EarnedRunDecisionAdjustRequest request) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能作废判定");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            EarnedRunDecisionListDTO result = this.earnedRunReconstructionService.voidDecision(
                    game.getTenantId(), gameId, decisionId,
                    request == null ? null : request.getReason(), operatorId);
            if (result != null) {
                recordLedger(game, "ER_DECISION", "VOID", "DECISION_ADJUSTMENT",
                        result.adjustmentId(), null, result.responsiblePitcherId(), 1,
                        "responsiblePitcherId", "责任投手",
                        String.valueOf(result.responsiblePitcherId()), "—",
                        "ACTIVE", result.adjustmentReason(), operatorId);
            }
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(400, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(409, "该判定已有其他调整，请刷新后重试");
        }
    }

    /** 赛后复核：撤销最近一次判定改派/作废。 */
    public EarnedRunDecisionListDTO revokeEarnedRunDecisionAdjustment(
            Long gameId, Long adjustmentId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能撤销判定调整");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            EarnedRunDecisionListDTO result = this.earnedRunReconstructionService
                    .revokeDecisionAdjustment(game.getTenantId(), gameId, adjustmentId, operatorId);
            if (result != null) {
                Long effectivePitcherId = result.effectivePitcherId() != null
                        ? result.effectivePitcherId() : result.responsiblePitcherId();
                recordLedger(game, "ER_DECISION", "REVOKE", "DECISION_ADJUSTMENT",
                        adjustmentId, null, effectivePitcherId, 1,
                        "responsiblePitcherId", "责任投手", "—",
                        String.valueOf(effectivePitcherId), "REVOKED", null, operatorId);
            }
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(400, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(409, "该判定已有其他调整，请刷新后重试");
        }
    }

    private void recordLedger(Game game, String scope, String action, String sourceType,
            Long sourceId, Long teamId, Long playerId, Integer isPitcher,
            String fieldKey, String fieldLabel, String beforeValue, String afterValue,
            String status, String reason, Long operatorId) {
        this.revisionAuditService.recordAdjustment(
                game.getTenantId(), game.getId(), scope, action, sourceType, sourceId,
                teamId, playerId, isPitcher, fieldKey, fieldLabel,
                beforeValue, afterValue, status, reason, operatorId);
    }

    /** 赛后守备补记：列出源 Play 候选与已补记记录，仅已完赛比赛可用。 */
    public List<FieldingAdjustmentListDTO> listFieldingAdjustments(Long gameId) {
        Game game = this.requireGameForFieldingReview(gameId);
        return this.fieldingAdjustmentService.list(game);
    }

    /** 赛后守备补记候选：失误 Play 与守备方可补记球员。 */
    public List<FieldingAdjustmentCandidateDTO> listFieldingAdjustmentCandidates(Long gameId) {
        Game game = this.requireGameForFieldingReview(gameId);
        return this.fieldingAdjustmentService.listCandidates(game);
    }

    /** 统一修订记录：聚合 ER 判定调整、R/ER 覆写、守备补记与投手行转记。 */
    public List<GameRevisionAuditDTO> listRevisionAudit(Long gameId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能查看修订记录");
        return this.revisionAuditService.list(game);
    }

    /** 基座与当前有效值对比；历史场次无基座返回 legacy=true。 */
    public StatBaseCompareDTO compareStatBase(Long gameId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能查看基座对比");
        return this.revisionAuditService.compareBase(game);
    }

    /** 统一修订台账：结果页字段级人工修改。 */
    public List<GameStatAdjustmentDTO> listStatAdjustments(Long gameId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能查看修订台账");
        return this.revisionAuditService.listAdjustments(game);
    }

    /** 删除统计行前诊断：返回剩余字段、有效 ER 判定、守备补记与守备位置。 */
    public StatRowDeleteCheckDTO checkStatRowDelete(Long gameId, Long statId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能检查删除条件");
        Long tenantId = game.getTenantId();
        GamePlayerStat stat = this.gamePlayerStatRepository.findById(statId)
                .filter(row -> Objects.equals(row.getGameId(), gameId)
                        && Objects.equals(row.getTenantId(), tenantId))
                .orElseThrow(() -> new BusinessException(404, "统计行不存在"));
        long playerId = stat.getPlayerId() == null ? 0L : stat.getPlayerId();
        int decisions = this.earnedRunReconstructionService
                .countResponsiblePitcherDecisions(gameId, playerId);
        long adjustments = this.fieldingAdjustmentService
                .countApprovedPlayerAdjustments(tenantId, gameId, playerId);
        long positions = this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId).stream()
                .filter(row -> Objects.equals(row.getTeamId(), stat.getTeamId())
                        && Objects.equals(row.getPlayerId(), stat.getPlayerId()))
                .count();
        return GameStatDeleteDiagnostics.build(stat, decisions, adjustments, positions);
    }

    /** 赛后补记助杀：源 Play 与守备统计同步由服务层校验。 */
    public FieldingAdjustmentListDTO createFieldingAdjustment(
            Long gameId, FieldingAdjustmentCreateRequest request) {
        Game game = this.requireGameForFieldingReview(gameId);
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        FieldingAdjustmentListDTO result =
                this.fieldingAdjustmentService.create(game, operatorId, request);
        if (result != null) {
            recordLedger(game, "FIELDING_A", "APPROVE", "FIELDING_ADJUSTMENT",
                    result.adjustmentId(), result.teamId(), result.playerId(), null,
                    "a", "助杀 A", "0", String.valueOf(result.assistCount()),
                    "APPROVED", result.note(), operatorId);
        }
        return result;
    }

    /** 撤销赛后补记：保留审计行并即时扣回 A/TC。 */
    public FieldingAdjustmentListDTO revokeFieldingAdjustment(
            Long gameId, Long adjustmentId) {
        Game game = this.requireGameForFieldingReview(gameId);
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        FieldingAdjustmentListDTO result =
                this.fieldingAdjustmentService.revoke(game, adjustmentId, operatorId);
        if (result != null) {
            recordLedger(game, "FIELDING_A", "REVOKE", "FIELDING_ADJUSTMENT",
                    result.adjustmentId(), result.teamId(), result.playerId(), null,
                    "a", "助杀 A", String.valueOf(result.assistCount()), "0",
                    "REVOKED", result.note(), operatorId);
        }
        return result;
    }

    private Game requireGameForFieldingReview(Long gameId) {
        return this.requireFinalGameForReview(gameId, "比赛结束后才能进行守备补记");
    }

    private Game requireFinalGameForReview(Long gameId, String message) {
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            throw new BusinessException(404, "比赛不存在");
        }
        this.assertGameWritable(game);
        if (!"final".equalsIgnoreCase(game.getStatus())) {
            throw new BusinessException(400, message);
        }
        return game;
    }

    /** 赛后复核：列出本场投手 R/ER 合计与生效中的人工覆写。 */
    public List<PitcherRunTotalsDTO> listPitcherRunTotals(Long gameId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能复核投手 R/ER");
        if (!this.earnedRunReconstructionService.hasEngineData(gameId)) {
            return List.of();
        }
        return this.earnedRunReconstructionService.listPitcherTotals(game.getTenantId(), gameId);
    }

    /** 赛后复核：保存指定投手的 R/ER 人工覆写。 */
    public PitcherRunTotalsDTO overridePitcherRuns(
            Long gameId, Long playerId, PitcherRunOverrideRequest request) {
        if (request == null) {
            throw new BusinessException(400, "缺少投手 R/ER 覆写资料");
        }
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能手工覆写投手 R/ER");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            PitcherRunTotalsDTO result = this.earnedRunReconstructionService.overridePitcherRuns(
                    game.getTenantId(), gameId, playerId,
                    request.getRunsAllowed(), request.getEarnedRuns(),
                    request.getNote(), operatorId);
            if (result != null) {
                recordLedger(game, "PITCHER_RUN_OVERRIDE", "OVERRIDE", "PITCHER_RUN_OVERRIDE",
                        result.overrideId(), result.teamId(), playerId, 1,
                        "pitchR/er", "R/ER",
                        result.engineRunsAllowed() + "/" + result.engineEarnedRuns(),
                        result.runsAllowed() + "/" + result.earnedRuns(),
                        "APPROVED", request.getNote(), operatorId);
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(409, "该投手已有其他复核结果，请刷新后重试");
        }
    }

    /** 赛后复核：撤销投手 R/ER 人工覆写，恢复引擎值。 */
    public PitcherRunTotalsDTO revokePitcherRunOverride(Long gameId, Long overrideId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能撤销投手 R/ER 覆写");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            PitcherRunTotalsDTO result = this.earnedRunReconstructionService
                    .revokePitcherOverride(game.getTenantId(), gameId, overrideId, operatorId);
            if (result != null) {
                recordLedger(game, "PITCHER_RUN_OVERRIDE", "REVOKE", "PITCHER_RUN_OVERRIDE",
                        overrideId, result.teamId(), result.playerId(), 1,
                        "pitchR/er", "R/ER", "人工覆写",
                        result.engineRunsAllowed() + "/" + result.engineEarnedRuns(),
                        "REVOKED", null, operatorId);
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    /** 赛后补录：投手行转记（只转记，不删除源行）。 */
    public PitcherTransferResultDTO transferPitcher(Long gameId, PitcherTransferRequest request) {
        if (request == null || request.getSourceStatId() == null
                || request.getTargetPlayerId() == null) {
            throw new BusinessException(400, "缺少转记源行或目标投手");
        }
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能转记投手数据");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            PitcherTransferResultDTO result = this.earnedRunReconstructionService.transferPitcher(
                    game.getTenantId(), gameId, request.getSourceStatId(),
                    request.getTargetPlayerId(), request.getNote(), operatorId);
            if (result != null) {
                recordLedger(game, "PITCHER_TRANSFER", "TRANSFER", "PITCHER_TRANSFER",
                        result.transferId(), null, result.targetPlayerId(), 1,
                        "pitcherRow", "投手行",
                        String.valueOf(result.sourcePlayerId()),
                        String.valueOf(result.targetPlayerId()),
                        "TRANSFERRED", request.getNote(), operatorId);
            }
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    /** 整名球员身份转记预览。 */
    public IdentityTransferPreviewDTO previewIdentityTransfer(
            Long gameId, Long sourcePlayerId, Long targetPlayerId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能进行身份转记");
        if (sourcePlayerId == null || targetPlayerId == null) {
            throw new BusinessException(400, "缺少源球员或目标球员");
        }
        try {
            return this.earnedRunReconstructionService.previewIdentityTransfer(
                    game.getTenantId(), gameId, sourcePlayerId, targetPlayerId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    /** 整名球员身份转记；转记成功后再单独确认删除源数据。 */
    public IdentityTransferResultDTO transferIdentity(
            Long gameId, IdentityTransferRequest request) {
        if (request == null || request.getSourcePlayerId() == null
                || request.getTargetPlayerId() == null) {
            throw new BusinessException(400, "缺少源球员或目标球员");
        }
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能进行身份转记");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            IdentityTransferResultDTO result = this.earnedRunReconstructionService
                    .transferIdentity(game.getTenantId(), gameId,
                            request.getSourcePlayerId(), request.getTargetPlayerId(),
                            request.getScopes(), request.getNote(), operatorId);
            recordLedger(game, "IDENTITY_TRANSFER", "TRANSFER", "IDENTITY_TRANSFER",
                    result.transferId(), null, result.targetPlayerId(), null,
                    "identity", "球员身份", String.valueOf(result.sourcePlayerId()),
                    String.valueOf(result.targetPlayerId()), "TRANSFERRED",
                    request.getNote(), operatorId);
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(400, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(409, "该转记与其他操作冲突，请刷新后重试");
        }
    }

    /** 身份转记成功后单独删除源数据。 */
    public void deleteIdentityTransferSource(Long gameId, Long transferId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能删除身份转记源数据");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            IdentityTransferResultDTO result = this.earnedRunReconstructionService
                    .deleteIdentityTransferSource(
                            game.getTenantId(), gameId, transferId, operatorId);
            recordLedger(game, "IDENTITY_TRANSFER", "DELETE_SOURCE", "IDENTITY_TRANSFER",
                    transferId, null, result.targetPlayerId(), null,
                    "identity", "球员身份",
                    String.valueOf(result.sourcePlayerId()), "—",
                    "DELETED", null, operatorId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    /** 转记成功后单独删除源行；必须已全零且无自责分判定。 */
    public void deletePitcherTransferSource(Long gameId, Long transferId) {
        Game game = this.requireFinalGameForReview(gameId, "比赛结束后才能删除转记源行");
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            PitcherTransferResultDTO result = this.earnedRunReconstructionService
                    .deletePitcherTransferSource(game.getTenantId(), gameId, transferId, operatorId);
            if (result != null) {
                recordLedger(game, "PITCHER_TRANSFER", "DELETE_SOURCE", "PITCHER_TRANSFER",
                        result.transferId(), null, result.targetPlayerId(), 1,
                        "pitcherRow", "投手行",
                        String.valueOf(result.sourcePlayerId()), "—",
                        "DELETED", null, operatorId);
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    @Transactional(rollbackFor={Exception.class})
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
        // 导入创建的是全新比赛，不可能有 ER 引擎判定可依恃（必然属于 legacy），
        // 因此这里不重算、只做分解自洽把关：对不上就不允许进表。
        this.assertImportedPitcherTotalsCoherent(dto.getStats());
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
            if (g.getStageCode() != null) {
                game.setStageCode(g.getStageCode());
            }
            if (g.getGroupCode() != null) {
                game.setGroupCode(g.getGroupCode());
            }
            if (g.getRoundNo() != null) {
                game.setRoundNo(g.getRoundNo());
            }
            if (g.getBracketSlot() != null) {
                game.setBracketSlot(g.getBracketSlot());
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
                this.normalizeTournamentFields(game, evSync);
            }
            this.gameRepository.save(game);
        }
        Long statTenantId = game.getTenantId();
        // 本轮 stats 写入里守备合计发生变化的球员：旧数据（无逐位置行）据此补行（Issue #704 兼容）。
        Set<String> fieldingChangedKeys = new LinkedHashSet<>();
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
            List<SaveGameResultDTO.StatPart> manualPitcherRunParts = new ArrayList<>();
            List<GameRevisionAuditService.StatChange> ledgerChanges = new ArrayList<>();
            for (SaveGameResultDTO.StatPart part : dto.getStats()) {
                GamePlayerStat stat;
                if (part.getTeamId() == null || part.getPlayerId() == null) continue;
                int isPitcher = part.getIsPitcher() == null ? 0 : part.getIsPitcher().intValue();
                if (engineManaged && isPitcher == 1
                        && Boolean.TRUE.equals(part.getManualPitcherRunOverride())) {
                    manualPitcherRunParts.add(part);
                }
                if (part.getId() != null) {
                    GamePlayerStat byId = existingById.get(part.getId());
                    // id 指向的列必須與該筆身分一致才認，否則視為前端 id 錯位（例如投手筆帶了打者列 id），
                    // 改由複合鍵決定落點，避免把既有列的 isPitcher 就地翻轉。
                    if (byId != null && isPitcher == (byId.getIsPitcher() == null ? 0 : byId.getIsPitcher().intValue())) {
                        stat = byId;
                        Map<String, Double> before = statValueMap(stat);
                        this.applyResultPartToStat(stat, part, gameId, statTenantId, engineManaged);
                        if (fieldingTotalsChanged(before, stat)) {
                            fieldingChangedKeys.add(resultStatPlayerKey(part.getTeamId(), part.getPlayerId()));
                        }
                        this.gamePlayerStatRepository.save(stat);
                        addStatChanges(ledgerChanges, stat, before);
                        keepIds.add(stat.getId());
                        continue;
                    }
                }
                String key = resultStatKey(part.getTeamId(), part.getPlayerId(), Integer.valueOf(isPitcher));
                stat = existingByKey.get(key);
                Map<String, Double> before = stat == null ? Map.of() : statValueMap(stat);
                if (stat == null) {
                    stat = new GamePlayerStat();
                }
                this.applyResultPartToStat(stat, part, gameId, statTenantId, engineManaged);
                if (fieldingTotalsChanged(before, stat)) {
                    fieldingChangedKeys.add(resultStatPlayerKey(part.getTeamId(), part.getPlayerId()));
                }
                this.gamePlayerStatRepository.save(stat);
                addStatChanges(ledgerChanges, stat, before);
                keepIds.add(stat.getId());
                existingByKey.put(key, stat);
            }
            this.revisionAuditService.recordStatChanges(
                    game, ledgerChanges, CurrentUserHolder.get());
            // 删除改为显式名单：只有 removedStatIds 里列出的既有行才删除。
            // payload 中没出现但未显式删除的行一律保留，避免旧页面/多标签页漏带
            // 把真实打者行一并物理删除。
            if (dto.getRemovedStatIds() != null) {
                this.deleteRemovedStatRows(gameId, statTenantId, engineManaged, dto.getRemovedStatIds(),
                        existingById, keepIds);
            }
            this.applyResultPitcherRunOverrides(game, manualPitcherRunParts);
        }
        // 逐位置整体替换必须晚于 stats 写入：新行与球员合计以逐位置表单为准。
        // 放在 stats 分支外，stats 缺失时 final 门禁照常生效；随后的 #704 差额回写会跳过这些球员。
        Set<String> replacedFieldingKeys =
                this.applyFieldingPlayerParts(game, dto.getFieldingPlayerParts());
        // Issue #701：结果页守备表显示的是逐位置合计，人工改写后必须同步回逐位置明细，
        // 否则刷新再保存会用旧合计把球员级人工值回滚。
        // 本次已提交逐位置行的球员跳过回写（合计来自逐位置行）；其余球员按差额回写，
        // 旧数据（无逐位置行）仅在本轮守备合计有变化时按当前守位补一行。
        this.syncResultFieldingPositions(
                gameId, dto.getStats(), replacedFieldingKeys, fieldingChangedKeys);
        // saveResult 的统计行来自结果页 /stats 读取（已含赛后补记），此处不再回放，避免重复 +1。
        // 首次 final 保存写入基座快照；已有基座不覆盖。
        this.revisionAuditService.captureBase(game, CurrentUserHolder.get(), "RESULT");
        // §11 結算驗證（非阻擋，僅記錄警告供人工複核）
        this.logSettlementValidation(gameId, statTenantId);
    }

    private static Map<String, Double> statValueMap(GamePlayerStat stat) {
        Map<String, Double> values = new HashMap<>();
        for (GamePlayerStatZeros.FieldValue value : GamePlayerStatZeros.values(stat)) {
            values.put(value.key(), value.value());
        }
        return values;
    }

    private static void addStatChanges(List<GameRevisionAuditService.StatChange> changes,
            GamePlayerStat stat, Map<String, Double> before) {
        if (stat == null || stat.getPlayerId() == null) {
            return;
        }
        for (GamePlayerStatZeros.FieldValue after : GamePlayerStatZeros.values(stat)) {
            double beforeValue = before.getOrDefault(after.key(), 0d);
            if (Double.compare(beforeValue, after.value()) == 0) {
                continue;
            }
            changes.add(new GameRevisionAuditService.StatChange(
                    stat.getTeamId(), stat.getPlayerId(), stat.getIsPitcher(),
                    stat.getId(), after.key(), after.label(), beforeValue, after.value()));
        }
    }

    /** 本次写入是否改动了任一守备合计字段（缺栏按 0 比较）。 */
    private static boolean fieldingTotalsChanged(Map<String, Double> before, GamePlayerStat stat) {
        for (GamePlayerStatZeros.FieldValue value : GamePlayerStatZeros.values(stat)) {
            if (FIELDING_TOTAL_KEYS.contains(value.key())
                    && Double.compare(before.getOrDefault(value.key(), 0d), value.value()) != 0) {
                return true;
            }
        }
        return false;
    }

    /** 结果页显式修改的投手 R/ER：保存为人工覆写，并由 ER 引擎全量重算避免后续被冲掉。 */
    private void applyResultPitcherRunOverrides(
            Game game, List<SaveGameResultDTO.StatPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        if (!"final".equalsIgnoreCase(game.getStatus())) {
            throw new BusinessException(400, "比赛结束后才能手工覆写投手 R/ER");
        }
        Long operatorId = CurrentUserHolder.get();
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        Map<Long, int[]> values = new LinkedHashMap<>();
        for (SaveGameResultDTO.StatPart part : parts) {
            if (part.getPlayerId() == null) {
                continue;
            }
            if (part.getPitchR() == null || part.getEr() == null) {
                throw new BusinessException(400, "缺少投手 R/ER");
            }
            values.put(part.getPlayerId(),
                    new int[]{part.getPitchR().intValue(), part.getEr().intValue()});
        }
        try {
            this.earnedRunReconstructionService.applyResultPitcherRunOverrides(
                    game.getTenantId(), game.getId(), values, operatorId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, ex.getMessage());
        }
    }

    /**
     * 结果页逐位置守备按球员整体替换：删除该球员旧行后按 rows 重建，并由新行重算球员级合计。
     *
     * <p>未出现在 parts 中的球员不动；rows 为空数组表示清空该球员全部守位行，
     * 缺少 rows 字段（null）视为客户端漏传并拒绝，避免误清空。
     * 仅 final 比赛接受该字段，球队必须是本场主/客队，球员必须已有本场统计行。</p>
     *
     * <p>顺序：先完成全部球员的校验与构建（含 200 行上限），通过后再落库并统一写台账。
     * 台账服务用独立事务（REQUIRES_NEW），若在循环中即时写入，后续球员校验失败、
     * 外层回滚时会留下无法回滚的台账孤行。</p>
     *
     * @return 被替换的球员键（teamId_playerId）
     */
    private Set<String> applyFieldingPlayerParts(
            Game game, List<SaveGameResultDTO.FieldingPlayerPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return Set.of();
        }
        if (!"final".equalsIgnoreCase(game.getStatus())) {
            throw new BusinessException(400, "比赛结束后才能编辑逐位置守备");
        }
        if (parts.size() > 200) {
            throw new BusinessException(400, "逐位置守备球员过多");
        }
        List<GameFieldingPosStat> existingRows = this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(game.getId());
        Map<String, List<GamePlayerStat>> statsByPlayer = new LinkedHashMap<>();
        for (GamePlayerStat stat : this.gamePlayerStatRepository.findByGameId(game.getId())) {
            statsByPlayer.computeIfAbsent(resultStatPlayerKey(stat.getTeamId(), stat.getPlayerId()),
                    key -> new ArrayList<>()).add(stat);
        }
        List<FieldingReplacement> replacements = new ArrayList<>();
        int totalRows = 0;
        for (SaveGameResultDTO.FieldingPlayerPart part : parts) {
            if (part == null || part.getTeamId() == null || part.getPlayerId() == null) {
                continue;
            }
            FieldingReplacement replacement =
                    planPlayerFieldingRows(game, part, statsByPlayer, existingRows);
            totalRows += replacement.newRows().size();
            if (totalRows > 200) {
                throw new BusinessException(400, "逐位置守备数据过多");
            }
            replacements.add(replacement);
        }
        List<GameRevisionAuditService.StatChange> recomputeChanges = new ArrayList<>();
        List<FieldingPositionChange> positionChanges = new ArrayList<>();
        Set<String> replaced = new LinkedHashSet<>();
        for (FieldingReplacement replacement : replacements) {
            List<GameFieldingPosStat> savedRows =
                    this.savePlayerFieldingRows(game, replacement, recomputeChanges);
            positionChanges.add(new FieldingPositionChange(replacement.oldRows(), savedRows));
            replaced.add(replacement.key());
        }
        this.revisionAuditService.recordStatChanges(game, recomputeChanges, CurrentUserHolder.get());
        for (FieldingPositionChange change : positionChanges) {
            recordFieldingPositionChanges(game, change.oldRows(), change.newRows());
        }
        return replaced;
    }

    /** 单个球员的逐位置整体替换计划：全部校验通过后由落库阶段执行。 */
    private record FieldingReplacement(
            String key, Long teamId, Long playerId, GamePlayerStat stat,
            List<GameFieldingPosStat> oldRows, List<GameFieldingPosStat> newRows) {
    }

    /** 一位球员的台账数据：替换前旧行与落库后的新行。 */
    private record FieldingPositionChange(
            List<GameFieldingPosStat> oldRows, List<GameFieldingPosStat> newRows) {
    }

    /** 校验球队/球员并构建某球员的逐位置替换计划（不落库、不写台账）。 */
    private FieldingReplacement planPlayerFieldingRows(
            Game game, SaveGameResultDTO.FieldingPlayerPart part,
            Map<String, List<GamePlayerStat>> statsByPlayer,
            List<GameFieldingPosStat> existingRows) {
        if (!Objects.equals(part.getTeamId(), game.getHomeTeamId())
                && !Objects.equals(part.getTeamId(), game.getAwayTeamId())) {
            throw new BusinessException(400, "守备球队不属于本场比赛");
        }
        String key = resultStatPlayerKey(part.getTeamId(), part.getPlayerId());
        List<GamePlayerStat> playerStats = statsByPlayer.get(key);
        if (playerStats == null || playerStats.isEmpty()) {
            throw new BusinessException(400, "逐位置守备对象不在本场统计行中");
        }
        GamePlayerStat stat = selectFieldingStatRow(playerStats);
        List<GameFieldingPosStat> newRows = buildFieldingPositionRows(game, stat, part);
        List<GameFieldingPosStat> oldRows = new ArrayList<>();
        for (GameFieldingPosStat row : existingRows) {
            if (Objects.equals(row.getTeamId(), part.getTeamId())
                    && Objects.equals(row.getPlayerId(), part.getPlayerId())) {
                oldRows.add(row);
            }
        }
        return new FieldingReplacement(
                key, part.getTeamId(), part.getPlayerId(), stat, oldRows, newRows);
    }

    /** 落库单个球员的逐位置整体替换（删除→flush→重建→重算合计），返回落库后的新行。 */
    private List<GameFieldingPosStat> savePlayerFieldingRows(
            Game game, FieldingReplacement replacement,
            List<GameRevisionAuditService.StatChange> recomputeChanges) {
        this.gameFieldingPosStatRepository.deleteByGameIdAndTeamIdAndPlayerId(
                game.getId(), replacement.teamId(), replacement.playerId());
        // 先 flush 删除再插入：部分唯一索引（game_id, team_id, player_id, position）会在 INSERT
        // 先于同事务 DELETE 生效时报冲突，与 save-live 全量替换保持同一顺序。
        this.gameFieldingPosStatRepository.flush();
        List<GameFieldingPosStat> newRows = replacement.newRows();
        if (!newRows.isEmpty()) {
            newRows = this.gameFieldingPosStatRepository.saveAll(newRows);
        }
        Map<String, Double> beforeTotals = statValueMap(replacement.stat());
        applyPositionTotals(replacement.stat(), newRows);
        this.gamePlayerStatRepository.save(replacement.stat());
        // 逐位置编辑路径（stats=null）也要把合计重算写入 PLAYER_STAT 台账；统一在本轮校验全部通过后写入。
        addStatChanges(recomputeChanges, replacement.stat(), beforeTotals);
        return newRows;
    }

    /**
     * 二刀流同一球员有打者/投手两行时，守备合计属于打者行：
     * 优先取 {@code isPitcher != 1} 的行，没有则取第一行。
     */
    private static GamePlayerStat selectFieldingStatRow(List<GamePlayerStat> candidates) {
        for (GamePlayerStat candidate : candidates) {
            if (!Integer.valueOf(1).equals(candidate.getIsPitcher())) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    /** 校验并构建某球员的逐位置行（不落库）；rows 缺失、位置非法或重复时抛 400。 */
    private List<GameFieldingPosStat> buildFieldingPositionRows(
            Game game, GamePlayerStat stat, SaveGameResultDTO.FieldingPlayerPart part) {
        List<SaveGameResultDTO.FieldingPositionRowPart> rows = part.getRows();
        if (rows == null) {
            throw new BusinessException(400, "缺少逐位置行");
        }
        if (rows.size() > 200) {
            throw new BusinessException(400, "逐位置守备数据过多");
        }
        Set<String> allowed = allowedFieldingPositions(game);
        String firstPosition = normalizeFirstPosition(stat.getFirstPosition());
        Set<String> used = new LinkedHashSet<>();
        List<GameFieldingPosStat> result = new ArrayList<>();
        for (SaveGameResultDTO.FieldingPositionRowPart partRow : rows) {
            if (partRow == null) {
                continue;
            }
            String position = normalizePosition(partRow.getPosition());
            if (position == null || !allowed.contains(position)) {
                throw new BusinessException(400, "逐位置守备位置无效：" + partRow.getPosition());
            }
            if (!used.add(position)) {
                throw new BusinessException(400, "逐位置守备存在重复行");
            }
            int outs = partRow.getOuts() == null ? 0 : partRow.getOuts();
            if (outs < 0 || outs > 999) {
                throw new BusinessException(400, "逐位置守备出局数无效");
            }
            GameFieldingPosStat row = new GameFieldingPosStat();
            row.setTenantId(game.getTenantId());
            row.setGameId(game.getId());
            row.setTeamId(part.getTeamId());
            row.setPlayerId(part.getPlayerId());
            row.setPosition(position);
            row.setOuts(Integer.valueOf(outs));
            row.setPo(Integer.valueOf(normalizeFieldingCredit(partRow.getPo(), "接杀数")));
            row.setA(Integer.valueOf(normalizeFieldingCredit(partRow.getA(), "助杀数")));
            row.setE(Integer.valueOf(normalizeFieldingCredit(partRow.getE(), "失误数")));
            row.setDp(Integer.valueOf(normalizeFieldingCredit(partRow.getDp(), "双杀数")));
            row.setPb(Integer.valueOf(normalizeFieldingCredit(partRow.getPb(), "捕逸数")));
            row.setCatcherCs(Integer.valueOf(normalizeFieldingCredit(partRow.getCatcherCs(), "盗垒阻杀数")));
            row.setCatcherSb(Integer.valueOf(normalizeFieldingCredit(partRow.getCatcherSb(), "被盗垒数")));
            boolean gs = Integer.valueOf(1).equals(stat.getFieldingGs())
                    && (firstPosition == null ? result.isEmpty() : firstPosition.equals(position));
            row.setFieldingGs(Boolean.valueOf(gs));
            row.setSource("RESULT");
            result.add(row);
        }
        return result;
    }

    /** 按逐位置行重算球员级守备合计（PO/A/E/DP/PB/CS/SB、INN、TC），首守位为空时补首个守位。 */
    private static void applyPositionTotals(GamePlayerStat stat, List<GameFieldingPosStat> rows) {
        stat.setPo(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getPo)));
        stat.setA(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getA)));
        stat.setE(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getE)));
        stat.setDp(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getDp)));
        stat.setPb(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getPb)));
        stat.setCatcherCs(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getCatcherCs)));
        stat.setCatcherSb(Integer.valueOf(sumPositionInt(rows, GameFieldingPosStat::getCatcherSb)));
        int outs = sumPositionInt(rows, GameFieldingPosStat::getOuts);
        stat.setDefInn(Double.valueOf(StatsFormatUtil.baseballOutsToIp(outs)));
        stat.setTc(Integer.valueOf((stat.getPo() == null ? 0 : stat.getPo())
                + (stat.getA() == null ? 0 : stat.getA())
                + (stat.getE() == null ? 0 : stat.getE())));
        if (normalizeFirstPosition(stat.getFirstPosition()) == null && !rows.isEmpty()) {
            stat.setFirstPosition(rows.get(0).getPosition());
        }
    }

    /** 累加逐位置行的整数字段；缺栏按 0 计。 */
    private static int sumPositionInt(
            List<GameFieldingPosStat> rows,
            Function<GameFieldingPosStat, Integer> getter) {
        int total = 0;
        for (GameFieldingPosStat row : rows) {
            total += valueOrZero(getter.apply(row));
        }
        return total;
    }

    /** 本场可用的逐位置守位集合（垒球额外允许 XF 系列）。 */
    private static Set<String> allowedFieldingPositions(Game game) {
        return "SOFTBALL".equalsIgnoreCase(game.getGameMode())
                ? SOFTBALL_FIELDING_POSITIONS : BASEBALL_FIELDING_POSITIONS;
    }

    /**
     * 把结果页保存的球员级守备合计差额回写到逐位置守备明细（Issue #701 / #704）。
     *
     * <p>结果页守备表以球员级汇总为编辑对象，逐位置明细继续作为盒子比分拆分与守备位置榜的数据源。
     * 本次已提交逐位置整体替换的球员跳过：其合计由逐位置行重算，不能再被 stats 差额拉回。
     * 其余已有逐位置行的球员按差额回写；同一球员多守位时，正差额落在出场局数最多的位置行，
     * 负差额按出场局数从多到少扣减。旧数据没有逐位置行时，只有本轮守备合计有变化才按当前守位补一行
     * （stats=null 的纯逐位置编辑不补行）。</p>
     */
    private void syncResultFieldingPositions(
            Long gameId, List<SaveGameResultDTO.StatPart> stats,
            Set<String> replacedKeys, Set<String> fieldingChangedKeys) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        List<GameFieldingPosStat> rows = this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId);
        if (rows.isEmpty() && fieldingChangedKeys.isEmpty()) {
            return;
        }
        Map<String, List<GameFieldingPosStat>> rowsByPlayer = new LinkedHashMap<>();
        for (GameFieldingPosStat row : rows) {
            rowsByPlayer.computeIfAbsent(resultStatPlayerKey(row.getTeamId(), row.getPlayerId()),
                    key -> new ArrayList<>()).add(row);
        }
        Map<String, SaveGameResultDTO.StatPart> partsByPlayer = collectResultFieldingParts(stats);
        boolean changed = false;
        for (Map.Entry<String, SaveGameResultDTO.StatPart> entry : partsByPlayer.entrySet()) {
            String key = entry.getKey();
            if (replacedKeys.contains(key)) {
                continue;
            }
            List<GameFieldingPosStat> playerRows = rowsByPlayer.get(key);
            if (playerRows == null) {
                if (fieldingChangedKeys.contains(key)) {
                    GameFieldingPosStat created = createPositionRowFromPart(gameId, entry.getValue());
                    if (created != null) {
                        this.gameFieldingPosStatRepository.save(created);
                        rows.add(created);
                        applyFieldingDeltas(List.of(created), entry.getValue());
                    }
                }
                continue;
            }
            changed |= applyFieldingDeltas(playerRows, entry.getValue());
        }
        if (changed) {
            this.gameFieldingPosStatRepository.saveAll(rows);
        }
    }

    /** 按球员聚合 stats 中的守备合计；二刀流以打者行（聚合全部守位）优先于投手行。 */
    private static Map<String, SaveGameResultDTO.StatPart> collectResultFieldingParts(
            List<SaveGameResultDTO.StatPart> stats) {
        Map<String, SaveGameResultDTO.StatPart> partsByPlayer = new LinkedHashMap<>();
        for (SaveGameResultDTO.StatPart part : stats) {
            if (part == null || part.getTeamId() == null || part.getPlayerId() == null) {
                continue;
            }
            String key = resultStatPlayerKey(part.getTeamId(), part.getPlayerId());
            SaveGameResultDTO.StatPart previous = partsByPlayer.get(key);
            if (previous == null || (isPitcherStatPart(previous) && !isPitcherStatPart(part))) {
                partsByPlayer.put(key, part);
            }
        }
        return partsByPlayer;
    }

    /**
     * 旧数据补行：按球员当前守位建一条 RESULT 逐位置行；
     * 非 final 比赛、无有效守位、打击专用身份或出局数越界返回 null（不建行）。
     */
    private GameFieldingPosStat createPositionRowFromPart(
            Long gameId, SaveGameResultDTO.StatPart part) {
        String position = normalizePosition(part.getPosition());
        if (position == null || "DH".equals(position) || "PH".equals(position) || "EP".equals(position)) {
            return null;
        }
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null || !"final".equalsIgnoreCase(game.getStatus())) {
            return null;
        }
        if (!allowedFieldingPositions(game).contains(position)) {
            return null;
        }
        int outs = StatsFormatUtil.baseballIpToOuts(
                part.getDefInn() == null ? 0.0 : part.getDefInn().doubleValue());
        if (outs < 0 || outs > 999) {
            return null;
        }
        GameFieldingPosStat row = new GameFieldingPosStat();
        row.setTenantId(game.getTenantId());
        row.setGameId(gameId);
        row.setTeamId(part.getTeamId());
        row.setPlayerId(part.getPlayerId());
        row.setPosition(position);
        row.setOuts(Integer.valueOf(outs));
        row.setPo(Integer.valueOf(valueOrZero(part.getPo())));
        row.setA(Integer.valueOf(valueOrZero(part.getA())));
        row.setE(Integer.valueOf(valueOrZero(part.getE())));
        row.setDp(Integer.valueOf(valueOrZero(part.getDp())));
        row.setPb(Integer.valueOf(valueOrZero(part.getPb())));
        row.setCatcherCs(Integer.valueOf(valueOrZero(part.getCatcherCs())));
        row.setCatcherSb(Integer.valueOf(valueOrZero(part.getCatcherSb())));
        row.setFieldingGs(Boolean.valueOf(Integer.valueOf(1).equals(part.getFieldingGs())));
        row.setSource("RESULT");
        return row;
    }

    /** 逐位置整体替换的字段级台账：按守位对齐旧/新行，记录新增、修改与删除。 */
    private void recordFieldingPositionChanges(
            Game game, List<GameFieldingPosStat> oldRows, List<GameFieldingPosStat> newRows) {
        Long operatorId = CurrentUserHolder.get();
        Map<String, GameFieldingPosStat> oldByPos = new LinkedHashMap<>();
        for (GameFieldingPosStat row : oldRows) {
            oldByPos.put(row.getPosition(), row);
        }
        Map<String, GameFieldingPosStat> newByPos = new LinkedHashMap<>();
        for (GameFieldingPosStat row : newRows) {
            newByPos.put(row.getPosition(), row);
        }
        for (GameFieldingPosStat oldRow : oldRows) {
            GameFieldingPosStat newRow = newByPos.get(oldRow.getPosition());
            if (newRow == null) {
                recordFieldingRowFields(game, oldRow, null, "DELETE_ROW", operatorId);
            } else {
                recordFieldingRowFields(game, oldRow, newRow, "UPDATE_FIELD", operatorId);
            }
        }
        for (GameFieldingPosStat newRow : newRows) {
            if (!oldByPos.containsKey(newRow.getPosition())) {
                recordFieldingRowFields(game, null, newRow, "ADD_ROW", operatorId);
            }
        }
    }

    /** 记录单个守位行各守备字段的 before/after；字段未变化不写。 */
    private void recordFieldingRowFields(
            Game game, GameFieldingPosStat oldRow, GameFieldingPosStat newRow,
            String action, Long operatorId) {
        GameFieldingPosStat source = newRow != null ? newRow : oldRow;
        recordFieldingField(game, source, action, "position", "守位",
                oldRow == null ? "" : oldRow.getPosition(),
                newRow == null ? "" : newRow.getPosition(), operatorId);
        recordFieldingField(game, source, action, "outs", "守备出局",
                numberText(oldRow == null ? null : oldRow.getOuts()),
                numberText(newRow == null ? null : newRow.getOuts()), operatorId);
        recordFieldingField(game, source, action, "po", "PO",
                numberText(oldRow == null ? null : oldRow.getPo()),
                numberText(newRow == null ? null : newRow.getPo()), operatorId);
        recordFieldingField(game, source, action, "a", "A",
                numberText(oldRow == null ? null : oldRow.getA()),
                numberText(newRow == null ? null : newRow.getA()), operatorId);
        recordFieldingField(game, source, action, "e", "E",
                numberText(oldRow == null ? null : oldRow.getE()),
                numberText(newRow == null ? null : newRow.getE()), operatorId);
        recordFieldingField(game, source, action, "dp", "DP",
                numberText(oldRow == null ? null : oldRow.getDp()),
                numberText(newRow == null ? null : newRow.getDp()), operatorId);
        recordFieldingField(game, source, action, "pb", "PB",
                numberText(oldRow == null ? null : oldRow.getPb()),
                numberText(newRow == null ? null : newRow.getPb()), operatorId);
        recordFieldingField(game, source, action, "catcherSb", "SB",
                numberText(oldRow == null ? null : oldRow.getCatcherSb()),
                numberText(newRow == null ? null : newRow.getCatcherSb()), operatorId);
        recordFieldingField(game, source, action, "catcherCs", "CS",
                numberText(oldRow == null ? null : oldRow.getCatcherCs()),
                numberText(newRow == null ? null : newRow.getCatcherCs()), operatorId);
        recordFieldingField(game, source, action, "fieldingGs", "GS",
                booleanText(oldRow == null ? null : oldRow.getFieldingGs()),
                booleanText(newRow == null ? null : newRow.getFieldingGs()), operatorId);
    }

    /** 写入一条 FIELDPOS_STAT 台账；before/after 相同则跳过。 */
    private void recordFieldingField(
            Game game, GameFieldingPosStat source, String action,
            String key, String label, String before, String after, Long operatorId) {
        if (Objects.equals(before, after)) {
            return;
        }
        Long tenantId = game.getTenantId();
        Long gameId = game.getId();
        if (tenantId == null || gameId == null) {
            return;
        }
        this.revisionAuditService.recordAdjustment(
                tenantId.longValue(), gameId.longValue(),
                "FIELDPOS_STAT", action, "RESULT_SAVE",
                source.getId(), source.getTeamId(), source.getPlayerId(), null,
                key, label, before, after, "ACTIVE", null, operatorId);
    }

    private static String numberText(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** 布尔字段台账文本：true/1，false/0，缺失为空串。 */
    private static String booleanText(Boolean value) {
        if (value == null) {
            return "";
        }
        return value.booleanValue() ? "1" : "0";
    }

    private static String resultStatPlayerKey(Long teamId, Long playerId) {
        return teamId + "_" + playerId;
    }

    /** 按差额把球员级守备合计分摊到逐位置行；返回是否有行发生变化。 */
    private static boolean applyFieldingDeltas(
            List<GameFieldingPosStat> rows, SaveGameResultDTO.StatPart part) {
        // 正差额落在出场最多的位置行，故按 outs 排一次供各计数字段共用。
        List<GameFieldingPosStat> ordered = orderedByOuts(rows);
        // 捕逸/盗垒阻杀/被盗垒只属于捕手位置，避免分摊到出场更多的其他守位。
        List<GameFieldingPosStat> catcherRows = rows.stream()
                .filter(row -> "C".equalsIgnoreCase(row.getPosition()))
                .toList();
        boolean changed = false;
        changed |= applyFieldingDelta(ordered, part.getPo(),
                GameFieldingPosStat::getPo, GameFieldingPosStat::setPo);
        changed |= applyFieldingDelta(ordered, part.getA(),
                GameFieldingPosStat::getA, GameFieldingPosStat::setA);
        changed |= applyFieldingDelta(ordered, part.getE(),
                GameFieldingPosStat::getE, GameFieldingPosStat::setE);
        changed |= applyFieldingDelta(ordered, part.getDp(),
                GameFieldingPosStat::getDp, GameFieldingPosStat::setDp);
        changed |= applyFieldingDelta(catcherRows, part.getPb(),
                GameFieldingPosStat::getPb, GameFieldingPosStat::setPb);
        changed |= applyFieldingDelta(catcherRows, part.getCatcherCs(),
                GameFieldingPosStat::getCatcherCs, GameFieldingPosStat::setCatcherCs);
        changed |= applyFieldingDelta(catcherRows, part.getCatcherSb(),
                GameFieldingPosStat::getCatcherSb, GameFieldingPosStat::setCatcherSb);
        if (part.getDefInn() != null) {
            changed |= applyFieldingDelta(ordered,
                    StatsFormatUtil.baseballIpToOuts(part.getDefInn().doubleValue()),
                    GameFieldingPosStat::getOuts, GameFieldingPosStat::setOuts);
        }
        if (part.getFieldingGs() != null) {
            changed |= applyFieldingGs(rows, part.getFieldingGs().intValue() == 1);
        }
        return changed;
    }

    /** 先发标记按球员归一：最多只保留一行 GS=1，目标为 0 时全部清除。 */
    private static boolean applyFieldingGs(List<GameFieldingPosStat> rows, boolean target) {
        GameFieldingPosStat chosen = null;
        if (target) {
            // 已有 GS=1 的行保持不变（同分取出场更多者），否则回落到出场最多的位置行。
            chosen = rows.stream()
                    .filter(row -> Boolean.TRUE.equals(row.getFieldingGs()))
                    .max(Comparator.comparingInt(row -> valueOrZero(row.getOuts())))
                    .orElseGet(() -> orderedByOuts(rows).get(0));
        }
        boolean changed = false;
        for (GameFieldingPosStat row : rows) {
            boolean desired = row == chosen;
            if (Boolean.TRUE.equals(row.getFieldingGs()) != desired) {
                row.setFieldingGs(desired);
                changed = true;
            }
        }
        return changed;
    }

    /** 单个守备字段按差额回写；入参已按 outs 降序，目标值缺栏时不动（旧客户端/漏字段不误清零）。 */
    private static boolean applyFieldingDelta(
            List<GameFieldingPosStat> orderedRows, Integer targetValue,
            Function<GameFieldingPosStat, Integer> getter,
            BiConsumer<GameFieldingPosStat, Integer> setter) {
        if (targetValue == null || orderedRows.isEmpty()) {
            return false;
        }
        int sum = 0;
        for (GameFieldingPosStat row : orderedRows) {
            sum += valueOrZero(getter.apply(row));
        }
        int delta = targetValue.intValue() - sum;
        if (delta == 0) {
            return false;
        }
        if (delta > 0) {
            GameFieldingPosStat row = orderedRows.get(0);
            setter.accept(row, valueOrZero(getter.apply(row)) + delta);
            return true;
        }
        // 负差额从出场最多的位置依次扣，且不把某一行扣成负数。
        int remaining = -delta;
        for (GameFieldingPosStat row : orderedRows) {
            int current = valueOrZero(getter.apply(row));
            int take = Math.min(remaining, current);
            if (take > 0) {
                setter.accept(row, current - take);
                remaining -= take;
            }
            if (remaining == 0) {
                break;
            }
        }
        return true;
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    /** 出场局数多的位置优先承接差额；同局数按 id 稳定排序。 */
    private static List<GameFieldingPosStat> orderedByOuts(List<GameFieldingPosStat> rows) {
        List<GameFieldingPosStat> ordered = new ArrayList<>(rows);
        ordered.sort(Comparator
                .comparingInt((GameFieldingPosStat row) -> valueOrZero(row.getOuts())).reversed()
                .thenComparing(GameFieldingPosStat::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return ordered;
    }

    /** 比賽統計列身分複合鍵：與 saveLive 的 existingMap key 同構（isPitcher 為 null 時視為 0） */
    private static String resultStatKey(Long teamId, Long playerId, Integer isPitcher) {
        return String.valueOf(teamId) + "_" + playerId + "_" + (isPitcher == null ? 0 : isPitcher.intValue());
    }

    /**
     * 显式删除本次保存要求移除的统计行。
     *
     * <p>只有调用方在 {@code removedStatIds} 中明确列出的行才删除；
     * payload 缺失但未列出的行不再自动删除，避免旧页面/多标签页漏带导致真实球员数据丢失。
     * 重复 id 只删除一次；null 或本场不存在的 id 忽略。</p>
     */
    private void deleteRemovedStatRows(Long gameId, Long tenantId, boolean engineManaged,
            List<Long> removedStatIds, Map<Long, GamePlayerStat> existingById,
            LinkedHashSet<Long> keepIds) {
        for (Long statId : new LinkedHashSet<>(removedStatIds)) {
            if (statId == null) {
                continue;
            }
            if (keepIds.contains(statId)) {
                throw new BusinessException(400, "同一统计行不能同时保存和删除");
            }
            GamePlayerStat row = existingById.get(statId);
            if (row == null) {
                continue;
            }
            if (row.getPlayerId() != null && tenantId != null
                    && this.fieldingAdjustmentService.hasApprovedPlayerAdjustment(
                            tenantId, gameId, row.getPlayerId())) {
                throw new BusinessException(400, "该球员仍有守备补记，请先撤销后再删除");
            }
            if (engineManaged && Integer.valueOf(1).equals(row.getIsPitcher())) {
                if (!GamePlayerStatZeros.isZero(row)) {
                    throw new BusinessException(400, "该投手行仍有数据，请先转记后再删除");
                }
                if (row.getPlayerId() != null
                        && this.earnedRunReconstructionService.hasResponsiblePitcherDecisions(
                                gameId, row.getPlayerId())) {
                    throw new BusinessException(400, "该投手仍有自责分判定，请先处理后再删除");
                }
            }
            this.gamePlayerStatRepository.delete(row);
        }
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
        // 赛后补录没有换位历史：仅新建行按当前守位兜底。
        // 已有行（尤其实时录入的代打/代跑）不得用最后守位回填，否则会把从未防守的球员重新写进守备表。
        if (stat.getId() == null && (stat.getFirstPosition() == null || stat.getFirstPosition().isBlank())) {
            stat.setFirstPosition(normalizeFirstPosition(part.getPosition()));
        }
        // 先发守备位：结果页新建行中，先发打者/先发投手按当前守位写入；替补不写
        if (stat.getId() == null && stat.getStartingPosition() == null) {
            boolean startingPitcher = Integer.valueOf(1).equals(part.getIsPitcher())
                    && Integer.valueOf(1).equals(part.getPitcherOrder());
            boolean starter = part.getBattingOrder() != null || startingPitcher;
            if (starter) {
                stat.setStartingPosition(startingPitcher && part.getBattingOrder() == null
                        ? "P" : normalizeFirstPosition(part.getPosition()));
            }
        }
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
        stat.setOutsRecorded(StatsFormatUtil.baseballIpToOuts(part.getIp() != null ? part.getIp() : 0.0));
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
            // 导入/legacy 客户端若带来自责分分解的两栏，按同口径写入；缺栏保留既有值，
            // 不把已算好的 UER/PEND 抹成 NULL（否则 R=ER+UER+PENDING 永远无法核对）。
            if (part.getUnearnedR() != null) {
                stat.setUnearnedR(part.getUnearnedR());
            }
            if (part.getPendingR() != null) {
                stat.setPendingR(part.getPendingR());
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
        syncPitchPaBf(stat, part.getPitchBf(), part.getPitchPa());
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
        if (part.getPickoffAttempts() != null) {
            stat.setPickoffAttempts(part.getPickoffAttempts());
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

    /**
     * 导入场次的投手失分分解把关：失分必须等于自责分＋非自责分＋待判定，且自责分不得大于失分。
     *
     * <p>importGameResult 新建的比赛没有 bs_earned_run_decision 可依附，引擎无从重算，
     * 所以只能要求送进来的数字自己对得上；这与 saveLive/saveResult「引擎场次不收客户端 er」
     * 是同一个目标的两面：能算的交给引擎，算不了的至少不能自相矛盾。
     *
     * <p>任一栏缺失即跳过该列不判（历史数据缺值略过判定的既定口径），
     * 因此只送 er/pitchR 的旧导入端不会被新规则挡下。
     */
    private void assertImportedPitcherTotalsCoherent(List<SaveGameResultDTO.StatPart> stats) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        for (SaveGameResultDTO.StatPart part : stats) {
            if (part == null || part.getPlayerId() == null || part.getTeamId() == null) {
                continue;
            }
            if (!Integer.valueOf(1).equals(part.getIsPitcher())) {
                continue;
            }
            Integer r = part.getPitchR();
            Integer er = part.getEr();
            Integer unearned = part.getUnearnedR();
            Integer pending = part.getPendingR();
            if (r == null || er == null || unearned == null || pending == null) {
                continue;
            }
            if (er < 0 || unearned < 0 || pending < 0 || er > r || er + unearned + pending != r) {
                throw new BusinessException(400, String.format(
                        "投手失分分解不一致：球员 %d 失分 R=%d，自责分 %d＋非自责分 %d＋待判定 %d=%d。导入场次没有引擎判定可重算，请先让四项对得上再导入。",
                        part.getPlayerId(), r, er, unearned, pending, er + unearned + pending));
            }
        }
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

    /** PA 与 BF 同口径：任一栏有值时两栏同步，避免结果页/实时录入产生两套面对打席数。 */
    private static void syncPitchPaBf(GamePlayerStat stat, Integer pitchBf, Integer pitchPa) {
        Integer value = pitchBf != null ? pitchBf : pitchPa;
        if (value != null) {
            stat.setPitchBf(value);
            stat.setPitchPa(value);
        }
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
        // 首个守位只由实时录入首次上场时写入；旧客户端不带该字段时保留库中已有值
        if (part.getFirstPosition() != null) {
            stat.setFirstPosition(normalizeFirstPosition(part.getFirstPosition()));
        }
        // 先发守备位只由实时录入在开赛前阵容写入；旧客户端/替补不带该字段时保留空值
        if (part.getStartingPosition() != null) {
            stat.setStartingPosition(normalizeFirstPosition(part.getStartingPosition()));
        }
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
        stat.setOutsRecorded(StatsFormatUtil.baseballIpToOuts(part.getIp() != null ? part.getIp() : 0.0));
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
        syncPitchPaBf(stat, part.getPitchBf(), part.getPitchPa());
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
        if (part.getPickoffAttempts() != null) {
            stat.setPickoffAttempts(part.getPickoffAttempts());
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
    public GameService(GameRepository gameRepository, GamePlayerStatRepository gamePlayerStatRepository, GameSubstitutionEventRepository gameSubstitutionEventRepository, EventRepository eventRepository, StadiumRepository stadiumRepository, AccountScopeService accountScopeService, ScopeQuerySupport scopeQuerySupport, ResourceGuard resourceGuard, TenantQueryPolicyService tenantQueryPolicyService, EarnedRunReconstructionService earnedRunReconstructionService, EventRegistrationService eventRegistrationService, GameFieldingPosStatRepository gameFieldingPosStatRepository, GameFieldingAdjustmentService fieldingAdjustmentService, GameRevisionAuditService revisionAuditService, GameSnapshotRepository gameSnapshotRepository) {
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
        this.gameFieldingPosStatRepository = gameFieldingPosStatRepository;
        this.fieldingAdjustmentService = fieldingAdjustmentService;
        this.revisionAuditService = revisionAuditService;
        this.gameSnapshotRepository = gameSnapshotRepository;
    }
}

