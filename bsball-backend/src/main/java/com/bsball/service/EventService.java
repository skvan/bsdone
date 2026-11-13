/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.Event
 *  com.bsball.model.entity.League
 *  com.bsball.repository.EventRepository
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.service.EventService
 *  com.bsball.service.TenantQueryPolicyService
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.League;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private static final String FORMAT_SINGLE_ROUND_ROBIN = "SINGLE_ROUND_ROBIN";
    private static final Set<String> TOURNAMENT_FORMATS = Set.of(
            FORMAT_SINGLE_ROUND_ROBIN, "DOUBLE_ROUND_ROBIN", "KNOCKOUT", "GROUP_KNOCKOUT");
    private static final Set<Integer> BRACKET_SIZES = Set.of(2, 4, 8, 16, 32);

    private final EventRepository eventRepository;
    private final LeagueRepository leagueRepository;
    private final AccountScopeService accountScopeService;
    private final ScopeQuerySupport scopeQuerySupport;
    private final ResourceGuard resourceGuard;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final GameRepository gameRepository;
    private final LeagueSpecialRuleService leagueSpecialRuleService;
    private final EventSpecialRuleService eventSpecialRuleService;

    public PageResult<Event> list(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        Page result;
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, sortProp, sortOrder);
            Page globalResult = this.eventRepository.findByDeletedAtIsNull(gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleLeagueIds = this.scopeQuerySupport.visibleLeagueIds(scope);
        if (visibleLeagueIds != null && visibleLeagueIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (visibleLeagueIds != null) {
            result = this.eventRepository.findByTenantIdAndLeagueIdInAndDeletedAtIsNull(Long.valueOf(tid), visibleLeagueIds, p);
        } else {
            result = this.eventRepository.findByTenantIdAndDeletedAtIsNull(Long.valueOf(tid), p);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public Event get(Long id) {
        Event entity = this.eventRepository.findById(id).orElse(null);
        if (entity == null || entity.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(entity.getTenantId(), tid)) {
            return null;
        }
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleLeagueIds = this.scopeQuerySupport.visibleLeagueIds(scope);
        // 批次 3b（前置⑤）：管理上下文 + 受限身份下，leagueId 为 null 亦拒绝（与 list 收窄口径一致）；非管理上下文（宽读）不变。
        if (visibleLeagueIds != null && (entity.getLeagueId() == null || !visibleLeagueIds.contains(entity.getLeagueId()))) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u8d5b\u4e8b");
        }
        // 赛事级特殊规则（Issue #275，2026-10-24 口径调整）：随详情响应附带生效值与可用标识。
        // 联赛能力关闭时，已配置规则的赛事仍保留规则；无规则赛事按默认值（extraBatter=false、forceHalf=true、ohtaniRule=false、softballReentry=false）装配。
        boolean hasEventRules = this.eventSpecialRuleService.hasRules(entity.getId());
        Map<String, Boolean> eventRules = hasEventRules
                ? this.eventSpecialRuleService.getEffective(entity.getId())
                : Map.of();
        Map<String, Boolean> specialRules = new LinkedHashMap<>();
        specialRules.put("specialRulesEnabled", this.leagueSpecialRuleService.isSpecialRulesEnabled(entity.getLeagueId()));
        specialRules.put("hasEventRules", hasEventRules);
        specialRules.put("extraBatter", Boolean.TRUE.equals(eventRules.get("extraBatter")));
        specialRules.put("forceHalf", !Boolean.FALSE.equals(eventRules.get("forceHalf")));
        specialRules.put("ohtaniRule", Boolean.TRUE.equals(eventRules.get("ohtaniRule")));
        specialRules.put("softballReentry", Boolean.TRUE.equals(eventRules.get("softballReentry")));
        entity.setSpecialRules(specialRules);
        return entity;
    }

    @Transactional(rollbackFor={Exception.class})
    public Event create(Event entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        this.normalizeTournamentConfig(entity);
        if (entity.getRequireRoster() == null) {
            // 报名制（Issue #318 决策 11）：新建默认 true；显式 null 归一化，防撞 not null 约束
            entity.setRequireRoster(Boolean.TRUE);
        }
        if (entity.getLeagueId() != null) {
            League league = this.leagueRepository.findById(entity.getLeagueId()).orElse(null);
            if (league == null || league.getDeletedAt() != null || !Objects.equals(league.getTenantId(), tid)) {
                throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
            }
            this.resourceGuard.assertCanManageLeague(entity.getLeagueId());
        }
        Map<String, Boolean> rules = entity.getSpecialRules();
        boolean writeRules = rules != null && !rules.isEmpty();
        if (writeRules) {
            Long leagueId = entity.getLeagueId();
            if (leagueId == null || !this.leagueSpecialRuleService.isSpecialRulesEnabled(leagueId)) {
                throw new BusinessException(400, "该联盟未开放特殊规则能力");
            }
        }
        entity.setTenantId(Long.valueOf(tid));
        Event saved = (Event)this.eventRepository.save(entity);
        if (writeRules) {
            this.eventSpecialRuleService.updateRules(saved.getId(), saved.getTenantId(), rules);
        }
        return saved;
    }

    @Transactional(rollbackFor={Exception.class})
    public Event update(Long id, Event entity) {
        League league;
        Long leagueId;
        Event existing = this.eventRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u8d5b\u4e8b");
        }
        Long l = leagueId = entity.getLeagueId() != null ? entity.getLeagueId() : existing.getLeagueId();
        if (!(leagueId == null || (league = (League)this.leagueRepository.findById(leagueId).orElse(null)) != null && league.getDeletedAt() == null && Objects.equals(league.getTenantId(), tid))) {
            throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        this.resourceGuard.assertCanManageEvent(id);
        if (leagueId != null && !leagueId.equals(existing.getLeagueId())) {
            this.resourceGuard.assertCanManageLeague(leagueId);
        }
        // 赛事结束校验（Issue #318 决策 7）：置为 ended 前，须该赛事全部比赛已终态（final/cancelled）
        if ("ended".equals(entity.getStatus()) && !"ended".equals(existing.getStatus())
                && this.gameRepository.countUnfinishedGamesByEvent(id) > 0L) {
            throw new BusinessException(400, "存在未结束的比赛，无法结束赛事");
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        // 报名制标记创建时定型（决策 11：不提供 UI 开关）：编辑一律沿用旧值，
        // 防请求体缺省值（实体默认 TRUE）把存量赛事静默翻成报名制。
        entity.setRequireRoster(existing.getRequireRoster());
        this.mergeTournamentConfig(entity, existing);
        Map<String, Boolean> rules = entity.getSpecialRules();
        boolean writeRules = rules != null && !rules.isEmpty();
        if (writeRules && !this.eventSpecialRuleService.hasRules(id)) {
            if (leagueId == null || !this.leagueSpecialRuleService.isSpecialRulesEnabled(leagueId)) {
                throw new BusinessException(400, "该联盟未开放特殊规则能力");
            }
        }
        Event saved = (Event)this.eventRepository.save(entity);
        if (writeRules) {
            this.eventSpecialRuleService.updateRules(id, entity.getTenantId(), rules);
        }
        return saved;
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Event existing = this.eventRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8d5b\u4e8b");
        }
        this.resourceGuard.assertCanManageEvent(id);
        if (!this.resourceGuard.isCurrentUserSuperAdmin()) {
            // 历史数据处置权（spec §6.10）：非超管删除 = 归还（软删 + 平台资产标记），不改 tenant_id
            existing.setPlatformOwned(Boolean.TRUE);
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.eventRepository.save(existing);
    }

    /**
     * 赛制与积分制默认值归一化（新建赛事）。
     * 存量客户端可只传部分字段；null 一律落到默认值，非法值直接 400。
     */
    private void normalizeTournamentConfig(Event entity) {
        entity.setFormatType(this.normalizeFormatType(entity.getFormatType()));
        entity.setPointsWin(this.normalizePoints(entity.getPointsWin(), 2, "胜场积分"));
        entity.setPointsDraw(this.normalizePoints(entity.getPointsDraw(), 1, "平局积分"));
        entity.setPointsLoss(this.normalizePoints(entity.getPointsLoss(), 0, "负场积分"));
        entity.setGroupCount(this.normalizeGroupCount(entity.getGroupCount()));
        entity.setAdvancePerGroup(this.normalizeAdvancePerGroup(entity.getAdvancePerGroup()));
        entity.setThirdPlace(entity.getThirdPlace() == null ? Boolean.TRUE : entity.getThirdPlace());
        entity.setBracketSize(this.normalizeBracketSize(entity.getBracketSize()));
    }

    /** 编辑赛事时缺省字段沿用存量值，避免旧客户端把赛制配置重置为默认。 */
    private void mergeTournamentConfig(Event entity, Event existing) {
        if (entity.getFormatType() == null) {
            entity.setFormatType(existing.getFormatType());
        }
        if (entity.getPointsWin() == null) {
            entity.setPointsWin(existing.getPointsWin());
        }
        if (entity.getPointsDraw() == null) {
            entity.setPointsDraw(existing.getPointsDraw());
        }
        if (entity.getPointsLoss() == null) {
            entity.setPointsLoss(existing.getPointsLoss());
        }
        if (entity.getGroupCount() == null) {
            entity.setGroupCount(existing.getGroupCount());
        }
        if (entity.getAdvancePerGroup() == null) {
            entity.setAdvancePerGroup(existing.getAdvancePerGroup());
        }
        if (entity.getThirdPlace() == null) {
            entity.setThirdPlace(existing.getThirdPlace());
        }
        if (entity.getBracketSize() == null) {
            entity.setBracketSize(existing.getBracketSize());
        }
        this.normalizeTournamentConfig(entity);
    }

    private String normalizeFormatType(String value) {
        String normalized = value == null || value.isBlank()
                ? FORMAT_SINGLE_ROUND_ROBIN
                : value.trim().toUpperCase(Locale.ROOT);
        if (!TOURNAMENT_FORMATS.contains(normalized)) {
            throw new BusinessException(400, "不支持的赛制：" + value);
        }
        return normalized;
    }

    private int normalizePoints(Integer value, int fallback, String label) {
        int points = value == null ? fallback : value;
        if (points < 0 || points > 99) {
            throw new BusinessException(400, label + "必须在 0-99 之间");
        }
        return points;
    }

    private int normalizeGroupCount(Integer value) {
        int count = value == null ? 2 : value;
        if (count < 1 || count > 8) {
            throw new BusinessException(400, "小组数必须在 1-8 之间");
        }
        return count;
    }

    private int normalizeAdvancePerGroup(Integer value) {
        int count = value == null ? 2 : value;
        if (count < 1 || count > 4) {
            throw new BusinessException(400, "每组晋级数必须在 1-4 之间");
        }
        return count;
    }

    private int normalizeBracketSize(Integer value) {
        int size = value == null ? 8 : value;
        if (!BRACKET_SIZES.contains(size)) {
            throw new BusinessException(400, "签表规模必须是 2/4/8/16/32");
        }
        return size;
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
    public EventService(EventRepository eventRepository, LeagueRepository leagueRepository, AccountScopeService accountScopeService, ScopeQuerySupport scopeQuerySupport, ResourceGuard resourceGuard, TenantQueryPolicyService tenantQueryPolicyService, GameRepository gameRepository, LeagueSpecialRuleService leagueSpecialRuleService, EventSpecialRuleService eventSpecialRuleService) {
        this.eventRepository = eventRepository;
        this.leagueRepository = leagueRepository;
        this.accountScopeService = accountScopeService;
        this.scopeQuerySupport = scopeQuerySupport;
        this.resourceGuard = resourceGuard;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.gameRepository = gameRepository;
        this.leagueSpecialRuleService = leagueSpecialRuleService;
        this.eventSpecialRuleService = eventSpecialRuleService;
    }
}

