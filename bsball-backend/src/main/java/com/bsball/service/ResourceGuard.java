/*
 * 账号权限重构（批次 2）：统一写保护组件（ResourceGuard）。
 *
 * 职责：为写路径提供统一的数据级 403 / 404 判定，供 T2.4 十项服务接线消费。
 *  - 语义铁律：只认「租户内不受限 ∪ 集合命中」，绝不读 guest 读标志（读语义不参与写判定）；
 *  - 全部拒绝均抛 BusinessException(403, "...")（文案逐字对齐 spec §5.5 表）；
 *  - 找不到目标实体抛 BusinessException(404, "球员不存在")；
 *  - 资源 → 维度映射：联盟=leagueIds；赛事=事件所属联盟；比赛=经赛事所属联盟；
 *    球队=teamIds；球员=TEAM（当前经历）或 SELF（本人 userId）；认领=当前球队 ∈ teamIds。
 *
 * 本类为批次 2 范围护栏的一部分，批次 3 规则（如更细粒度写保护）由后续任务扩展，
 * 本任务不预埋任何批次 3 语义。
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

    /** 联盟写保护：受限身份需命中自有联盟集合。 */
    public void assertCanManageLeague(Long leagueId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (leagueId == null || !s.canManageLeague(leagueId)) throw new BusinessException(403, "无权管理该联盟");
    }

    /** 球队写保护：受限身份需命中自有球队集合（联盟派生 ≠ 可管理）。 */
    public void assertCanManageTeam(Long teamId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        if (teamId == null || !s.canManageTeam(teamId)) throw new BusinessException(403, "无权管理该球队");
    }

    /** 赛事写保护：经赛事所属联盟判定。 */
    public void assertCanManageEvent(Long eventId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        Event ev = eventRepository.findById(eventId).orElse(null);
        if (ev == null || ev.getLeagueId() == null || !s.canManageLeague(ev.getLeagueId())) throw new BusinessException(403, "无权管理该赛事");
    }

    /** 比赛写保护：比赛 → 赛事 → 所属联盟判定。 */
    public void assertCanManageGame(Long gameId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        Game g = gameRepository.findById(gameId).orElse(null);
        if (g == null || g.getEventId() == null) throw new BusinessException(403, "无权管理该比赛");
        Event ev = eventRepository.findById(g.getEventId()).orElse(null);
        if (ev == null || ev.getLeagueId() == null || !s.canManageLeague(ev.getLeagueId())) throw new BusinessException(403, "无权管理该比赛");
    }

    /**
     * 球员档案（增删）写保护：仅租户内不受限放行；球队账号一律禁止增删球员档案。
     * 参数 teamId 暂未参与判定（签名供调用方对齐，保留以便后续按球队细分）。
     */
    public void assertCanManagePlayerRoster(Long teamId) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        throw new BusinessException(403, "球队账号不可增删球员档案");
    }

    /**
     * 球员档案编辑写保护：SELF 通道认本人 userId；ROSTER 通道认球员当前经历所在球队 ∈ teamIds。
     */
    public void assertCanEditPlayerProfile(Long playerId, PlayerEditChannel channel) {
        EffectiveScope s = accountScopeService.resolveCurrent();
        if (s.isUnrestrictedInTenant()) return;
        Player p = playerRepository.findById(playerId).orElse(null);
        if (p == null) throw new BusinessException(404, "球员不存在");
        if (channel == PlayerEditChannel.SELF) {
            if (CurrentUserHolder.get() != null && CurrentUserHolder.get().equals(p.getUserId())) return;
            throw new BusinessException(403, "只能编辑本人档案");
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
        if (claim == null || !"team_manager".equals(claim.getReviewerType())) throw new BusinessException(403, "无权审核该认领");
        Player p = playerRepository.findById(claim.getPlayerId()).orElse(null);
        if (p == null) throw new BusinessException(404, "球员不存在");
        for (Long teamId : playerTeamService.currentTeamIds(p.getId())) {
            if (s.canManageTeam(teamId)) return;
        }
        throw new BusinessException(403, "无权审核该认领");
    }

    @Generated
    public ResourceGuard(AccountScopeService accountScopeService, EventRepository eventRepository,
            GameRepository gameRepository, PlayerRepository playerRepository, PlayerTeamService playerTeamService) {
        this.accountScopeService = accountScopeService;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.playerTeamService = playerTeamService;
    }
}
