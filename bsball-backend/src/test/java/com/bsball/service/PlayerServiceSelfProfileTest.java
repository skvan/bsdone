package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

        Player result = service.updateSelfProfile(1L, body);
        
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

        Player result = service.updateSelfProfile(1L, body);
        
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
                () -> service.updateSelfProfile(1L, new HashMap<>()));

        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("自助建档：裁剪客户端可注入字段（id/ROSTER 语义）并归一化组图")
    void createSelfProfile_prunesInjectedFields() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(sysConfigService.getBoolean(TENANT, "portalPlayerSelfCreateEnabled", true)).thenReturn(true);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(USER)).thenReturn(Optional.empty());
        when(playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(USER, "approved")).thenReturn(false);
        Player draft = new Player();
        draft.setName("张三");
        draft.setBirthDate("2000-01-01");
        // 注入：id 走 update 语义 + ROSTER 语义字段 + 超限且重复的组图
        draft.setId(99L);
        draft.setTeamId(5L);
        draft.setNumber("7");
        draft.setCurrentJoinRecordId(3L);
        draft.setSort(9);
        draft.setDeletedAt(LocalDateTime.now());
        draft.setDeletedBy(1L);
        draft.setBgImages(List.of("a", "a", "b", "c", "d", "e", "f"));
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

        // id 注入被清空 → 走 insert 语义（save mock 仅当 id 为 null 时赋 7）
        assertEquals(7L, result.getId().longValue());
        assertNull(result.getTeamId());
        assertNull(result.getNumber());
        assertNull(result.getCurrentJoinRecordId());
        assertNull(result.getSort());
        assertNull(result.getDeletedAt());
        assertNull(result.getDeletedBy());
        // 组图去重并截断至 5 张（normalizePlayerBackgroundFields）
        assertEquals(List.of("a", "b", "c", "d", "e"), result.getBgImages());
        assertEquals("a", result.getBgImage());
        assertEquals(Long.valueOf(TENANT), result.getTenantId());
        assertEquals(Long.valueOf(USER), result.getUserId());
        assertEquals("active", result.getStatus());
    }

    @Test
    @DisplayName("本人档案编辑：目标档案已软删 → 404「球员不存在」")
    void updateSelfProfile_softDeleted_notFound() {
        Player p = player(1L);
        p.setDeletedAt(LocalDateTime.now());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSelfProfile(1L, new HashMap<>()));

        assertEquals(404, ex.getCode());
        assertEquals("球员不存在", ex.getMessage());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("白名单一致性：SELF_EDITABLE 全部 20 键均有 switch 分支（逐键生效）")
    void updateSelfProfile_whitelistCoversEverySwitchBranch() {
        Map<String, Object> samples = new LinkedHashMap<>();
        samples.put("height", "180");
        samples.put("weight", "75");
        samples.put("throwHand", "R");
        samples.put("batHand", "L");
        samples.put("avatar", "avatar-1");
        samples.put("bgImage", "bg-image-1");
        samples.put("bgImages", List.of("u1", "u2"));
        samples.put("bgFocusConfig", Map.of("scale", 2));
        samples.put("nickname", "nick");
        samples.put("nameEn", "Ben");
        samples.put("birthDate", "2000-02-02");
        samples.put("birthPlace", "北京");
        samples.put("education", "本科");
        samples.put("intro", "简介文本");
        samples.put("contactPhone", "13800000000");
        samples.put("contactEmail", "a@b.com");
        samples.put("draft", "draft-1");
        samples.put("debut", "debut-1");
        samples.put("name", "李四");
        samples.put("positions", List.of("P", "C"));

        assertEquals(20, samples.size(), "SELF_EDITABLE 白名单应为 20 个键");

        final Player[] holder = new Player[1];
        when(playerRepository.findById(1L)).thenAnswer(invocation -> Optional.of(holder[0]));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (Map.Entry<String, Object> e : samples.entrySet()) {
            holder[0] = player(1L);
            Player result = service.updateSelfProfile(1L, Map.of(e.getKey(), e.getValue()));
            assertTrue(fieldApplied(e.getKey(), result),
                    "键 " + e.getKey() + " 未被 switch 分支处理（白名单与 switch 可能不一致）");
        }
    }

    @Test
    @DisplayName("本人档案编辑：非标量值传入标量字段 → 跳过该键（保持原值）")
    void updateSelfProfile_nonScalarValueOnScalarField_skipped() {
        Player p = player(1L);
        p.setHeight("170");
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> body = new HashMap<>();
        body.put("height", Map.of("a", 1));

        Player result = service.updateSelfProfile(1L, body);

        assertEquals("170", result.getHeight());
        verify(playerRepository).save(p);
    }

    @Test
    @DisplayName("本人档案编辑：显式 null 值 → 透传清空字段（区别于非标量跳过）")
    void updateSelfProfile_nullValue_clearsField() {
        Player p = player(1L);
        p.setHeight("170");
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> body = new HashMap<>();
        body.put("height", null);

        Player result = service.updateSelfProfile(1L, body);

        assertNull(result.getHeight());
        verify(playerRepository).save(p);
    }

    /** 断言指定白名单键经 updateSelfProfile 后目标字段确已生效（值随样本固定）。 */
    private static boolean fieldApplied(String key, Player p) {
        return switch (key) {
            case "height" -> "180".equals(p.getHeight());
            case "weight" -> "75".equals(p.getWeight());
            case "throwHand" -> "R".equals(p.getThrowHand());
            case "batHand" -> "L".equals(p.getBatHand());
            case "avatar" -> "avatar-1".equals(p.getAvatar());
            case "bgImage" -> "bg-image-1".equals(p.getBgImage());
            case "bgImages" -> List.of("u1", "u2").equals(p.getBgImages());
            case "bgFocusConfig" -> Map.of("scale", 2).equals(p.getBgFocusConfig());
            case "nickname" -> "nick".equals(p.getNickname());
            case "nameEn" -> "Ben".equals(p.getNameEn());
            case "birthDate" -> "2000-02-02".equals(p.getBirthDate());
            case "birthPlace" -> "北京".equals(p.getBirthPlace());
            case "education" -> "本科".equals(p.getEducation());
            case "intro" -> "简介文本".equals(p.getIntro());
            case "contactPhone" -> "13800000000".equals(p.getContactPhone());
            case "contactEmail" -> "a@b.com".equals(p.getContactEmail());
            case "draft" -> "draft-1".equals(p.getDraft());
            case "debut" -> "debut-1".equals(p.getDebut());
            case "name" -> "李四".equals(p.getName());
            case "positions" -> List.of("P", "C").equals(p.getPositionsList());
            default -> false;
        };
    }

    private static Player player(Long id) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(TENANT);
        return p;
    }
}
