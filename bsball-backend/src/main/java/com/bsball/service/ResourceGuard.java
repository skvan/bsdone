/*
 * 账号权限重构（批次 2）：统一写保护组件（ResourceGuard）。
 *
 * 职责：为写路径提供统一的数据级 403 / 404 判定，供 T2.4 十项服务接线消费。
 *  - 语义铁律：只认「租户内不受限 ∪ 集合命中」，绝不读 guest 读标志（读语义不参与写判定）；
 *  - 全部拒绝均抛 BusinessException(403, "...")（文案逐字对齐 spec §5.5 表）；
 *  - 资源 → 维度映射：联盟=leagueIds；赛事=事件所属联盟；比赛=经赛事所属联盟；
 *    球队=teamIds；球员=TEAM（当前经历）或 SELF（本人 userId）；认领=当前球队 ∈ teamIds。
 *
 * 错误边界（有意设计）：
 *  - 赛事 / 比赛链路：对「不存在 / 缺联盟 / 越权」统一 403（隐藏存在性；文案不区分原因，排障依赖日志上下文）；
 *  - 球员档案 / 认领链路：对「球员不存在」（含 playerId 为空）返回 404（文案逐字「球员不存在」）。
 *
 * 批次 3b（spec §6.9）扩展：球员档案写分档（已认领 → 上级只读 403；未认领 → 本队可写）
 * 与球员「代建」守卫 assertCanCreateUnclaimedPlayer。
 *
 * Issue #318（球队去联盟归属）：球队维度写保护收敛为「不受限 ∪ 命中自有球队集合」——
 * 原「联盟接管 / 代管无主」分支与 assertCanStewardOrManageTeam 一并移除（与 assertCanManageTeam 同义合并）。
 */
package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaim;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.PlayerRepository;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class ResourceGuard {

    /** 球员档案编辑通道：本人（SELF）或球队赛务（ROSTER）。 */
    public enum PlayerEditChannel { SELF, ROSTER }

    private final AccountScopeService accountScopeService;
    private final EventRepository eventRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamService playerTeamService;
    private final ApiPermissionService apiPermissionService;

    /** 联盟写保护：受限身份需命中自有联盟集合。 */
    public void assertCanManageLeague(Long leagueId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (leagueId == null || !s.canManageLeague(leagueId)) throw new BusinessException(403, "无权管理该联盟");
    }

    /**
     * 球队写保护（Issue #318 起：接管/代管与直接管理收敛为同一判定）：
     * 受限身份需命中自有球队集合（含「创建即负责」落下的 team_manager 关系）；无主球队由租管/超管指派负责人。
     */
    public void assertCanManageTeam(Long teamId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (teamId == null || !s.canManageTeam(teamId)) throw new BusinessException(403, "无权管理该球队");
    }

    /** 赛事写保护：经赛事所属联盟判定。 */
    public void assertCanManageEvent(Long eventId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (eventId == null) throw new BusinessException(403, "无权管理该赛事");
        Event ev = eventRepository.findById(eventId).orElse(null);
        if (ev == null || ev.getLeagueId() == null || !s.canManageLeague(ev.getLeagueId())) throw new BusinessException(403, "无权管理该赛事");
    }

    /** 比赛写保护：比赛 → 赛事 → 所属联盟判定。 */
    public void assertCanManageGame(Long gameId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (gameId == null) throw new BusinessException(403, "无权管理该比赛");
        Game g = gameRepository.findById(gameId).orElse(null);
        if (g == null || g.getEventId() == null) throw new BusinessException(403, "无权管理该比赛");
        Event ev = eventRepository.findById(g.getEventId()).orElse(null);
        if (ev == null || ev.getLeagueId() == null || !s.canManageLeague(ev.getLeagueId())) throw new BusinessException(403, "无权管理该比赛");
    }

    /**
     * 球员「代建」写保护（供 PlayerService.create / batchImport 共用，Issue #318 起口径）：
     *  - 租户内不受限（超管 / 租管）→ 直通；
     *  - 受限：teamId 为空 → 403（仅不受限可建无队球员）；
     *  - 受限：teamId ∈ 自有球队集合（含"创建即负责"）→ 放行；
     *  - 其余 → 403（"联盟代建"分支已随"去联盟归属"移除）。
     */
    public void assertCanCreateUnclaimedPlayer(Long teamId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (teamId == null) throw new BusinessException(403, "无权代建该球员");
        if (s.canManageTeam(teamId)) return;
        throw new BusinessException(403, "无权代建该球员");
    }

    /**
     * 球员档案编辑写保护：SELF 通道认本人 userId；ROSTER 通道认球员当前经历所在球队 ∈ teamIds。
     */
    public void assertCanEditPlayerProfile(Long playerId, PlayerEditChannel channel) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (playerId == null) throw new BusinessException(404, "球员不存在");
        Player p = playerRepository.findById(playerId).orElse(null);
        if (p == null) throw new BusinessException(404, "球员不存在");
        if (channel == PlayerEditChannel.SELF) {
            Long uid = CurrentUserHolder.get();
            if (uid != null && uid.equals(p.getUserId())) return;
            throw new BusinessException(403, "只能编辑本人档案");
        }
        // ROSTER 通道（Issue #318 起）：已认领 → 球队不可直接改档案；未认领 → 仅"本人球队集合"可写。
        if (p.getUserId() != null) {
            throw new BusinessException(403, "球队/联盟不可直接修改已认领球员信息");
        }
        for (Long teamId : playerTeamService.currentTeamIds(playerId)) {
            if (s.canManageTeam(teamId)) return;
        }
        throw new BusinessException(403, "无权修改该球员的赛务信息");
    }

    /** 认领审核写保护：reviewerType 须为 team_manager，且球员当前经历所在球队 ∈ teamIds。 */
    public void assertCanReviewClaim(PlayerClaim claim) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (claim == null || !PlayerClaim.REVIEWER_TEAM_MANAGER.equals(claim.getReviewerType())) throw new BusinessException(403, "无权审核该认领");
        Long pid = claim.getPlayerId();
        if (pid == null) throw new BusinessException(404, "球员不存在");
        Player p = playerRepository.findById(pid).orElse(null);
        if (p == null) throw new BusinessException(404, "球员不存在");
        for (Long teamId : playerTeamService.currentTeamIds(p.getId())) {
            if (s.canManageTeam(teamId)) return;
        }
        throw new BusinessException(403, "无权审核该认领");
    }

    /**
     * 解除认领写保护（Feature 解除认领）：球员本人（player.userId == 当前登录人）自助放行；
     * 超管放行；租户管理员限本租户（player.tenantId == 当前请求租户，防 IDOR）；其余 403。
     */
    public void assertCanReleasePlayerClaim(Player player) {
        if (player == null) throw new BusinessException(404, "\u7403\u5458\u4e0d\u5b58\u5728");
        Long uid = CurrentUserHolder.get();
        if (uid != null && uid.equals(player.getUserId())) return;
        if (this.isCurrentUserSuperAdmin()) return;
        EffectiveScope s = this.accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) {
            Long curTenant = CurrentUserHolder.getTenantId();
            if (curTenant != null && curTenant.equals(player.getTenantId())) return;
        }
        throw new BusinessException(403, "\u65e0\u6743\u89e3\u9664\u8be5\u8ba4\u9886");
    }

    /**
     * 历史数据销毁（最终处置）守卫（批次 3b，spec §6.10）：
     * <p>历史数据属平台资产；租户管理员及以下的「删除」仅为归还（软删 + platform_owned 置位），
     * 最终销毁权仅归系统超管，且须审计。
     * <p><b>仅超管</b>放行（以 {@link ApiPermissionService#isSuperAdmin} 独立判定，
     * 不可用 effective scope 的「租户内不受限」——后者合并超管与租管）；非超管一律 403。
     * <p><b>预留通道</b>：待纠错/合规销毁立项后启用，当前无生产调用点（本批不新增销毁端点）。
     */
    public void assertCanPurgeHistoricalData() {
        if (!isCurrentUserSuperAdmin()) throw new BusinessException(403, "仅系统超管可销毁历史数据");
    }

    /**
     * 当前调用者是否系统超管（独立于 effective scope）：供删除路径决定是否置 platform_owned 归还标记。
     * 超管为最终处置方，软删不置归还标记。
     */
    public boolean isCurrentUserSuperAdmin() {
        return apiPermissionService.isSuperAdmin(CurrentUserHolder.get());
    }

    /**
     * 当前调用者是否租户管理员（独立于 effective scope）：球员删除/恢复的角色边界判定（2026-10-09 收窄）。
     */
    public boolean isCurrentUserTenantAdmin() {
        return apiPermissionService.isTenantAdmin(CurrentUserHolder.get());
    }

    @Generated
    public ResourceGuard(AccountScopeService accountScopeService, EventRepository eventRepository,
            GameRepository gameRepository, PlayerRepository playerRepository, PlayerTeamService playerTeamService,
            ApiPermissionService apiPermissionService) {
        this.accountScopeService = accountScopeService;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.playerTeamService = playerTeamService;
        this.apiPermissionService = apiPermissionService;
    }
}
