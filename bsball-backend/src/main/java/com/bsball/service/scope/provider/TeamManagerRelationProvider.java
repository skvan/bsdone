/*
 * 账号权限重构（批次 1）：球队管理员关系提供者（TeamManagerRelationProvider）。
 *
 * 职责：
 *  - 若当前账号在 bs_team_manager 中关联了任意 active 球队，则判定本 Provider 适用于该账号；
 *  - 适用时把其管理的球队 ID 注入上下文（teamIds）。
 *
 * 契约（T1.4）：
 *  - 注入一律走 ctx.addTeam(...)（内置 null 守卫），不直接操作 ctx.getTeamIds() 集合；
 *  - contribute 仅在 supports 返回 true 后由编排方调用，故此处不重复判定适用性。
 */
package com.bsball.service.scope.provider;

import com.bsball.model.entity.TeamManager;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.service.scope.ScopeRelationProvider;
import com.bsball.service.scope.ScopeResolutionContext;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
public class TeamManagerRelationProvider implements ScopeRelationProvider {

    private final TeamManagerRepository teamManagerRepository;

    /** 该 Provider 是否适用于当前账号（按 TeamManager 关系数据判定）。 */
    @Override
    public boolean supports(ScopeResolutionContext ctx) {
        return ctx.getUserId() != null
                && !teamManagerRepository
                        .findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                                ctx.getUserId(), ctx.getTenantId(), TeamManager.STATUS_ACTIVE)
                        .isEmpty();
    }

    /** 注入当前账号管理的球队 ID。 */
    @Override
    public void contribute(ScopeResolutionContext ctx) {
        List<TeamManager> managers = teamManagerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                ctx.getUserId(), ctx.getTenantId(), TeamManager.STATUS_ACTIVE);
        for (TeamManager tm : managers) {
            ctx.addTeam(tm.getTeamId());
        }
    }

    @Generated
    public TeamManagerRelationProvider(TeamManagerRepository teamManagerRepository) {
        this.teamManagerRepository = teamManagerRepository;
    }
}
