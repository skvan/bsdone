/*
 * 账号权限重构（批次 1）：联盟主办方关系提供者（LeagueOwnerRelationProvider）。
 *
 * 职责：
 *  - 若当前账号在 bs_league_owner 中于当前租户内关联了任意 active 联盟，则判定本 Provider 适用于该账号；
 *  - 适用时把其主办的联盟 ID 注入上下文（leagueIds）。
 *
 * 契约（T1.4 / 范围铁律）：
 *  - 注入一律走 ctx.addLeague(...)（内置 null 守卫），不直接操作 ctx.getLeagueIds() 集合；
 *  - 不注入 teamIds：球队写域不归主办方，球队「读」由 ScopeQuerySupport 从联盟派生；
 *  - contribute 仅在 supports 返回 true 后由编排方调用，故此处不重复判定适用性。
 */
package com.bsball.service.scope.provider;

import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueOwnerRepository;
import com.bsball.service.scope.ScopeRelationProvider;
import com.bsball.service.scope.ScopeResolutionContext;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
public class LeagueOwnerRelationProvider implements ScopeRelationProvider {

    private final LeagueOwnerRepository leagueOwnerRepository;

    /** 该 Provider 是否适用于当前账号（按 LeagueOwner 关系数据、限定本租户判定）。 */
    @Override
    public boolean supports(ScopeResolutionContext ctx) {
        return ctx.getUserId() != null
                && !leagueOwnerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                        ctx.getUserId(), ctx.getTenantId(), LeagueOwner.STATUS_ACTIVE).isEmpty();
    }

    /** 注入当前账号主办的联盟 ID（不注入 teamIds）。 */
    @Override
    public void contribute(ScopeResolutionContext ctx) {
        List<LeagueOwner> owners = leagueOwnerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                ctx.getUserId(), ctx.getTenantId(), LeagueOwner.STATUS_ACTIVE);
        for (LeagueOwner lo : owners) {
            ctx.addLeague(lo.getLeagueId());
        }
    }

    @Generated
    public LeagueOwnerRelationProvider(LeagueOwnerRepository leagueOwnerRepository) {
        this.leagueOwnerRepository = leagueOwnerRepository;
    }
}
