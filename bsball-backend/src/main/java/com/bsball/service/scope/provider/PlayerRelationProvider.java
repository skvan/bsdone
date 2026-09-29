/*
 * 账号权限重构（批次 1）：球员关系提供者（PlayerRelationProvider）。
 *
 * 职责：
 *  - 若当前账号已认证认领了某条球员档案（bs_player.user_id 命中且未删除），则判定本 Provider 适用于该账号；
 *  - 球员身份不注入任何管理集合（leagueIds / teamIds 均不注入）：本人档案的编辑权由
 *    ResourceGuard.assertCanEditPlayerProfile(SELF) 单独判定，与「管理范围」正交。
 *
 * 契约（T1.4）：
 *  - contribute 仅在 supports 返回 true 后由编排方调用，故此处不重复判定适用性；
 *  - 本 Provider 仅作「身份标记」，contribute 为空实现为有意为之，勿在此注入集合。
 */
package com.bsball.service.scope.provider;

import com.bsball.repository.PlayerRepository;
import com.bsball.service.scope.ScopeRelationProvider;
import com.bsball.service.scope.ScopeResolutionContext;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
public class PlayerRelationProvider implements ScopeRelationProvider {

    private final PlayerRepository playerRepository;

    /** 该 Provider 是否适用于当前账号（按已认领的球员档案判定）。 */
    @Override
    public boolean supports(ScopeResolutionContext ctx) {
        return ctx.getUserId() != null
                && playerRepository.findFirstByUserIdAndDeletedAtIsNull(ctx.getUserId()).isPresent();
    }

    /** 球员身份不注入管理集合（本人档案由 ResourceGuard.assertCanEditPlayerProfile(SELF) 判定）。 */
    @Override
    public void contribute(ScopeResolutionContext ctx) {
        // 有意留空：球员身份仅作标记，不注入 leagueIds / teamIds。
    }

    @Generated
    public PlayerRelationProvider(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }
}
