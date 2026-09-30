/*
 * 账号权限重构（批次 1 遗留 Minor①，随 Task 2.5 补）：PlayerRelationProvider 实现级单测。
 *
 * 覆盖契约：
 *  - supports：userId 为空短路（不查库）；已认证认领球员档案（bs_player.user_id 命中且未删除）即适用；
 *  - contribute：球员身份仅作标记，**不注入任何管理集合**（leagueIds / teamIds 均空）。
 */
package com.bsball.service.scope.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.entity.Player;
import com.bsball.repository.PlayerRepository;
import com.bsball.service.scope.ScopeResolutionContext;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerRelationProvider：球员身份判定（不注入管理集合）")
class PlayerRelationProviderTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 2L;

    @Mock
    private PlayerRepository playerRepository;

    private PlayerRelationProvider provider() {
        return new PlayerRelationProvider(playerRepository);
    }

    @Test
    @DisplayName("supports：userId 为空 → false 且不查库")
    void supports_nullUser_false_noQuery() {
        assertFalse(provider().supports(new ScopeResolutionContext(null, TENANT_ID)));
        verifyNoInteractions(playerRepository);
    }

    @Test
    @DisplayName("supports：已认领球员档案（未删除）→ true")
    void supports_claimedPlayer_true() {
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(Optional.of(new Player()));

        assertTrue(provider().supports(new ScopeResolutionContext(USER_ID, TENANT_ID)));
        verify(playerRepository).findFirstByUserIdAndDeletedAtIsNull(USER_ID);
    }

    @Test
    @DisplayName("supports：无已认领球员档案 → false")
    void supports_noClaimedPlayer_false() {
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

        assertFalse(provider().supports(new ScopeResolutionContext(USER_ID, TENANT_ID)));
    }

    @Test
    @DisplayName("contribute：不注入 leagueIds / teamIds（仅身份标记）")
    void contribute_noManagementCollectionsInjected() {
        ScopeResolutionContext ctx = new ScopeResolutionContext(USER_ID, TENANT_ID);

        provider().contribute(ctx);

        assertTrue(ctx.getLeagueIds().isEmpty());
        assertTrue(ctx.getTeamIds().isEmpty());
        verifyNoInteractions(playerRepository);
    }
}
