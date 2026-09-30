/*
 * 账号权限重构（批次 2 / 批次 3b）：统一写保护组件 ResourceGuard 的单测。
 *
 * 覆盖语义（对照 spec §5.5 与 §6.9）：
 *  - 每个守卫：不受限（超管 / 租管）直接放行 + 受限越权 403；
 *  - assertCanStewardOrManageTeam（批次 3b）：本队命中 / 无主且归属联盟命中放行；有主只读 403；
 *  - assertCanCreateUnclaimedPlayer（批次 3b）：本队命中 / 域内球队（联盟所属）放行；无队 / 他队 / 域外 403；
 *  - assertCanEditPlayerProfile：SELF（本人放行 / 他人 403）与 ROSTER（已认领上级只读 403；未认领域内可写）；
 *  - assertCanReviewClaim：reviewerType 不符 / 球队命中 / 未命中 分支；
 *  - 403 / 404 文案逐字一致（见 spec §5.5 / §6.9）。
 *
 * 风格参照 AccountScopeServiceTest：外部依赖一律 Mockito mock，不启动 Spring、不连库；
 * 被测 guard 手工 new（@Generated 构造器注入），CurrentUserHolder 为 ThreadLocal 需逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaim;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ResourceGuard.PlayerEditChannel;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceGuard：统一写保护（403 / 404 判定矩阵）")
class ResourceGuardTest {

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerTeamService playerTeamService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    private ResourceGuard guard;

    @BeforeEach
    void setUp() {
        // CurrentUserHolder 为 ThreadLocal，用例起点必须清空（与 AccountScopeServiceTest 基线对齐）
        CurrentUserHolder.clear();
        guard = new ResourceGuard(accountScopeService, eventRepository, gameRepository, playerRepository,
                playerTeamService, teamRepository, teamManagerRepository);
    }

    @AfterEach
    void tearDown() {
        // CurrentUserHolder 为 ThreadLocal，用例结束显式清理，避免跨用例污染
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ assertCanManageLeague

    @Test
    @DisplayName("联盟守卫：不受限直接放行（不触碰任何仓库）")
    void league_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanManageLeague(null));
        assertDoesNotThrow(() -> guard.assertCanManageLeague(999L));
    }

    @Test
    @DisplayName("联盟守卫：受限仅放行自有联盟（10 放行 / 11 拒绝 403）")
    void league_restricted_managesOwnLeagueOnly() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        assertDoesNotThrow(() -> guard.assertCanManageLeague(10L));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageLeague(11L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该联盟", ex.getMessage());
    }

    @Test
    @DisplayName("联盟守卫：受限且 leagueId 为 null → 403")
    void league_restricted_null_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageLeague(null));
        assertEquals(403, ex.getCode());
    }

    // ------------------------------------------------------------------ assertCanManageTeam

    @Test
    @DisplayName("球队守卫：不受限直接放行")
    void team_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanManageTeam(999L));
    }

    @Test
    @DisplayName("球队守卫：球队管理员不能管理他人球队（100 放行 / 101 拒绝 403）")
    void teamManager_cannotEditOtherTeam() {
        // scope: teamIds=[100]
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));

        assertDoesNotThrow(() -> guard.assertCanManageTeam(100L));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageTeam(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该球队", ex.getMessage());
    }

    @Test
    @DisplayName("球队守卫：主办方不可管理球队（联盟派生≠可管理）")
    void organizer_cannotEditTeamProfile() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageTeam(100L)); // 联盟派生≠可管理
        assertEquals(403, ex.getCode());
    }

    // ------------------------------------------------------------------ assertCanStewardOrManageTeam（批次 3b）

    @Test
    @DisplayName("接管/代管守卫：不受限直接放行（不查库）")
    void steward_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanStewardOrManageTeam(999L));
        verifyNoInteractions(teamRepository, teamManagerRepository);
    }

    @Test
    @DisplayName("接管/代管守卫：本队命中放行（不查库）")
    void steward_ownTeam_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));

        assertDoesNotThrow(() -> guard.assertCanStewardOrManageTeam(100L));
        verifyNoInteractions(teamRepository, teamManagerRepository);
    }

    @Test
    @DisplayName("接管/代管守卫：受限且 teamId 为 null → 403（不查库）")
    void steward_nullId_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanStewardOrManageTeam(null));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该球队", ex.getMessage());
        verifyNoInteractions(teamRepository, teamManagerRepository);
    }

    @Test
    @DisplayName("接管/代管守卫：球队不存在 → 403（不查负责人）")
    void steward_teamNotFound_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanStewardOrManageTeam(101L));
        assertEquals(403, ex.getCode());
        verifyNoInteractions(teamManagerRepository);
    }

    @Test
    @DisplayName("接管/代管守卫：球队有主（active 负责人存在）→ 联盟只读 403")
    void steward_teamHasManager_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, 10L)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(101L, TeamManager.STATUS_ACTIVE))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanStewardOrManageTeam(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该球队", ex.getMessage());
    }

    @Test
    @DisplayName("接管/代管守卫：球队无主且归属联盟命中 → 联盟放行")
    void steward_noManager_leagueDomain_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, 10L)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(101L, TeamManager.STATUS_ACTIVE))
                .thenReturn(false);

        assertDoesNotThrow(() -> guard.assertCanStewardOrManageTeam(101L));
    }

    @Test
    @DisplayName("接管/代管守卫：球队无主但未归属联盟 → 403")
    void steward_noManager_nullLeague_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, null)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(101L, TeamManager.STATUS_ACTIVE))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanStewardOrManageTeam(101L));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("接管/代管守卫：球队无主但归属他方联盟 → 403")
    void steward_noManager_otherLeague_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, 20L)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(101L, TeamManager.STATUS_ACTIVE))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanStewardOrManageTeam(101L));
        assertEquals(403, ex.getCode());
    }

    // ------------------------------------------------------------------ assertCanManageEvent

    @Test
    @DisplayName("赛事守卫：不受限直接放行（不查库）")
    void event_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanManageEvent(5L));
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("赛事守卫：主办方自有联盟赛事放行（event.leagueId 命中）")
    void event_ownLeague_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, 10L)));

        assertDoesNotThrow(() -> guard.assertCanManageEvent(5L));
    }

    @Test
    @DisplayName("赛事守卫：他方联盟赛事拒绝 403")
    void event_otherLeague_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, 11L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageEvent(5L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该赛事", ex.getMessage());
    }

    @Test
    @DisplayName("赛事守卫：赛事不存在 / leagueId 为空 → 403")
    void event_notFoundOrNullLeague_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());
        when(eventRepository.findById(6L)).thenReturn(Optional.of(event(6L, null)));

        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageEvent(5L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageEvent(6L)).getCode());
    }

    @Test
    @DisplayName("赛事守卫：受限且 eventId 为 null → 403（不查库）")
    void event_restricted_nullId_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageEvent(null));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该赛事", ex.getMessage());
        verifyNoInteractions(eventRepository);
    }

    // ------------------------------------------------------------------ assertCanManageGame

    @Test
    @DisplayName("比赛守卫：不受限直接放行（不查库）")
    void game_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanManageGame(7L));
        verifyNoInteractions(gameRepository, eventRepository);
    }

    @Test
    @DisplayName("比赛守卫：经赛事命中自有联盟放行")
    void game_ownLeague_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(gameRepository.findById(7L)).thenReturn(Optional.of(game(7L, 5L)));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, 10L)));

        assertDoesNotThrow(() -> guard.assertCanManageGame(7L));
    }

    @Test
    @DisplayName("比赛守卫：赛事属他方联盟 → 403")
    void game_otherLeague_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(gameRepository.findById(7L)).thenReturn(Optional.of(game(7L, 5L)));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, 11L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageGame(7L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该比赛", ex.getMessage());
    }

    @Test
    @DisplayName("比赛守卫：比赛不存在 / eventId 为空 / 赛事不存在 → 403")
    void game_missingChain_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(gameRepository.findById(7L)).thenReturn(Optional.empty());
        when(gameRepository.findById(8L)).thenReturn(Optional.of(game(8L, null)));
        when(gameRepository.findById(9L)).thenReturn(Optional.of(game(9L, 5L)));
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());

        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageGame(7L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageGame(8L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageGame(9L)).getCode());
    }

    @Test
    @DisplayName("比赛守卫：受限且 gameId 为 null → 403（不查库）")
    void game_restricted_nullId_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanManageGame(null));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该比赛", ex.getMessage());
        verifyNoInteractions(gameRepository, eventRepository);
    }

    // ------------------------------------------------------------------ assertCanCreateUnclaimedPlayer（批次 3b）

    @Test
    @DisplayName("代建守卫：不受限放行（不查库）")
    void createPlayer_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanCreateUnclaimedPlayer(100L));
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("代建守卫：球队管理员本队放行（不查库）")
    void createPlayer_ownTeam_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));

        assertDoesNotThrow(() -> guard.assertCanCreateUnclaimedPlayer(100L));
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("代建守卫：联盟管理员域内球队放行（球队归属联盟命中）")
    void createPlayer_leagueDomainTeam_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, 10L)));

        assertDoesNotThrow(() -> guard.assertCanCreateUnclaimedPlayer(100L));
    }

    @Test
    @DisplayName("代建守卫：联盟管理员域外球队 → 403")
    void createPlayer_leagueOutsideDomain_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, 20L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权代建该球员", ex.getMessage());
    }

    @Test
    @DisplayName("代建守卫：球队管理员他队 → 403")
    void createPlayer_otherTeam_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, 20L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权代建该球员", ex.getMessage());
    }

    @Test
    @DisplayName("代建守卫：受限且 teamId 为 null → 403（不查库）")
    void createPlayer_nullTeam_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(null));
        assertEquals(403, ex.getCode());
        assertEquals("无权代建该球员", ex.getMessage());
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("代建守卫：球队不存在 → 403")
    void createPlayer_teamNotFound_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(101L));
        assertEquals(403, ex.getCode());
    }

    // ------------------------------------------------------------------ assertCanEditPlayerProfile

    @Test
    @DisplayName("编辑档案守卫：不受限放行（不查库）")
    void editPlayer_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.SELF));
        verifyNoInteractions(playerRepository, playerTeamService);
    }

    @Test
    @DisplayName("编辑档案守卫：球员不存在 → 404")
    void editPlayer_notFound_404() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.SELF));
        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
    }

    @Test
    @DisplayName("编辑档案守卫：playerId 为 null → 404（不查库）")
    void editPlayer_nullId_404() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(null, PlayerEditChannel.SELF));
        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
        verifyNoInteractions(playerRepository, playerTeamService);
    }

    @Test
    @DisplayName("编辑档案守卫：SELF 本人放行")
    void editPlayer_self_ownProfile_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of()));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, 7L)));
        CurrentUserHolder.set(7L, 2L);

        assertDoesNotThrow(() -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.SELF));
    }

    @Test
    @DisplayName("编辑档案守卫：SELF 编辑他人 → 403「只能编辑本人档案」")
    void editPlayer_self_otherProfile_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of()));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, 7L)));
        CurrentUserHolder.set(8L, 2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.SELF));
        assertEquals(403, ex.getCode());
        assertEquals("只能编辑本人档案", ex.getMessage());
    }

    @Test
    @DisplayName("编辑档案守卫：ROSTER 已认领 → 上级只读 403（不查经历球队）")
    void editPlayer_roster_claimedPlayer_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, 7L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.ROSTER));
        assertEquals(403, ex.getCode());
        assertEquals("球队/联盟不可直接修改已认领球员信息", ex.getMessage());
        verifyNoInteractions(playerTeamService);
    }

    @Test
    @DisplayName("编辑档案守卫：ROSTER 未认领本队命中放行")
    void editPlayer_roster_teamHit_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, null)));
        when(playerTeamService.currentTeamIds(5L)).thenReturn(Set.of(200L, 100L));

        assertDoesNotThrow(() -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.ROSTER));
    }

    @Test
    @DisplayName("编辑档案守卫：ROSTER 未认领且归属联盟命中放行")
    void editPlayer_roster_leagueDomain_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, null)));
        when(playerTeamService.currentTeamIds(5L)).thenReturn(Set.of(100L));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, 10L)));

        assertDoesNotThrow(() -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.ROSTER));
    }

    @Test
    @DisplayName("编辑档案守卫：ROSTER 未认领当前队未命中 → 403「无权修改该球员的赛务信息」")
    void editPlayer_roster_teamMiss_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, null)));
        when(playerTeamService.currentTeamIds(5L)).thenReturn(Set.of(200L));
        when(teamRepository.findById(200L)).thenReturn(Optional.of(team(200L, 30L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.ROSTER));
        assertEquals(403, ex.getCode());
        assertEquals("无权修改该球员的赛务信息", ex.getMessage());
    }

    @Test
    @DisplayName("编辑档案守卫：ROSTER 未认领当前队为空集 → 403（空集不静默放行）")
    void editPlayer_roster_emptyTeams_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, null)));
        when(playerTeamService.currentTeamIds(5L)).thenReturn(Set.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.ROSTER));
        assertEquals(403, ex.getCode());
        assertEquals("无权修改该球员的赛务信息", ex.getMessage());
    }

    // ------------------------------------------------------------------ assertCanReviewClaim

    @Test
    @DisplayName("认领审核守卫：不受限放行")
    void reviewClaim_unrestricted_passes() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());

        assertDoesNotThrow(() -> guard.assertCanReviewClaim(null));
        verifyNoInteractions(playerRepository, playerTeamService);
    }

    @Test
    @DisplayName("认领审核守卫：claim 为空 / reviewerType 非 team_manager → 403")
    void reviewClaim_nullOrWrongReviewerType_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));

        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanReviewClaim(null)).getCode());

        PlayerClaim platformAdmin = new PlayerClaim();
        platformAdmin.setReviewerType("platform_admin");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanReviewClaim(platformAdmin));
        assertEquals(403, ex.getCode());
        assertEquals("无权审核该认领", ex.getMessage());
    }

    @Test
    @DisplayName("认领审核守卫：team_manager 且球员当前队命中放行")
    void reviewClaim_teamManager_teamHit_passes() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player(50L, null)));
        when(playerTeamService.currentTeamIds(50L)).thenReturn(Set.of(100L));

        assertDoesNotThrow(() -> guard.assertCanReviewClaim(claim(50L)));
    }

    @Test
    @DisplayName("认领审核守卫：team_manager 但球员当前队未命中 → 403")
    void reviewClaim_teamManager_teamMiss_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player(50L, null)));
        when(playerTeamService.currentTeamIds(50L)).thenReturn(Set.of(200L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanReviewClaim(claim(50L)));
        assertEquals(403, ex.getCode());
        assertEquals("无权审核该认领", ex.getMessage());
    }

    @Test
    @DisplayName("认领审核守卫：currentTeamIds 为空集 → 403（空集不静默放行）")
    void reviewClaim_emptyTeams_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(50L)).thenReturn(Optional.of(player(50L, null)));
        when(playerTeamService.currentTeamIds(50L)).thenReturn(Set.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanReviewClaim(claim(50L)));
        assertEquals(403, ex.getCode());
        assertEquals("无权审核该认领", ex.getMessage());
    }

    @Test
    @DisplayName("认领审核守卫：球员不存在 → 404")
    void reviewClaim_playerNotFound_404() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(50L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanReviewClaim(claim(50L)));
        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
    }

    @Test
    @DisplayName("认领审核守卫：playerId 为 null → 404（不查库）")
    void reviewClaim_nullPlayerId_404() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanReviewClaim(claim(null)));
        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
        verifyNoInteractions(playerRepository, playerTeamService);
    }

    // ------------------------------------------------------------------ 空域 / 只读放行（§12.2 矩阵：写侧）

    @Test
    @DisplayName("空域账号：联盟/球队/赛事/比赛/代建写一律 403")
    void emptyScope_allWritePath_forbidden() {
        // 门户角色无归属 → 空域（manage 集合为空，绝不放行任何写）
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.empty());
        // 赛事/比赛链路：资源不存在亦统一 403（隐藏存在性）
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());
        when(gameRepository.findById(7L)).thenReturn(Optional.empty());

        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageLeague(10L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageTeam(100L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageEvent(5L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageGame(7L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(100L)).getCode());
        // 空域下不触碰球员档案相关依赖
        verifyNoInteractions(playerRepository, playerTeamService);
    }

    @Test
    @DisplayName("空域账号：ROSTER 通道编辑未认领球员赛务信息 → 403（球队未命中）")
    void emptyScope_rosterEdit_forbidden() {
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.empty());
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, null)));
        when(playerTeamService.currentTeamIds(5L)).thenReturn(Set.of(200L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanEditPlayerProfile(5L, PlayerEditChannel.ROSTER));
        assertEquals(403, ex.getCode());
        assertEquals("无权修改该球员的赛务信息", ex.getMessage());
    }

    @Test
    @DisplayName("guest-like 只读放行：仅读语义，绝不授予任何写（空集仍 403）")
    void guestLikeRead_onlyReads_neverWrites() {
        // guest 读标志仅授予读语义：读放行，但不参与写判定
        EffectiveScope scope = EffectiveScope.restricted(true, Set.of(), Set.of());
        when(accountScopeService.resolveCurrent()).thenReturn(scope);

        // 读侧：guest 只读放行（管理集合为空亦可读）
        assertTrue(scope.canReadLeague(10L));
        assertTrue(scope.canReadTeam(100L));

        // 写侧：guest 读标志不参与写判定：管理集合为空 → 写一律 403
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageLeague(10L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanManageTeam(100L)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(100L)).getCode());
    }

    @Test
    @DisplayName("主办方（仅联盟）：域外球队代建 → 403")
    void organizer_createOutsideLeagueDomain_forbidden() {
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, 20L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertCanCreateUnclaimedPlayer(100L));
        assertEquals(403, ex.getCode());
        assertEquals("无权代建该球员", ex.getMessage());
    }

    // ------------------------------------------------------------------ 辅助

    private static Event event(Long id, Long leagueId) {
        Event ev = new Event();
        ev.setId(id);
        ev.setLeagueId(leagueId);
        return ev;
    }

    private static Game game(Long id, Long eventId) {
        Game g = new Game();
        g.setId(id);
        g.setEventId(eventId);
        return g;
    }

    private static Player player(Long id, Long userId) {
        Player p = new Player();
        p.setId(id);
        p.setUserId(userId);
        return p;
    }

    private static PlayerClaim claim(Long playerId) {
        PlayerClaim c = new PlayerClaim();
        c.setPlayerId(playerId);
        c.setReviewerType(PlayerClaim.REVIEWER_TEAM_MANAGER);
        return c;
    }

    private static Team team(Long id, Long leagueId) {
        Team t = new Team();
        t.setId(id);
        t.setLeagueId(leagueId);
        return t;
    }
}
