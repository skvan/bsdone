package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Player;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService：自助建档 + 本人档案编辑（SELF 白名单）")
class PlayerServiceSelfProfileTest {

    private static final long TENANT = 9L;
    private static final long USER = 42L;

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

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, resourceGuard, personnelHistoryRecorder,
                playerTeamService, tenantQueryPolicyService, sysConfigService, playerClaimRepository);
    }

    @Test
    @DisplayName("自助建档：租户开关关闭 → 400「自助建档未开放」")
    void createSelfProfile_toggleOff_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(sysConfigService.getBoolean(TENANT, "portalPlayerSelfCreateEnabled", true)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createSelfProfile(USER, new Player()));

        assertEquals(400, ex.getCode());
        assertEquals("自助建档未开放", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助建档：已有本人关联档案 → 400「您已有关联的球员档案」")
    void createSelfProfile_alreadyLinked_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(sysConfigService.getBoolean(TENANT, "portalPlayerSelfCreateEnabled", true)).thenReturn(true);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.of(player(1L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createSelfProfile(USER, new Player()));

        assertEquals(400, ex.getCode());
        assertEquals("您已有关联的球员档案", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助建档：已认领球员 → 400「您已认领球员，不能重复建档」")
    void createSelfProfile_alreadyClaimed_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(sysConfigService.getBoolean(TENANT, "portalPlayerSelfCreateEnabled", true)).thenReturn(true);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.empty());
        when(playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(USER, "approved")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createSelfProfile(USER, new Player()));

        assertEquals(400, ex.getCode());
        assertEquals("您已认领球员，不能重复建档", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助建档：同租户同名同生日去重命中 → 400「存在同名同生日的球员档案，请改用认领流程」")
    void createSelfProfile_duplicateNameBirthDate_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(sysConfigService.getBoolean(TENANT, "portalPlayerSelfCreateEnabled", true)).thenReturn(true);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.empty());
        when(playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(USER, "approved")).thenReturn(false);
        Player draft = new Player();
        draft.setName("张三");
        draft.setBirthDate("2000-01-01");
        when(playerRepository.findByNameAndBirthDateAndTenantIdAndDeletedAtIsNull("张三", "2000-01-01", TENANT))
                .thenReturn(List.of(player(2L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createSelfProfile(USER, draft));

        assertEquals(400, ex.getCode());
        assertEquals("存在同名同生日的球员档案，请改用认领流程", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助建档：校验通过 → 置入 tenantId/userId/status 并落库")
    void createSelfProfile_success_setsOwnershipAndStatus() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(sysConfigService.getBoolean(TENANT, "portalPlayerSelfCreateEnabled", true)).thenReturn(true);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.empty());
        when(playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(USER, "approved")).thenReturn(false);
        Player draft = new Player();
        draft.setName("张三");
        draft.setBirthDate("2000-01-01");
        when(playerRepository.findByNameAndBirthDateAndTenantIdAndDeletedAtIsNull("张三", "2000-01-01", TENANT))
                .thenReturn(List.of());
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
            Player arg = invocation.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(7L);
            }
            return arg;
        });

        Player result = service.createSelfProfile(USER, draft);

        assertEquals(7L, result.getId().longValue());
        assertEquals(Long.valueOf(TENANT), result.getTenantId());
        assertEquals(Long.valueOf(USER), result.getUserId());
        assertEquals("active", result.getStatus());
    }

    @Test
    @DisplayName("取本人档案：存在 → 返回该档案")
    void getSelfProfile_present_returnsProfile() {
        Player p = player(1L);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.of(p));

        assertSame(p, service.getSelfProfile(USER));
    }

    @Test
    @DisplayName("取本人档案：不存在 → 返回 null")
    void getSelfProfile_absent_returnsNull() {
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.empty());

        assertNull(service.getSelfProfile(USER));
    }

    @Test
    @DisplayName("本人档案编辑：经 guard 校验并按白名单生效（组图/JSON 走归一化链）")
    void updateSelfProfile_guardInvokedAndWhitelistApplied() {
        Player p = player(1L);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> body = new HashMap<>();
        body.put("height", "180");
        body.put("name", "李四");
        body.put("bgImages", List.of("a", "a", "b"));
        body.put("positions", List.of("P", "C"));

        Player result = service.updateSelfProfile(USER, 1L, body);

        verify(resourceGuard).assertCanEditPlayerProfile(1L, ResourceGuard.PlayerEditChannel.SELF);
        assertEquals("180", result.getHeight());
        assertEquals("李四", result.getName());
        assertEquals(List.of("a", "b"), result.getBgImages());
        assertEquals("a", result.getBgImage());
        assertEquals(List.of("P", "C"), result.getPositionsList());
        verify(playerRepository).save(p);
    }

    @Test
    @DisplayName("本人档案编辑：白名单外的键一律忽略")
    void updateSelfProfile_nonWhitelistedKeysIgnored() {
        Player p = player(1L);
        p.setNumber("7");
        p.setStatus("active");
        p.setTeamId(5L);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> body = new HashMap<>();
        body.put("number", "99");
        body.put("status", "retired");
        body.put("teamId", 8L);
        body.put("sort", 123);

        Player result = service.updateSelfProfile(USER, 1L, body);

        assertEquals("7", result.getNumber());
        assertEquals("active", result.getStatus());
        assertEquals(Long.valueOf(5L), result.getTeamId());
        assertEquals(Integer.valueOf(0), result.getSort());
    }

    @Test
    @DisplayName("本人档案编辑：目标球员不存在 → 404「球员不存在」")
    void updateSelfProfile_missingPlayer_notFound() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(USER, 1L, new HashMap<>()));

        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    private static Player player(Long id) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(TENANT);
        return p;
    }
}
