/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.common.json.PositionsJsonUtil
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.PlayerGameLogEntryDTO
 *  com.bsball.model.dto.PlayerOptionDto
 *  com.bsball.model.dto.PlayerStatsByEventDTO
 *  com.bsball.model.dto.TeamPlayerOptionDto
 *  com.bsball.model.entity.HistoryRecord
 *  com.bsball.model.entity.Player
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.PlayerRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.bsball.service.PlayerService
 *  com.bsball.service.StatsService
 *  com.bsball.service.TenantQueryPolicyService
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.persistence.criteria.Root
 *  jakarta.persistence.criteria.Subquery
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.common.json.PositionsJsonUtil;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.model.dto.PlayerOptionDto;
import com.bsball.model.dto.PlayerStatsByEventDTO;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.dto.TeamPlayerOptionDto;
import com.bsball.model.entity.HistoryRecord;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import com.bsball.service.PersonnelHistoryRecorder;
import com.bsball.service.StatsService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final StatsService statsService;
    private final AccountScopeService accountScopeService;
    private final ScopeQuerySupport scopeQuerySupport;
    private final ResourceGuard resourceGuard;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final PlayerTeamService playerTeamService;
    private final PlayerTeamRepository playerTeamRepository;
    private final SysConfigService sysConfigService;
    private final PlayerClaimRepository playerClaimRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private static final int PLAYER_BG_IMAGES_MAX = 5;

    /** 本人档案（SELF 通道）可编辑字段白名单；白名单外的键一律忽略。 */
    private static final Set<String> SELF_EDITABLE = Set.of(
            "height", "weight", "throwHand", "batHand", "avatar", "bgImage", "bgImages", "bgFocusConfig",
            "nickname", "nameEn", "birthDate", "birthPlace", "education", "intro", "contactPhone", "contactEmail",
            "draft", "debut", "name", "positions");

    public List<PlayerOptionDto> listForSelect() {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            return this.playerRepository.findAllForSelect();
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (visibleTeamIds == null) {
            return this.playerRepository.findAllForSelectByTenantId(tid);
        }
        if (visibleTeamIds.isEmpty()) {
            return List.of();
        }
        return this.playerRepository.findAllForSelectByTenantIdAndTeamIdIn(tid, visibleTeamIds);
    }

    public List<TeamPlayerOptionDto> listTeamPlayerOptions(long teamId) {
        if (teamId <= 0L) {
            throw new BusinessException(400, "\u5fc5\u987b\u4f20\u5165\u7403\u961f ID\uff08teamId\uff09");
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (visibleTeamIds != null && !visibleTeamIds.contains(teamId)) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7403\u961f\u7684\u7403\u5458");
        }
        List<Object[]> rows = this.playerTeamRepository.findTeamPlayerOptionFields(tid, teamId);
        ArrayList<TeamPlayerOptionDto> out = new ArrayList<TeamPlayerOptionDto>(rows.size());
        for (Object[] r : rows) {
            Long id = r[0] != null ? Long.valueOf(((Number)r[0]).longValue()) : null;
            String name = r[1] != null ? String.valueOf(r[1]) : null;
            String number = r[2] != null ? String.valueOf(r[2]) : null;
            String positionsRaw = r[3] != null ? String.valueOf(r[3]) : null;
            String batHand = r[4] != null ? String.valueOf(r[4]) : null;
            String throwHand = r[5] != null ? String.valueOf(r[5]) : null;
            String status = r[6] != null ? String.valueOf(r[6]) : null;
            out.add(new TeamPlayerOptionDto(id, name, number, PlayerService.parsePositionsList((String)positionsRaw), batHand, throwHand, status));
        }
        return out;
    }

    private static List<String> parsePositionsList(String raw) {
        return PositionsJsonUtil.parseList((String)raw);
    }

    public PageResult<Player> list(Integer page, Integer pageSize, String sortProp, String sortOrder, Long teamId, List<Long> ids, String keyword, String number, String position, String throwHand, String batHand, String status, String joinDateFrom, String joinDateTo) {
        boolean global = this.tenantQueryPolicyService.isGlobalQueryMode();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (!global && visibleTeamIds != null && visibleTeamIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (!(global || visibleTeamIds == null || teamId == null || teamId == 0L || visibleTeamIds.contains(teamId))) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (ids != null && !ids.isEmpty()) {
            List<Player> rows = this.playerRepository.findByDeletedAtIsNullAndIdIn(ids);
            List<Player> list;
            if (global) {
                list = rows;
            } else {
                List<Player> tenantRows = rows.stream().filter(p -> Objects.equals(p.getTenantId(), tid)).toList();
                if (visibleTeamIds == null) {
                    list = tenantRows;
                } else {
                    Map<Long, Set<Long>> currentTeams = this.playerTeamService.currentTeamIdsByPlayerIds(tenantRows.stream().map(Player::getId).toList());
                    list = tenantRows.stream().filter(p -> this.visibleInScope(visibleTeamIds, currentTeams.get(p.getId()))).toList();
                }
            }
            this.playerTeamService.attachEntries(list);
            return PageResult.of((List)list, (long)list.size());
        }
        boolean hasFilter = keyword != null && !keyword.isBlank() || number != null && !number.isBlank() || position != null && !position.isBlank() || throwHand != null && !throwHand.isBlank() || batHand != null && !batHand.isBlank() || status != null && !status.isBlank() || joinDateFrom != null && !joinDateFrom.isBlank() || joinDateTo != null && !joinDateTo.isBlank() || teamId != null;
        Pageable p2 = this.buildPageable(page, pageSize, sortProp, sortOrder);
        Specification spec = this.buildListSpec(teamId, keyword, number, position, throwHand, batHand, status, joinDateFrom, joinDateTo, tid, visibleTeamIds, hasFilter, global);
        Page result = this.playerRepository.findAll(spec, p2);
        List<Player> content = result.getContent();
        this.playerTeamService.attachEntries(content);
        return PageResult.of((List)content, (long)result.getTotalElements());
    }

    private Specification<Player> buildListSpec(Long teamId, String keyword, String number, String position, String throwHand, String batHand, String status, String joinDateFrom, String joinDateTo, long tid, List<Long> visibleTeamIds, boolean applyExtraFilters, boolean global) {
        return (root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (!global) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            boolean freeAgentOnly = teamId != null && teamId == 0L;
            if (!(global || visibleTeamIds == null || freeAgentOnly)) {
                preds.add(cb.exists(this.currentTeamSubquery(cb, q, root, visibleTeamIds, null)));
            }
            if (teamId != null && teamId != 0L) {
                preds.add(cb.exists(this.currentTeamSubquery(cb, q, root, null, teamId)));
            } else if (freeAgentOnly) {
                preds.add(cb.not(cb.exists(this.currentTeamSubquery(cb, q, root, null, null))));
            }
            if (applyExtraFilters) {
                if (keyword != null && !keyword.isBlank()) {
                    String k = "%" + keyword.toLowerCase() + "%";
                    preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("name")), k), cb.like(cb.lower((Expression)root.get("nickname")), k), cb.like(cb.lower((Expression)root.get("shortName")), k)}));
                }
                if (number != null && !number.isBlank()) {
                    preds.add(this.entryFieldExists(cb, q, root, "number", "%" + number + "%"));
                }
                if (position != null && !position.isBlank()) {
                    preds.add(this.entryFieldExists(cb, q, root, "positions", "%\"" + position + "\"%"));
                }
                if (throwHand != null && !throwHand.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("throwHand"), (Object)throwHand));
                }
                if (batHand != null && !batHand.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("batHand"), (Object)batHand));
                }
                if (status != null && !status.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("status"), (Object)status));
                }
                if (joinDateFrom != null && !joinDateFrom.isBlank() || joinDateTo != null && !joinDateTo.isBlank()) {
                    Subquery sq = q.subquery(Long.class);
                    Root pcRoot = sq.from(HistoryRecord.class);
                    sq.select((Expression)pcRoot.get("id"));
                    ArrayList<Predicate> sqPreds = new ArrayList<Predicate>();
                    if (joinDateFrom != null && !joinDateFrom.isBlank()) {
                        sqPreds.add(cb.greaterThanOrEqualTo((Expression)pcRoot.get("changeDate"), (Comparable)((Object)joinDateFrom)));
                    }
                    if (joinDateTo != null && !joinDateTo.isBlank()) {
                        sqPreds.add(cb.lessThanOrEqualTo((Expression)pcRoot.get("changeDate"), (Comparable)((Object)joinDateTo)));
                    }
                    sq.where((Expression)cb.and(sqPreds.toArray(new Predicate[0])));
                    preds.add(root.get("currentJoinRecordId").in(new Expression[]{sq}));
                }
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    private Subquery<Long> currentTeamSubquery(CriteriaBuilder cb, CriteriaQuery<?> q, Root<Player> root, Collection<Long> teamIds, Long teamId) {
        Subquery<Long> sq = q.subquery(Long.class);
        Root<PlayerTeam> entry = sq.from(PlayerTeam.class);
        sq.select((Expression)entry.get("id"));
        ArrayList<Predicate> conditions = new ArrayList<Predicate>();
        conditions.add(cb.isNull((Expression)entry.get("deletedAt")));
        conditions.add(cb.equal(entry.get("playerId"), root.get("id")));
        conditions.add(cb.isTrue(entry.get("current")));
        if (teamId != null) {
            conditions.add(cb.equal(entry.get("teamId"), (Object)teamId));
        }
        if (teamIds != null) {
            if (teamIds.isEmpty()) {
                conditions.add(cb.disjunction());
            } else {
                conditions.add(entry.get("teamId").in((Collection)teamIds));
            }
        }
        sq.where((Expression)cb.and(conditions.toArray(new Predicate[0])));
        return sq;
    }

    private Predicate entryFieldExists(CriteriaBuilder cb, CriteriaQuery<?> q, Root<Player> root, String field, String pattern) {
        Subquery<Long> sq = q.subquery(Long.class);
        Root<PlayerTeam> entry = sq.from(PlayerTeam.class);
        sq.select((Expression)entry.get("id"));
        sq.where((Expression)cb.and(
                cb.isNull((Expression)entry.get("deletedAt")),
                cb.equal(entry.get("playerId"), root.get("id")),
                cb.like(entry.get(field).as(String.class), pattern)));
        return cb.exists(sq);
    }

    private boolean visibleInScope(Collection<Long> visibleTeamIds, Set<Long> currentTeamIds) {
        if (currentTeamIds == null || currentTeamIds.isEmpty()) {
            return true;
        }
        for (Long teamId : currentTeamIds) {
            if (visibleTeamIds.contains(teamId)) {
                return true;
            }
        }
        return false;
    }

    public Player get(Long id) {
        Player p = this.playerRepository.findById(id).orElse(null);
        if (p == null || p.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(p.getTenantId(), tid)) {
            return null;
        }
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (visibleTeamIds != null && this.playerTeamService.countCurrentEntriesInTeams(p.getId(), visibleTeamIds) == 0L) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7403\u5458");
        }
        this.playerTeamService.attachEntries(p);
        return p;
    }

    public Map<String, Object> getStats(Long id, String gameMode) {
        if (id == null) {
            return this.statsService.emptyPlayerStats();
        }
        Player p = this.get(id);
        if (p == null) {
            return this.statsService.emptyPlayerStats();
        }
        return this.statsService.getPlayerStats(id, gameMode);
    }

    public List<PlayerStatsByEventDTO> getStatsBySeason(Long id, String gameMode) {
        if (id == null || this.get(id) == null) {
            return List.of();
        }
        return this.statsService.getPlayerStatsBySeason(id, gameMode);
    }

    public List<PlayerGameLogEntryDTO> getGameLog(Long id, int limit, String gameMode) {
        if (id == null || this.get(id) == null) {
            return List.of();
        }
        return this.statsService.getPlayerGameLog(id, limit, gameMode);
    }

    public PageResult<Map<String, Object>> drillDownBatting(Long id, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (id == null || this.get(id) == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        return this.statsService.drillDownBatting(id, metric, page, pageSize, eventId, season, gameMode);
    }

    public PageResult<Map<String, Object>> drillDownPitching(Long id, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (id == null || this.get(id) == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        return this.statsService.drillDownPitching(id, metric, page, pageSize, eventId, season, gameMode);
    }

    public PageResult<Map<String, Object>> drillDownFielding(Long id, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (id == null || this.get(id) == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        return this.statsService.drillDownFielding(id, metric, page, pageSize, eventId, season, gameMode);
    }

    @Transactional(rollbackFor={Exception.class})
    public Player create(Player entity) {
        if (entity.getName() != null) {
            String n = entity.getName().trim();
            entity.setName(n.isEmpty() ? null : n);
        }
        this.applyTenantFromTeam(entity);
        this.validateTeamId(entity.getTeamId());
        // 批次 3b（spec §6.9）：代建守卫——受限身份仅可为本队（team_manager）/ 本联盟域内球队（league_organizer）代建未认领球员。
        this.resourceGuard.assertCanCreateUnclaimedPlayer(entity.getTeamId());
        PlayerService.normalizeBlankStringsToNull((Player)entity);
        PlayerService.normalizePlayerBackgroundFields((Player)entity);
        PlayerTeamService.PlayerTeamSyncPlan plan = this.playerTeamService.plan(entity, entity.getTeamEntries(), entity.getTeamEntries() != null);
        this.playerTeamService.applyMirror(entity, plan);
        Player saved = (Player)this.playerRepository.save(entity);
        this.playerTeamService.persistPlan(saved.getId(), plan);
        Long joinRecordId = this.personnelHistoryRecorder.recordPlayerTeamTransitions(saved, plan.beforeCurrentTeamIds(), plan.afterCurrentTeamIds());
        if (joinRecordId != null) {
            saved.setCurrentJoinRecordId(joinRecordId);
            saved = (Player)this.playerRepository.save(saved);
        }
        this.playerTeamService.attachEntries(saved);
        return saved;
    }

    public boolean isFullNameDuplicate(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String n = name.trim();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (excludeId != null) {
            return this.playerRepository.countActiveByTenantIdAndFullNameExcludingId(tid, n, excludeId.longValue()) > 0L;
        }
        return this.playerRepository.countActiveByTenantIdAndFullName(tid, n) > 0L;
    }

    @Transactional(rollbackFor={Exception.class})
    public Player update(Long id, Player entity) {
        Player existing = this.playerRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u7403\u5458");
        }
        this.resourceGuard.assertCanEditPlayerProfile(id, ResourceGuard.PlayerEditChannel.ROSTER);
        this.applyTenantFromTeam(entity);
        this.validateTeamId(entity.getTeamId());
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        if (entity.getName() != null) {
            String n = entity.getName().trim();
            entity.setName(n.isEmpty() ? null : n);
        }
        PlayerService.normalizeBlankStringsToNull((Player)entity);
        PlayerService.normalizePlayerBackgroundFields((Player)entity);
        Player before = PersonnelHistoryRecorder.snapshotPlayer((Player)existing);
        PlayerTeamService.PlayerTeamSyncPlan plan = this.playerTeamService.plan(entity, entity.getTeamEntries(), entity.getTeamEntries() != null);
        this.assertEntriesDeletable(id, plan.toDelete());
        this.playerTeamService.applyMirror(entity, plan);
        Player saved = (Player)this.playerRepository.save(entity);
        this.playerTeamService.persistPlan(saved.getId(), plan);
        this.personnelHistoryRecorder.afterPlayerUpdate(before, saved);
        Long joinRecordId = this.personnelHistoryRecorder.recordPlayerTeamTransitions(saved, plan.beforeCurrentTeamIds(), plan.afterCurrentTeamIds());
        if (joinRecordId != null) {
            saved.setCurrentJoinRecordId(joinRecordId);
            saved = (Player)this.playerRepository.save(saved);
        }
        for (PlayerTeam removedEntry : plan.toDelete()) {
            this.personnelHistoryRecorder.recordPlayerTeamEntryRemoval(saved.getId(), saved.getTenantId(), removedEntry);
        }
        this.playerTeamService.attachEntries(saved);
        return saved;
    }

    /**
     * 取本人（SELF）关联的球员档案；无关联时返回 {@code null}。
     *
     * @param userId 当前登录用户 ID
     * @return 本人档案或 {@code null}
     */
    public Player getSelfProfile(Long userId) {
        return this.playerRepository.findFirstByUserIdAndDeletedAtIsNull(userId).orElse(null);
    }

    /**
     * 自助建档：为当前用户创建一份归属自身的球员档案。
     * <p>依次校验：租户开关、是否已有本人档案、是否已认领球员、同租户内同名同生日去重；
     * 通过后置入 tenantId/userId/status 并落库。
     *
     * @param userId 当前登录用户 ID
     * @param draft  档案草稿（由控制器反序列化）
     * @return 落库后的球员档案
     */
    @Transactional(rollbackFor={Exception.class})
    public Player createSelfProfile(Long userId, Player draft) {
        // TODO(批3b硬化)：并发双击可能双建档（bs_player.user_id 无唯一约束）；随唯一索引 ux_bs_player_user_active 迁移硬化。
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!this.sysConfigService.getBoolean(tid, "portalPlayerSelfCreateEnabled", true)) {
            throw new BusinessException(400, "自助建档未开放");
        }
        if (this.playerRepository.findFirstByUserIdAndDeletedAtIsNull(userId).isPresent()) {
            throw new BusinessException(400, "您已有关联的球员档案");
        }
        if (this.playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(userId, "approved")) {
            throw new BusinessException(400, "您已认领球员，不能重复建档");
        }
        if (draft.getName() != null && draft.getBirthDate() != null
                && !this.playerRepository.findByNameAndBirthDateAndTenantIdAndDeletedAtIsNull(
                        draft.getName(), draft.getBirthDate(), tid).isEmpty()) {
            throw new BusinessException(400, "存在同名同生日的球员档案，请改用认领流程");
        }
        draft.setTenantId(tid);
        draft.setUserId(userId);
        draft.setStatus("active");
        // 对齐 admin create() 做法：裁剪客户端可注入字段——
        // 置空 id 以防注入走 update 语义；显式置空 ROSTER 语义字段（自助建档未入队，
        // 编号/镜像/经历由 T3.7/T3.10 入队流产生）。
        draft.setId(null);
        draft.setTeamId(null);
        draft.setNumber(null);
        draft.setCurrentJoinRecordId(null);
        draft.setSort(null);
        draft.setDeletedAt(null);
        draft.setDeletedBy(null);
        // 复用与 updateSelfProfile/admin create 同一归一化链（create() 的
        // applyTenantFromTeam/validateTeamId 面向有队场景，自助建档无队，跳过）。
        PlayerService.normalizeBlankStringsToNull(draft);
        PlayerService.normalizePlayerBackgroundFields(draft);
        return this.playerRepository.save(draft);
    }

    /**
     * 本人档案编辑（SELF 通道）：先经写保护守卫，再按 {@link #SELF_EDITABLE} 白名单逐字段适配；
     * 白名单外的键一律忽略；组图 / JSON 字段复用既有归一化链。
     *
     * <p>身份以写保护守卫（{@link ResourceGuard#assertCanEditPlayerProfile}，基于
     * {@code CurrentUserHolder} 的当前登录用户）为准，方法不再接收 userId 形参。
     *
     * @param playerId 目标球员档案 ID
     * @param body     请求体（仅白名单键生效）
     * @return 落库后的球员档案
     */
    @Transactional(rollbackFor={Exception.class})
    public Player updateSelfProfile(Long playerId, Map<String, Object> body) {
        this.resourceGuard.assertCanEditPlayerProfile(playerId, ResourceGuard.PlayerEditChannel.SELF);
        Player p = this.playerRepository.findById(playerId).orElseThrow(() -> new BusinessException(404, "球员不存在"));
        // 与 GET 返 null 一致：软删档案不可编辑。
        if (p.getDeletedAt() != null) {
            throw new BusinessException(404, "球员不存在");
        }
        // 经历（teamEntries）自助白名单：仅当显式提供 List 时才进入；
        // 与既有经历逐条比对，先校验后落库（违规 403 发生在任何写之前）。
        List<PlayerTeamEntryDto> incomingEntries = null;
        if (body != null && body.containsKey("teamEntries")) {
            // M-2：显式提供 teamEntries 即进入解析（非 List 由 parseSelfTeamEntries 抛 400），消除旧 instanceof 死分支/静默忽略。
            incomingEntries = PlayerService.parseSelfTeamEntries(body.get("teamEntries"));
            this.validateSelfTeamEntriesMutation(p, incomingEntries);
        }
        if (body != null) {
            for (Map.Entry<String, Object> e : body.entrySet()) {
                if (!SELF_EDITABLE.contains(e.getKey())) continue;
                this.applySelfEditableField(p, e.getKey(), e.getValue());
            }
        }
        PlayerService.normalizeBlankStringsToNull(p);
        PlayerService.normalizePlayerBackgroundFields(p);
        if (incomingEntries == null) {
            // 兼容 3a：body 无 teamEntries 时行为完全不变，仅按白名单编辑字段。
            return this.playerRepository.save(p);
        }
        // 自助经历流水线：plan → 删除守卫（先校验）→ 镜像 → 落库 → 保存计划 → 沿革 → 回写 → 删除审计。
        PlayerTeamService.PlayerTeamSyncPlan plan = this.playerTeamService.plan(p, incomingEntries, true);
        this.assertEntriesDeletable(playerId, plan.toDelete());
        this.playerTeamService.applyMirror(p, plan);
        Player saved = this.playerRepository.save(p);
        this.playerTeamService.persistPlan(saved.getId(), plan);
        Long joinRecordId = this.personnelHistoryRecorder.recordPlayerTeamTransitions(saved, plan.beforeCurrentTeamIds(), plan.afterCurrentTeamIds());
        if (joinRecordId != null) {
            saved.setCurrentJoinRecordId(joinRecordId);
            saved = this.playerRepository.save(saved);
        }
        for (PlayerTeam removedEntry : plan.toDelete()) {
            this.personnelHistoryRecorder.recordPlayerTeamEntryRemoval(saved.getId(), saved.getTenantId(), removedEntry);
        }
        return saved;
    }

    /**
     * 删除守卫（管理端与自助两路径共用，亦供邀请入队防御性复用）：对“将被删除”的经历逐条校验；
     * 若该“球员+球队”已有有效比赛记录（口径与统计一致），抛 400 阻断删除，改为引导“取消当前球队”。
     */
    void assertEntriesDeletable(Long playerId, List<PlayerTeam> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            return;
        }
        for (PlayerTeam entry : toDelete) {
            long count = this.gamePlayerStatRepository.countValidByPlayerIdAndTeamId(playerId, entry.getTeamId());
            if (count > 0L) {
                throw new BusinessException(400, "该经历已参加 " + count + " 场比赛，不可删除；离队请取消当前球队");
            }
        }
    }

    /**
     * 球员删除权收窄（2026-10-09）：删除/批量删除仅限超级管理员/租户管理员；
     * 球队管理员/赛事主办方请使用「移除出球队」（POST /player/remove-from-team/:id）。
     */
    private void assertRoleCanDeletePlayer() {
        if (!this.resourceGuard.isCurrentUserSuperAdmin() && !this.resourceGuard.isCurrentUserTenantAdmin()) {
            throw new BusinessException(403, "删除球员仅限租户管理员/超级管理员；球队侧请使用「移除出球队」");
        }
    }

    /**
     * 自助经历变更白名单：仅放行 ① 本人经历 current true→false；② 删除无比赛记录经历（守卫另处）。
     * 新增行 / 复活 / current→true / 改 number / 改 positions 一律 403 并给出明确指引。
     */
    private void validateSelfTeamEntriesMutation(Player p, List<PlayerTeamEntryDto> incoming) {
        List<PlayerTeam> existing = this.playerTeamRepository
                .findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(p.getId());
        LinkedHashMap<Long, PlayerTeam> existingByTeam = new LinkedHashMap<>();
        for (PlayerTeam e : existing) {
            existingByTeam.put(e.getTeamId(), e);
        }
        // M-1：自去重——incoming 重复 teamId 直接 400（与 plan 端文案对齐），消除 400/403 漂移。
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        for (PlayerTeamEntryDto dto : incoming) {
            Long teamId = dto.teamId();
            if (teamId == null || teamId <= 0L) {
                throw new BusinessException(400, "球队经历缺少有效球队 ID（teamId）");
            }
            if (!seen.add(teamId)) {
                throw new BusinessException(400, "球队经历存在重复球队：ID " + teamId);
            }
            PlayerTeam current = existingByTeam.get(teamId);
            if (current == null) {
                boolean revived = this.playerTeamRepository
                        .findFirstByPlayerIdAndTeamIdAndDeletedAtIsNotNullOrderByIdDesc(p.getId(), teamId)
                        .isPresent();
                throw new BusinessException(403, revived ? "重新入队请通过球队邀请" : "新增球队经历请通过球队邀请");
            }
            boolean wantCurrent = dto.current() != null && dto.current();
            if (wantCurrent && !Boolean.TRUE.equals(current.getCurrent())) {
                throw new BusinessException(403, "重新入队请通过球队邀请");
            }
            String newNumber = dto.number() == null || dto.number().isBlank() ? null : dto.number().trim();
            if (!Objects.equals(current.getNumber(), newNumber)) {
                throw new BusinessException(403, "背号/守位请通过球队管理");
            }
            List<String> newPositions = dto.positions() == null ? List.of() : dto.positions();
            if (!Objects.equals(current.getPositionsList(), newPositions)) {
                throw new BusinessException(403, "背号/守位请通过球队管理");
            }
        }
    }

    /** 解析自助请求体中的 teamEntries（List<Map>）为经历 DTO 列表；非数组结构 → 400。 */
    private static List<PlayerTeamEntryDto> parseSelfTeamEntries(Object raw) {
        if (!(raw instanceof List<?> list)) {
            throw new BusinessException(400, "球队经历格式不正确");
        }
        ArrayList<PlayerTeamEntryDto> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                throw new BusinessException(400, "球队经历格式不正确");
            }
            out.add(new PlayerTeamEntryDto(
                    PlayerService.asLong(m.get("id")),
                    PlayerService.asLong(m.get("teamId")),
                    m.get("teamName") == null ? null : String.valueOf(m.get("teamName")),
                    m.get("number") == null ? null : String.valueOf(m.get("number")),
                    PlayerService.asStringList(m.get("positions")),
                    PlayerService.asBoolean(m.get("current")),
                    PlayerService.asInteger(m.get("sort"))));
        }
        return out;
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer asInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * M-3：current 取值严格化——Boolean 原样；Number 明确 {@code intValue()!=0}；字符串仅接受 true/false
     * （忽略大小写 + trim）；其余（含 "1" 等）抛 400，消除 {@code Boolean.valueOf("1") == false} 的静默降级。
     */
    private static Boolean asBoolean(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        if ("true".equalsIgnoreCase(s)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(s)) return Boolean.FALSE;
        throw new BusinessException(400, "字段取值非法：current");
    }

    private static List<String> asStringList(Object v) {
        if (!(v instanceof List<?> raw)) {
            return null;
        }
        ArrayList<String> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o != null) out.add(String.valueOf(o));
        }
        return out;
    }

    /** 按白名单键将请求体原始值适配到 Player 字段（组图 / JSON 字段走既有 JSON/归一化工具）。 */
    private void applySelfEditableField(Player p, String key, Object value) {
        switch (key) {
            case "height" -> PlayerService.applyScalarText(value, p::setHeight);
            case "weight" -> PlayerService.applyScalarText(value, p::setWeight);
            case "throwHand" -> PlayerService.applyScalarText(value, p::setThrowHand);
            case "batHand" -> PlayerService.applyScalarText(value, p::setBatHand);
            case "avatar" -> PlayerService.applyScalarText(value, p::setAvatar);
            case "bgImage" -> PlayerService.applyScalarText(value, p::setBgImage);
            case "nickname" -> PlayerService.applyScalarText(value, p::setNickname);
            case "nameEn" -> PlayerService.applyScalarText(value, p::setNameEn);
            case "birthDate" -> PlayerService.applyScalarText(value, p::setBirthDate);
            case "birthPlace" -> PlayerService.applyScalarText(value, p::setBirthPlace);
            case "education" -> PlayerService.applyScalarText(value, p::setEducation);
            case "intro" -> PlayerService.applyScalarText(value, p::setIntro);
            case "contactPhone" -> PlayerService.applyScalarText(value, p::setContactPhone);
            case "contactEmail" -> PlayerService.applyScalarText(value, p::setContactEmail);
            case "draft" -> PlayerService.applyScalarText(value, p::setDraft);
            case "debut" -> PlayerService.applyScalarText(value, p::setDebut);
            case "name" -> PlayerService.applyScalarText(value, p::setName);
            case "positions" -> p.setPositions(PlayerService.asPositionsStorage(value));
            case "bgImages" -> p.setBgImages(PlayerService.asTextList(value));
            case "bgFocusConfig" -> p.setBgFocusConfig(PlayerService.asTextObjectMap(value));
            default -> { }
        }
    }
    
    /**
     * 标量字段写入：值经 {@link #asText} 适配后写入 setter。
     * <p>M-2：对象/数组等“非标量”值传入标量字段时跳过该键（保持原值），
     * 避免写入 {@code String.valueOf(map)} 之类的坏值。
     */
    private static void applyScalarText(Object value, java.util.function.Consumer<String> setter) {
        if (PlayerService.isNonScalar(value)) {
            return;
        }
        setter.accept(PlayerService.asText(value));
    }
    
    /** 非标量判定：Map / 集合 / 数组等结构值，不可写入标量字段。 */
    private static boolean isNonScalar(Object v) {
        return v instanceof Map || v instanceof Collection || v != null && v.getClass().isArray();
    }
    
    /**
     * 标量文本转换（{@code null} 透传以支持清空）；非标量由 {@link #applyScalarText} 提前跳过，
     * 此处对非标量返回 {@code null} 兜底，确保绝不产出坏值。
     */
    private static String asText(Object v) {
        return v == null || PlayerService.isNonScalar(v) ? null : String.valueOf(v);
    }

    private static List<String> asTextList(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof List<?> raw) {
            ArrayList<String> out = new ArrayList<String>();
            for (Object o : raw) {
                if (o != null) out.add(String.valueOf(o));
            }
            return out;
        }
        if (v instanceof String s) {
            List<String> parsed = PositionsJsonUtil.parseList(s);
            if (!parsed.isEmpty()) {
                return parsed;
            }
            return s.isBlank() ? List.of() : List.of(s);
        }
        return List.of(String.valueOf(v));
    }

    private static Map<String, Object> asTextObjectMap(Object v) {
        if (!(v instanceof Map<?, ?> raw)) {
            return null;
        }
        HashMap<String, Object> out = new HashMap<String, Object>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    private static String asPositionsStorage(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof List<?> raw) {
            ArrayList<String> out = new ArrayList<String>();
            for (Object o : raw) {
                if (o != null) out.add(String.valueOf(o));
            }
            return PositionsJsonUtil.toStorage(out);
        }
        return String.valueOf(v);
    }

    private static void normalizeBlankStringsToNull(Player p) {
        if (p == null) {
            return;
        }
        if (p.getName() != null && p.getName().isBlank()) {
            p.setName(null);
        }
        if (p.getShortName() != null && p.getShortName().isBlank()) {
            p.setShortName(null);
        }
        if (p.getNameEn() != null && p.getNameEn().isBlank()) {
            p.setNameEn(null);
        }
        if (p.getNickname() != null && p.getNickname().isBlank()) {
            p.setNickname(null);
        }
        if (p.getNumber() != null && p.getNumber().isBlank()) {
            p.setNumber(null);
        }
        if (p.getPositions() != null && p.getPositions().isBlank()) {
            p.setPositions(null);
        }
        if (p.getAvatar() != null && p.getAvatar().isBlank()) {
            p.setAvatar(null);
        }
        if (p.getBgImage() != null && p.getBgImage().isBlank()) {
            p.setBgImage(null);
        }
        if (p.getBgFocusConfig() != null && p.getBgFocusConfig().isEmpty()) {
            p.setBgFocusConfig(null);
        }
        if (p.getBgImages() != null && p.getBgImages().isEmpty()) {
            p.setBgImages(null);
        }
        if (p.getBirthDate() != null && p.getBirthDate().isBlank()) {
            p.setBirthDate(null);
        }
        if (p.getBirthPlace() != null && p.getBirthPlace().isBlank()) {
            p.setBirthPlace(null);
        }
        if (p.getHeight() != null && p.getHeight().isBlank()) {
            p.setHeight(null);
        }
        if (p.getWeight() != null && p.getWeight().isBlank()) {
            p.setWeight(null);
        }
        if (p.getThrowHand() != null && p.getThrowHand().isBlank()) {
            p.setThrowHand(null);
        }
        if (p.getBatHand() != null && p.getBatHand().isBlank()) {
            p.setBatHand(null);
        }
        if (p.getDraft() != null && p.getDraft().isBlank()) {
            p.setDraft(null);
        }
        if (p.getDebut() != null && p.getDebut().isBlank()) {
            p.setDebut(null);
        }
        if (p.getEducation() != null && p.getEducation().isBlank()) {
            p.setEducation(null);
        }
        if (p.getStatus() != null && p.getStatus().isBlank()) {
            p.setStatus(null);
        }
        if (p.getContactPhone() != null && p.getContactPhone().isBlank()) {
            p.setContactPhone(null);
        }
        if (p.getContactEmail() != null && p.getContactEmail().isBlank()) {
            p.setContactEmail(null);
        }
        if (p.getIntro() != null && p.getIntro().isBlank()) {
            p.setIntro(null);
        }
    }

    private static void normalizePlayerBackgroundFields(Player p) {
        if (p == null) {
            return;
        }
        if (p.getBgImages() != null && !p.getBgImages().isEmpty()) {
            ArrayList<String> normalized = new ArrayList<String>();
            HashSet<String> seen = new HashSet<String>();
            for (String u : p.getBgImages()) {
                String t;
                if (u == null || u.isBlank() || !seen.add(t = u.trim())) continue;
                normalized.add(t);
                if (normalized.size() < 5) continue;
                break;
            }
            if (normalized.isEmpty()) {
                p.setBgImages(null);
                p.setBgImage(null);
                return;
            }
            p.setBgImages(normalized);
            p.setBgImage((String)normalized.get(0));
            return;
        }
        if (p.getBgImage() != null && !p.getBgImage().isBlank()) {
            p.setBgImages(List.of(p.getBgImage().trim()));
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        this.assertRoleCanDeletePlayer();
        Player existing = this.playerRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        if (!Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u7403\u5458");
        }
        this.resourceGuard.assertCanEditPlayerProfile(id, ResourceGuard.PlayerEditChannel.ROSTER);
        if (!this.resourceGuard.isCurrentUserSuperAdmin()) {
            // 历史数据处置权（spec §6.10）：非超管删除 = 归还（软删 + 平台资产标记），不改 tenant_id
            existing.setPlatformOwned(Boolean.TRUE);
        }
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        existing.setDeletedAt(now);
        existing.setDeletedBy(uid);
        this.playerRepository.save(existing);
    }

    @Transactional(rollbackFor={Exception.class})
    public void deleteBatch(List<Long> ids) {
        this.assertRoleCanDeletePlayer();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> validIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (validIds.isEmpty()) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        List<Player> toSoftDelete = this.playerRepository.findAllById(validIds).stream().filter(p -> Objects.equals(p.getTenantId(), tid)).filter(p -> p.getDeletedAt() == null).toList();
        boolean superAdmin = this.resourceGuard.isCurrentUserSuperAdmin();
        for (Player p2 : toSoftDelete) {
            this.resourceGuard.assertCanEditPlayerProfile(p2.getId(), ResourceGuard.PlayerEditChannel.ROSTER);
            if (!superAdmin) {
                // 历史数据处置权（spec §6.10）：非超管删除 = 归还（软删 + 平台资产标记），不改 tenant_id
                p2.setPlatformOwned(Boolean.TRUE);
            }
            p2.setDeletedAt(now);
            p2.setDeletedBy(uid);
        }
        this.playerRepository.saveAll(toSoftDelete);
    }

    /**
     * 恢复已删除球员（2026-10-09）：仅超管/租户管理员；租管限本租户。
     * 清 deletedAt/deletedBy 并还原 platformOwned（归还标记撤销），球员回到正常列表。
     */
    @Transactional(rollbackFor={Exception.class})
    public void restore(Long id) {
        boolean superAdmin = this.resourceGuard.isCurrentUserSuperAdmin();
        if (!superAdmin && !this.resourceGuard.isCurrentUserTenantAdmin()) {
            throw new BusinessException(403, "仅租户管理员/超级管理员可恢复球员");
        }
        Player existing = this.playerRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() == null) {
            throw new BusinessException(400, "该球员未被删除");
        }
        if (!superAdmin && !Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "无权恢复该球员");
        }
        existing.setDeletedAt(null);
        existing.setDeletedBy(null);
        existing.setPlatformOwned(Boolean.FALSE);
        this.playerRepository.save(existing);
    }

    /**
     * 移除球员出球队（2026-10-09）：球队管理员（自有球队）/赛事主办方（域内）与租管/超管可用。
     * 语义：无有效比赛记录 → 删除该球队经历（写删经历沿革）；有记录 → 取消当前球队（current=false，写离队沿革）。
     * 不限制认领状态（确认口径：已认领球员同样可被移除）。
     */
    @Transactional(rollbackFor={Exception.class})
    public void removeFromTeam(Long playerId, Long teamId) {
        if (teamId == null || teamId <= 0L) {
            throw new BusinessException(400, "缺少球队 ID");
        }
        Player player = this.playerRepository.findById(playerId).orElse(null);
        if (player == null || player.getDeletedAt() != null) {
            throw new BusinessException(404, "球员不存在");
        }
        boolean superAdmin = this.resourceGuard.isCurrentUserSuperAdmin();
        if (!superAdmin && !Objects.equals(player.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "无权移除该球员");
        }
        Team team = this.teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            throw new BusinessException(400, "球队不存在");
        }
        EffectiveScope s = this.accountScopeService.resolveCurrent();
        boolean allowed = s.isUnrestrictedInTenant()
                || s.canManageTeam(teamId)
                || team.getLeagueId() != null && s.canManageLeague(team.getLeagueId());
        if (!allowed) {
            throw new BusinessException(403, "无权将该球员移出该球队");
        }
        List<PlayerTeam> existing = this.playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(playerId);
        PlayerTeam target = existing.stream()
                .filter(e -> Objects.equals(e.getTeamId(), teamId) && Boolean.TRUE.equals(e.getCurrent()))
                .findFirst().orElse(null);
        if (target == null) {
            throw new BusinessException(400, "该球员当前不在该球队");
        }
        long played = this.gamePlayerStatRepository.countValidByPlayerIdAndTeamId(playerId, teamId);
        ArrayList<PlayerTeamEntryDto> incoming = new ArrayList<PlayerTeamEntryDto>();
        for (PlayerTeam e : existing) {
            boolean isTarget = Objects.equals(e.getTeamId(), teamId);
            if (isTarget && played <= 0L) {
                continue;
            }
            Boolean cur = isTarget ? Boolean.FALSE : Boolean.valueOf(Boolean.TRUE.equals(e.getCurrent()));
            incoming.add(new PlayerTeamEntryDto(e.getId(), e.getTeamId(), null, e.getNumber(), e.getPositionsList(), cur, e.getSort()));
        }
        PlayerTeamService.PlayerTeamSyncPlan plan = this.playerTeamService.plan(player, incoming, true);
        this.playerTeamService.applyMirror(player, plan);
        Player saved = this.playerRepository.save(player);
        this.playerTeamService.persistPlan(saved.getId(), plan);
        Long joinRecordId = this.personnelHistoryRecorder.recordPlayerTeamTransitions(saved, plan.beforeCurrentTeamIds(), plan.afterCurrentTeamIds());
        if (joinRecordId != null) {
            saved.setCurrentJoinRecordId(joinRecordId);
            this.playerRepository.save(saved);
        }
        for (PlayerTeam removed : plan.toDelete()) {
            this.personnelHistoryRecorder.recordPlayerTeamEntryRemoval(saved.getId(), saved.getTenantId(), removed);
        }
    }

    /**
     * 已删除球员分页（2026-10-09）：仅超管/租户管理员。超管全局模式跨租户，租管限本租户。
     * 支持 keyword（姓名/英文名/简称/昵称模糊）；固定按 deletedAt 倒序。
     */
    public PageResult<Player> listDeleted(Integer page, Integer pageSize, String keyword) {
        if (!this.resourceGuard.isCurrentUserSuperAdmin() && !this.resourceGuard.isCurrentUserTenantAdmin()) {
            throw new BusinessException(403, "仅租户管理员/超级管理员可查看已删除球员");
        }
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        boolean global = this.tenantQueryPolicyService.isGlobalQueryMode();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Specification<Player> spec = (root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNotNull((Expression)root.get("deletedAt")));
            if (!global) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)Long.valueOf(tid)));
            }
            if (kw != null) {
                String like = "%" + kw + "%";
                preds.add(cb.or(cb.like((Expression)root.get("name"), like), cb.like((Expression)root.get("nameEn"), like),
                        cb.like((Expression)root.get("shortName"), like), cb.like((Expression)root.get("nickname"), like)));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Pageable pg = PageRequest.of(p - 1, ps, Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Player> result = this.playerRepository.findAll(spec, pg);
        List<Player> content = result.getContent();
        this.playerTeamService.attachEntries(content);
        return PageResult.of((List)content, (long)result.getTotalElements());
    }

    private void validateTeamId(Long teamId) {
        if (teamId != null && teamId > 0L && !this.teamRepository.existsById(teamId)) {
            throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u521b\u5efa\u7403\u961f");
        }
    }

    private void applyTenantFromTeam(Player entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (entity.getTeamId() != null && entity.getTeamId() > 0L) {
            Team team = this.teamRepository.findById(entity.getTeamId()).orElse(null);
            if (team == null) {
                throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u521b\u5efa\u7403\u961f");
            }
            if (!Objects.equals(team.getTenantId(), tid)) {
                throw new BusinessException(400, "\u7403\u961f\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
            entity.setTenantId(team.getTenantId());
        } else {
            entity.setTenantId(Long.valueOf(tid));
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public Map<String, Object> batchImport(List<Player> items, String duplicateStrategy) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Set<Long> teamIds = items.stream().map(Player::getTeamId).filter(Objects::nonNull).filter(id -> id > 0L).collect(Collectors.toSet());
        HashSet<Long> invalidTeamIds = new HashSet<Long>();
        for (Long x : teamIds) {
            Team t = this.teamRepository.findById(x).orElse(null);
            if (t != null && Objects.equals(t.getTenantId(), tid)) continue;
            invalidTeamIds.add(x);
        }
        if (!invalidTeamIds.isEmpty()) {
            throw new BusinessException(400, "\u4ee5\u4e0b\u7403\u961f\u4e0d\u5b58\u5728\u6216\u4e0d\u5c5e\u4e8e\u5f53\u524d\u79df\u6237\uff1aID " + String.valueOf(invalidTeamIds));
        }
        // 批次 3b（spec §6.9）：去重键对齐自助建档口径——同租户 name+birthDate（仅双非空参与）。
        // 命中源①：既有同租户未删除档案（findByDeletedAtIsNullAndTenantId 预载后按新键归一化，
        // 等价于 findByNameAndBirthDateAndTenantIdAndDeletedAtIsNull 的“既有同键档案”语义）。
        HashMap<String, Player> existingByKey = new HashMap<String, Player>();
        for (Player p : this.playerRepository.findByDeletedAtIsNullAndTenantId(tid)) {
            String existingKey = this.dupKey(p);
            if (existingKey != null) {
                existingByKey.put(existingKey, p);
            }
        }
        boolean overwrite = "overwrite".equalsIgnoreCase(duplicateStrategy);
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (Player p : items) {
            if (p.getName() == null || p.getName().isBlank()) {
                ++skipped;
                continue;
            }
            this.applyTenantFromTeam(p);
            // 批次 3b（spec §6.9）：逐行代建守卫——受限身份仅可为本队 / 本联盟域内球队代建（越权整体 403 并事务回滚）。
            this.resourceGuard.assertCanCreateUnclaimedPlayer(p.getTeamId());
            // 命中源①/②统一：键为空（name 或 birthDate 任一为空）不参与去重，照常建档。
            String dupKey = this.dupKey(p);
            Player existing = dupKey != null ? (Player)existingByKey.get(dupKey) : null;
            if (existing != null) {
                if (overwrite) {
                    p.setId(existing.getId());
                    p.setCreatedAt(existing.getCreatedAt());
                    Player savedOverwrite = (Player)this.playerRepository.save(p);
                    this.playerTeamService.syncLegacyEntry(savedOverwrite);
                    ++updated;
                    continue;
                }
                ++skipped;
                continue;
            }
            Player savedNew = (Player)this.playerRepository.save(p);
            this.playerTeamService.syncLegacyEntry(savedNew);
            // 命中源②：本次导入文件内同键行——建后回填，供后续同键行去重。
            if (dupKey != null) {
                existingByKey.put(dupKey, savedNew);
            }
            ++created;
        }
        return Map.of("created",created, "updated",updated, "skipped", (Object)skipped);
    }

    /**
     * 批量代建去重键（批次 3b，spec §6.9）：同租户 {@code name+birthDate}，与自助建档
     * {@link #createSelfProfile} 口径一致——<b>仅当 name 与 birthDate 均非空（trim 后）</b>才构成
     * 去重键；任一为空则返回 {@code null}，该行不参与去重（照常建档）。
     *
     * @param p 待判定球员（导入行或既有档案）
     * @return 归一化去重键 {@code name|birthDate}；name / birthDate 任一为空时为 {@code null}
     */
    private String dupKey(Player p) {
        if (p.getName() == null || p.getName().isBlank()) {
            return null;
        }
        if (p.getBirthDate() == null || p.getBirthDate().isBlank()) {
            return null;
        }
        return p.getName().trim() + "|" + p.getBirthDate().trim();
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps);
    }

    @Generated
    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository, PlayerTeamRepository playerTeamRepository, StatsService statsService, AccountScopeService accountScopeService, ScopeQuerySupport scopeQuerySupport, ResourceGuard resourceGuard, PersonnelHistoryRecorder personnelHistoryRecorder, PlayerTeamService playerTeamService, TenantQueryPolicyService tenantQueryPolicyService, SysConfigService sysConfigService, PlayerClaimRepository playerClaimRepository, GamePlayerStatRepository gamePlayerStatRepository) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.statsService = statsService;
        this.accountScopeService = accountScopeService;
        this.scopeQuerySupport = scopeQuerySupport;
        this.resourceGuard = resourceGuard;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.playerTeamService = playerTeamService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.sysConfigService = sysConfigService;
        this.playerClaimRepository = playerClaimRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
    }
}

