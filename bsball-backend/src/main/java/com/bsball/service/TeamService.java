/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.TeamOptionDto
 *  com.bsball.model.entity.League
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.bsball.service.TeamService
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
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.dto.TeamOptionDto;
import com.bsball.model.entity.League;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final AccountScopeService accountScopeService;
    private final ScopeQuerySupport scopeQuerySupport;
    private final ResourceGuard resourceGuard;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final TeamManagerRepository teamManagerRepository;
    private final ApiPermissionService apiPermissionService;
    private final GameRepository gameRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamService playerTeamService;

    public PageResult<Team> list(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        Page result;
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, sortProp, sortOrder);
            Page globalResult = this.teamRepository.findByDeletedAtIsNull(gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (visibleTeamIds != null && visibleTeamIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (visibleTeamIds != null) {
            result = this.teamRepository.findByTenantIdAndIdInAndDeletedAtIsNull(Long.valueOf(tid), visibleTeamIds, p);
        } else {
            result = this.teamRepository.findByTenantIdAndDeletedAtIsNull(Long.valueOf(tid), p);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public List<TeamOptionDto> listForSelect() {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            return this.teamRepository.findAllForSelect();
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (visibleTeamIds == null) {
            return this.teamRepository.findForSelectByTenantId(Long.valueOf(tid));
        }
        if (visibleTeamIds.isEmpty()) {
            return List.of();
        }
        return this.teamRepository.findForSelectByTenantIdAndIdIn(Long.valueOf(tid), visibleTeamIds);
    }

    public Team get(Long id) {
        Team t = this.teamRepository.findById(id).orElse(null);
        if (t == null || t.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(t.getTenantId(), tid)) {
            return null;
        }
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleTeamIds = this.scopeQuerySupport.visibleTeamIds(scope, tid);
        if (visibleTeamIds != null && !visibleTeamIds.contains(id)) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7403\u961f");
        }
        return t;
    }

    /**
     * 门户自助创建球队即授职（批次 3a，Task 3.5）：保存球队后，若当前登录用户为门户账号
     * （非超管 / 非租管），则把其落为球队负责人（bs_team_manager 落 active 行，仅防御性判存），
     * 并失效其范围缓存（后置提交）。带联盟归属（leagueId != null）时先校验其对该联盟的管理权；
     * 超管 / 租管 / 未登录（uid == null）不落 team_manager。
     *
     * <p>事务性：整体 {@code @Transactional}（对齐批 3 自助创建链路），联盟守卫 / 关系落库异常时
     * 整体回滚，不会遗留 orphan 球队。
     */
    @Transactional(rollbackFor = Exception.class)
    public Team create(Team entity) {
        TeamService.normalizeBlankStringsToNull((Team)entity);
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getLeagueId() != null) {
            League league = this.leagueRepository.findById(entity.getLeagueId()).orElse(null);
            if (league == null || league.getDeletedAt() != null) {
                throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728");
            }
            if (!Objects.equals(league.getTenantId(), tid)) {
                throw new BusinessException(400, "\u8054\u76df\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
        }
        Team saved = (Team)this.teamRepository.save(entity);
        Long uid = CurrentUserHolder.get();
        if (uid != null && !this.apiPermissionService.isSuperAdmin(uid) && !this.apiPermissionService.isTenantAdmin(uid)) {
            if (saved.getLeagueId() != null) {
                this.resourceGuard.assertCanManageLeague(saved.getLeagueId());
            }
            // 防御性判存：create 路径球队为新 id，判存必空；此处用于既有球队复用 / 防重的防御，而非严格幂等。
            if (this.teamManagerRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(saved.getId(), uid).isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                TeamManager tm = new TeamManager();
                tm.setTenantId(saved.getTenantId());
                tm.setTeamId(saved.getId());
                tm.setUserId(uid);
                tm.setStatus(TeamManager.STATUS_ACTIVE);
                tm.setCreatedAt(now);
                tm.setUpdatedAt(now);
                this.teamManagerRepository.save(tm);
            }
            this.accountScopeService.evictUserScopeCacheAfterCommit(uid);
        }
        return saved;
    }

    public Team update(Long id, Team entity) {
        Team existing = this.teamRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u7403\u961f");
        }
        this.resourceGuard.assertCanManageTeam(id);
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        TeamService.normalizeBlankStringsToNull((Team)entity);
        if (entity.getLeagueId() != null) {
            League league = this.leagueRepository.findById(entity.getLeagueId()).orElse(null);
            if (league == null || league.getDeletedAt() != null) {
                throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728");
            }
            if (!Objects.equals(league.getTenantId(), tid)) {
                throw new BusinessException(400, "\u8054\u76df\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
        }
        Team before = PersonnelHistoryRecorder.snapshotTeam((Team)existing);
        Team saved = (Team)this.teamRepository.save(entity);
        this.personnelHistoryRecorder.afterTeamUpdate(before, saved);
        return saved;
    }

    /**
     * 球队解散（批次 3b，Task 3.11 / spec §6.7）：由“软删一行”升级为完整解散语义。
     * <p>顺序：幂等返回 → 租户校验（跨租户 403）→ ResourceGuard 写权限 → <b>解散守卫</b>（存在未开打比赛则 400）
     * → <b>批量球员离队</b>（复用 PlayerTeamService 流水线，逐球员置解散队经历 current=false、产生 leave 沿革、镜像重算）
     * → <b>负责人指派失效</b>（active → inactive + 软删 + 逐人失效范围缓存）→ 软删球队 + <b>解散沿革</b>。
     * <p>守卫口径：以 Game 的 {@code status} 为准，非 {@code final}/{@code cancelled} 视为“未开打/未完成”。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Team existing = this.teamRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u7403\u961f");
        }
        this.resourceGuard.assertCanManageTeam(id);
        if (this.gameRepository.countPendingGamesByTeamId(id) > 0L) {
            throw new BusinessException(400, "\u5b58\u5728\u672a\u5f00\u6253\u7684\u6bd4\u8d5b\uff0c\u8bf7\u5148\u5904\u7406\u8d5b\u7a0b");
        }
        this.dissolveTeamPlayers(existing);
        this.deactivateTeamManagers(existing);
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.teamRepository.save(existing);
        this.personnelHistoryRecorder.afterTeamDissolve(existing);
    }

    /**
     * 解散级联（b）：该队所有 {@code current = true} 经历的球员批量离队。
     * 复用 {@link PlayerTeamService} 流水线：desired 保留既有全部经历，仅将“解散队”经历置 current=false（其余原样），
     * 因此 {@code toDelete} 恒空、不产生静默软删；沿革经 {@code recordPlayerTeamTransitions} 产生 leave；
     * 镜像重算（无其他当前队 → 自由球员）。
     */
    private void dissolveTeamPlayers(Team team) {
        List<PlayerTeam> currentEntries = this.playerTeamRepository.findCurrentEntriesByTeamId(team.getId());
        if (currentEntries.isEmpty()) {
            return;
        }
        List<Long> playerIds = currentEntries.stream()
                .map(PlayerTeam::getPlayerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (playerIds.isEmpty()) {
            return;
        }
        for (Player player : this.playerRepository.findByDeletedAtIsNullAndIdIn(playerIds)) {
            this.dissolvePlayerFromTeam(player, team.getId());
        }
    }

    private void dissolvePlayerFromTeam(Player player, Long teamId) {
        List<PlayerTeam> existing = this.playerTeamRepository
                .findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(player.getId());
        List<PlayerTeamEntryDto> desired = new ArrayList<>(existing.size());
        for (PlayerTeam e : existing) {
            boolean stillCurrent = Boolean.TRUE.equals(e.getCurrent())
                    && !Objects.equals(e.getTeamId(), teamId);
            desired.add(new PlayerTeamEntryDto(e.getId(), e.getTeamId(), null, e.getNumber(),
                    e.getPositionsList(), stillCurrent, e.getSort()));
        }
        PlayerTeamService.PlayerTeamSyncPlan plan = this.playerTeamService.plan(player, desired, true);
        this.playerTeamService.applyMirror(player, plan);
        Player saved = (Player)this.playerRepository.save(player);
        this.playerTeamService.persistPlan(saved.getId(), plan);
        Long joinRecordId = this.personnelHistoryRecorder.recordPlayerTeamTransitions(
                saved, plan.beforeCurrentTeamIds(), plan.afterCurrentTeamIds());
        if (joinRecordId != null) {
            saved.setCurrentJoinRecordId(joinRecordId);
            this.playerRepository.save(saved);
        }
    }

    /** 解散级联（c）：该队 active 负责人指派置 inactive + 软删，逐人记失效沿革 + 失效其范围缓存（后置提交）。 */
    private void deactivateTeamManagers(Team team) {
        List<TeamManager> actives = this.teamManagerRepository
                .findByTeamIdAndStatusAndDeletedAtIsNull(team.getId(), TeamManager.STATUS_ACTIVE);
        if (actives.isEmpty()) {
            return;
        }
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        for (TeamManager tm : actives) {
            tm.setStatus(TeamManager.STATUS_INACTIVE);
            tm.setDeletedAt(now);
            tm.setDeletedBy(uid);
            this.teamManagerRepository.save(tm);
            if (tm.getUserId() != null) {
                this.personnelHistoryRecorder.recordTeamManagerRemoved(
                        team.getId(), team.getTenantId(), tm.getUserId());
                this.accountScopeService.evictUserScopeCacheAfterCommit(tm.getUserId());
            }
        }
    }

    private static void normalizeBlankStringsToNull(Team t) {
        if (t == null) {
            return;
        }
        if (t.getName() != null && t.getName().isBlank()) {
            t.setName(null);
        }
        if (t.getNameEn() != null && t.getNameEn().isBlank()) {
            t.setNameEn(null);
        }
        if (t.getShortName() != null && t.getShortName().isBlank()) {
            t.setShortName(null);
        }
        if (t.getLogo() != null && t.getLogo().isBlank()) {
            t.setLogo(null);
        }
        if (t.getWordmark() != null && t.getWordmark().isBlank()) {
            t.setWordmark(null);
        }
        if (t.getBgImage() != null && t.getBgImage().isBlank()) {
            t.setBgImage(null);
        }
        if (t.getCity() != null && t.getCity().isBlank()) {
            t.setCity(null);
        }
        if (t.getStadium() != null && t.getStadium().isBlank()) {
            t.setStadium(null);
        }
        if (t.getDescription() != null && t.getDescription().isBlank()) {
            t.setDescription(null);
        }
        if (t.getContactPhone() != null && t.getContactPhone().isBlank()) {
            t.setContactPhone(null);
        }
        if (t.getContactEmail() != null && t.getContactEmail().isBlank()) {
            t.setContactEmail(null);
        }
        if (t.getContactPerson() != null && t.getContactPerson().isBlank()) {
            t.setContactPerson(null);
        }
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
    public TeamService(TeamRepository teamRepository, LeagueRepository leagueRepository, AccountScopeService accountScopeService, ScopeQuerySupport scopeQuerySupport, ResourceGuard resourceGuard, PersonnelHistoryRecorder personnelHistoryRecorder, TenantQueryPolicyService tenantQueryPolicyService, TeamManagerRepository teamManagerRepository, ApiPermissionService apiPermissionService, GameRepository gameRepository, PlayerTeamRepository playerTeamRepository, PlayerRepository playerRepository, PlayerTeamService playerTeamService) {
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
        this.accountScopeService = accountScopeService;
        this.scopeQuerySupport = scopeQuerySupport;
        this.resourceGuard = resourceGuard;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.teamManagerRepository = teamManagerRepository;
        this.apiPermissionService = apiPermissionService;
        this.gameRepository = gameRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.playerRepository = playerRepository;
        this.playerTeamService = playerTeamService;
    }
}

