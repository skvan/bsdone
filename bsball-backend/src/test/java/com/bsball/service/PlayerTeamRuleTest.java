/*
 * 账号权限重构（批次 3b，Task 3.10）：球队经历操作规则落地单元测试（Mockito 手工装配）。
 *
 * 覆盖：
 *  自助路径（updateSelfProfile 的 teamEntries 白名单）
 *   ① current true→false 放行（透传 plan / 镜像 / 落库 / 沿革）；
 *   ② 删除无比赛记录经历放行 + 写删除审计；
 *   ③ 删除有比赛记录经历 → 400（文案含场次 N，守卫先于任何写）；
 *   ④ 新增行 → 403；复活 → 403；current→true → 403；改 number / 改 positions → 403；
 *   ⑤ 操作他人档案 → 403（守卫拦截，不做任何查询）；
 *   ⑥ 无 teamEntries 时字段编辑不受影响（兼容 3a）。
 *  管理端路径（update）
 *   ⑦ 删除有记录经历 → 400（守卫在 persistPlan / save 之前）；
 *   ⑧ 删除无记录经历 → 落库 + 写删除审计。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.PlayerTeamService.PlayerTeamSyncPlan;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService：球队经历操作规则（删除守卫 / 自助白名单 / 审计）")
@SuppressWarnings("unchecked")
class PlayerTeamRuleTest {

    private static final long TENANT = 9L;
    private static final long PLAYER_ID = 1L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerTeamRepository playerTeamRepository;

    @Mock
    private StatsService statsService;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;

    @Mock
    private PlayerTeamService playerTeamService;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private PlayerClaimRepository playerClaimRepository;

    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, resourceGuard, personnelHistoryRecorder,
                playerTeamService, tenantQueryPolicyService, sysConfigService, playerClaimRepository,
                gamePlayerStatRepository);
    }

    // ------------------------------------------------------------------ 自助路径

    @Test
    @DisplayName("自助：current true→false 放行 → 透传 plan / 镜像 / 落库 / 沿革")
    void self_uncheckCurrent_passesThroughPipeline() {
        Player p = player(PLAYER_ID, TENANT);
        p.setTeamId(5L);
        PlayerTeam e = entry(11L, 5L, "7", true, 0);
        e.setPositionsList(List.of("C"));
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(), List.of(e), List.of(), null,
                Set.of(5L), Set.of(), List.of(e));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e));
        when(playerTeamService.plan(eq(p), anyList(), eq(true))).thenReturn(plan);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(null);

        service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "7", List.of("C"), false)));

        verify(playerTeamService).plan(eq(p), anyList(), eq(true));
        verify(playerTeamService).applyMirror(p, plan);
        verify(playerTeamService).persistPlan(PLAYER_ID, plan);
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(p, Set.of(5L), Set.of());
        verify(gamePlayerStatRepository, never()).countValidByPlayerIdAndTeamId(any(), any());
    }

    @Test
    @DisplayName("自助：删除无比赛记录经历放行 → 落库 + 写删除审计")
    void self_removeEntryWithoutGames_persistsAndAudits() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam keep = entry(11L, 5L, "7", true, 0);
        keep.setPositionsList(List.of("C"));
        PlayerTeam drop = entry(12L, 6L, "8", false, 1);
        drop.setPositionsList(List.of("P"));
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(), List.of(), List.of(drop), keep,
                Set.of(5L), Set.of(5L), List.of(keep));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(keep, drop));
        when(playerTeamService.plan(eq(p), anyList(), eq(true))).thenReturn(plan);
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(PLAYER_ID, 6L)).thenReturn(0L);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(null);

        service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "7", List.of("C"), true)));

        verify(playerTeamService).persistPlan(PLAYER_ID, plan);
        verify(gamePlayerStatRepository).countValidByPlayerIdAndTeamId(PLAYER_ID, 6L);
        verify(personnelHistoryRecorder).recordPlayerTeamEntryRemoval(PLAYER_ID, TENANT, drop);
    }

    @Test
    @DisplayName("自助：删除有比赛记录经历 → 400（文案含场次 N，守卫先于任何写）")
    void self_removeEntryWithGames_rejected400() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam keep = entry(11L, 5L, "7", true, 0);
        keep.setPositionsList(List.of("C"));
        PlayerTeam drop = entry(12L, 6L, "8", false, 1);
        drop.setPositionsList(List.of("P"));
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(), List.of(), List.of(drop), keep,
                Set.of(5L), Set.of(5L), List.of(keep));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(keep, drop));
        when(playerTeamService.plan(eq(p), anyList(), eq(true))).thenReturn(plan);
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(PLAYER_ID, 6L)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "7", List.of("C"), true))));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("3"), "文案应含实际场次");
        assertTrue(ex.getMessage().contains("不可删除"));
        verify(playerRepository, never()).save(any(Player.class));
        verify(playerTeamService, never()).persistPlan(any(), any());
        verify(playerTeamService, never()).applyMirror(any(), any());
    }

    @Test
    @DisplayName("自助：新增球队经历 → 403（指引走球队邀请）")
    void self_addNewEntry_forbidden403() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of());
        when(playerTeamRepository.findFirstByPlayerIdAndTeamIdAndDeletedAtIsNotNullOrderByIdDesc(PLAYER_ID, 7L))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(7L, null, null, true))));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("新增球队经历"));
        verify(playerRepository, never()).save(any(Player.class));
        verify(playerTeamService, never()).plan(any(), any(), eq(true));
    }

    @Test
    @DisplayName("自助：复活已软删经历 → 403（指引重新入队）")
    void self_reviveEntry_forbidden403() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of());
        when(playerTeamRepository.findFirstByPlayerIdAndTeamIdAndDeletedAtIsNotNullOrderByIdDesc(PLAYER_ID, 7L))
                .thenReturn(Optional.of(entry(99L, 7L, "9", false, 0)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(7L, null, null, true))));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("重新入队"));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助：非当前经历勾回 current→true → 403")
    void self_checkCurrentBack_forbidden403() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e = entry(11L, 5L, "7", false, 0);
        e.setPositionsList(List.of("C"));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "7", List.of("C"), true))));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助：修改背号 → 403（指引走球队管理）")
    void self_changeNumber_forbidden403() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e = entry(11L, 5L, "7", true, 0);
        e.setPositionsList(List.of("C"));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "99", List.of("C"), true))));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("背号"));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助：修改守备位置 → 403（指引走球队管理）")
    void self_changePositions_forbidden403() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e = entry(11L, 5L, "7", true, 0);
        e.setPositionsList(List.of("C"));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "7", List.of("P"), true))));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("守位"));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助：操作他人档案 → 403（SELF 守卫拦截，不做任何查询）")
    void self_otherProfile_forbidden403() {
        doThrow(new BusinessException(403, "无权编辑该球员档案"))
                .when(resourceGuard).assertCanEditPlayerProfile(PLAYER_ID, ResourceGuard.PlayerEditChannel.SELF);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(PLAYER_ID, body(entryMap(5L, "7", List.of("C"), false))));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).findById(any());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助：无 teamEntries 时字段编辑不受影响（兼容 3a）")
    void self_withoutTeamEntries_fieldEditUnaffected() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("height", "180");

        Player result = service.updateSelfProfile(PLAYER_ID, body);

        assertEquals("180", result.getHeight());
        verify(playerTeamService, never()).plan(any(), any(), eq(true));
        verify(playerTeamRepository, never()).findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(any());
        verify(gamePlayerStatRepository, never()).countValidByPlayerIdAndTeamId(any(), any());
    }

    // ------------------------------------------------------------------ 管理端路径

    @Test
    @DisplayName("管理端：删除有记录经历 → 400（守卫先于 persistPlan / save）")
    void admin_removeEntryWithGames_rejected400() {
        Player existing = player(PLAYER_ID, TENANT);
        existing.setTeamId(5L);
        existing.setStatus("active");
        PlayerTeam drop = entry(12L, 6L, "8", false, 1);
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(), List.of(), List.of(drop), null,
                Set.of(5L), Set.of(5L), List.of());
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(existing));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(playerTeamService.plan(any(Player.class), any(), eq(true))).thenReturn(plan);
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(PLAYER_ID, 6L)).thenReturn(2L);

        Player body = new Player();
        body.setTeamEntries(List.of(new PlayerTeamEntryDto(null, 5L, null, "7", List.of("C"), true, null)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(PLAYER_ID, body));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("2"));
        verify(playerTeamService, never()).persistPlan(any(), any());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("管理端：删除无记录经历 → 落库 + 写删除审计")
    void admin_removeEntryWithoutGames_persistsAndAudits() {
        Player existing = player(PLAYER_ID, TENANT);
        existing.setTeamId(5L);
        existing.setStatus("active");
        PlayerTeam drop = entry(12L, 6L, "8", false, 1);
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(), List.of(), List.of(drop), null,
                Set.of(5L), Set.of(5L), List.of());
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(existing));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(playerTeamService.plan(any(Player.class), any(), eq(true))).thenReturn(plan);
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(PLAYER_ID, 6L)).thenReturn(0L);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(null);

        Player body = new Player();
        body.setTeamEntries(List.of(new PlayerTeamEntryDto(null, 5L, null, "7", List.of("C"), true, null)));

        service.update(PLAYER_ID, body);

        verify(gamePlayerStatRepository).countValidByPlayerIdAndTeamId(PLAYER_ID, 6L);
        verify(playerTeamService).persistPlan(PLAYER_ID, plan);
        verify(personnelHistoryRecorder).recordPlayerTeamEntryRemoval(PLAYER_ID, TENANT, drop);
    }

    // ------------------------------------------------------------------ 辅助

    private static Map<String, Object> body(Map<String, Object> entry) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("teamEntries", List.of(entry));
        return body;
    }

    private static Map<String, Object> entryMap(long teamId, String number, List<String> positions, boolean current) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("teamId", teamId);
        m.put("number", number);
        m.put("positions", positions);
        m.put("current", current);
        return m;
    }

    private static Player player(long id, long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        return p;
    }

    private static PlayerTeam entry(long id, long teamId, String number, boolean current, int sort) {
        PlayerTeam e = new PlayerTeam();
        e.setId(id);
        e.setPlayerId(PLAYER_ID);
        e.setTeamId(teamId);
        e.setTenantId(TENANT);
        e.setNumber(number);
        e.setCurrent(current);
        e.setSort(sort);
        return e;
    }
}
