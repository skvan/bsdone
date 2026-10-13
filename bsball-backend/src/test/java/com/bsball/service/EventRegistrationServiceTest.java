package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EventTeamDto;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventRosterPlayer;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventRepository;
import com.bsball.repository.EventRosterPlayerRepository;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 电子秩序册（Issue #310）规则测试：邀请幂等/重邀、确认谢绝状态机、名单差量提交、
 * 锁定守卫与在役校验。纯单元测试（Mockito mock，不启动 Spring）。
 */
@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {
    private static final long TENANT_ID = 1L;
    private static final long UID = 9L;
    private static final long EVENT_ID = 700L;
    private static final long TEAM_ID = 100L;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerTeamRepository playerTeamRepository;
    @Mock
    private EventTeamRepository eventTeamRepository;
    @Mock
    private EventRosterPlayerRepository eventRosterPlayerRepository;
    @Mock
    private ResourceGuard resourceGuard;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    private EventRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new EventRegistrationService(eventRepository, teamRepository, playerRepository,
                playerTeamRepository, eventTeamRepository, eventRosterPlayerRepository,
                resourceGuard, tenantQueryPolicyService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    private static Event event(long id) {
        Event e = new Event();
        e.setId(Long.valueOf(id));
        e.setTenantId(Long.valueOf(TENANT_ID));
        e.setName("春季联赛");
        e.setSeason("2026");
        e.setGameMode("BASEBALL");
        return e;
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(Long.valueOf(id));
        t.setTenantId(Long.valueOf(tenantId));
        t.setName("雷霆队");
        return t;
    }

    private static Player player(long id, String name) {
        Player p = new Player();
        p.setId(Long.valueOf(id));
        p.setTenantId(Long.valueOf(TENANT_ID));
        p.setName(name);
        return p;
    }

    private static EventTeam reg(String status) {
        EventTeam r = new EventTeam();
        r.setId(Long.valueOf(1L));
        r.setTenantId(Long.valueOf(TENANT_ID));
        r.setEventId(Long.valueOf(EVENT_ID));
        r.setTeamId(Long.valueOf(TEAM_ID));
        r.setStatus(status);
        return r;
    }

    private static EventRosterPlayer rosterRow(long id, long playerId, int sort) {
        EventRosterPlayer r = new EventRosterPlayer();
        r.setId(Long.valueOf(id));
        r.setTenantId(Long.valueOf(TENANT_ID));
        r.setEventId(Long.valueOf(EVENT_ID));
        r.setTeamId(Long.valueOf(TEAM_ID));
        r.setPlayerId(Long.valueOf(playerId));
        r.setSort(Integer.valueOf(sort));
        return r;
    }

    private static PlayerTeam entry(long playerId, String number) {
        PlayerTeam e = new PlayerTeam();
        e.setPlayerId(Long.valueOf(playerId));
        e.setTeamId(Long.valueOf(TEAM_ID));
        e.setNumber(number);
        return e;
    }

    private void stubInviteContext() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
    }

    private void stubToDtoContext() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        when(eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(2L);
    }

    @Test
    @DisplayName("邀请：新球队创建为 invited（带邀请人/时间/备注）")
    void inviteTeams_newTeams_createdAsInvited() {
        stubInviteContext();
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.empty());
        CurrentUserHolder.set(UID, TENANT_ID);

        Map<String, Object> out = service.inviteTeams(EVENT_ID, List.of(TEAM_ID), "备注");

        assertEquals(1, out.get("invited"));
        assertEquals(0, out.get("skipped"));
        ArgumentCaptor<EventTeam> captor = ArgumentCaptor.forClass(EventTeam.class);
        verify(eventTeamRepository).save(captor.capture());
        assertEquals(EventTeam.STATUS_INVITED, captor.getValue().getStatus());
        assertEquals(Long.valueOf(UID), captor.getValue().getInvitedBy());
        assertEquals(Long.valueOf(TENANT_ID), captor.getValue().getTenantId());
        assertNotNull(captor.getValue().getInvitedAt());
    }

    @Test
    @DisplayName("邀请：谢绝球队被重新邀请时重置为 invited（清空确认/谢绝/提交时间）")
    void inviteTeams_declinedTeam_reinvitedReset() {
        stubInviteContext();
        EventTeam declined = reg(EventTeam.STATUS_DECLINED);
        declined.setDeclinedAt(LocalDateTime.now().minusDays(1));
        declined.setConfirmedAt(LocalDateTime.now().minusDays(2));
        declined.setRosterSubmittedAt(LocalDateTime.now().minusDays(2));
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(declined));
        CurrentUserHolder.set(UID, TENANT_ID);

        Map<String, Object> out = service.inviteTeams(EVENT_ID, List.of(TEAM_ID), null);

        assertEquals(1, out.get("invited"));
        assertEquals(EventTeam.STATUS_INVITED, declined.getStatus());
        assertNull(declined.getDeclinedAt());
        assertNull(declined.getConfirmedAt());
        assertNull(declined.getRosterSubmittedAt());
        assertEquals(Long.valueOf(UID), declined.getInvitedBy());
        verify(eventTeamRepository).save(declined);
    }

    @Test
    @DisplayName("邀请：已确认球队重复邀请被幂等跳过（无写）")
    void inviteTeams_confirmedTeam_skippedIdempotent() {
        stubInviteContext();
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));

        Map<String, Object> out = service.inviteTeams(EVENT_ID, List.of(TEAM_ID), null);

        assertEquals(1, out.get("skipped"));
        assertEquals(0, out.get("invited"));
        verify(eventTeamRepository, never()).save(any(EventTeam.class));
    }

    @Test
    @DisplayName("邀请：跨租户球队 → 400")
    void inviteTeams_crossTenantTeam_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(teamRepository.findById(200L)).thenReturn(Optional.of(team(200L, 2L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteTeams(EVENT_ID, List.of(200L), null));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("提交名单：球员当前不在本队（countCurrentEntry=0）→ 400 并列明球员")
    void submitRoster_playerNotCurrentInTeam_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, "张三")));
        when(playerTeamRepository.countCurrentEntry(5L, TEAM_ID)).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("张三"));
        verify(eventRosterPlayerRepository, never()).save(any(EventRosterPlayer.class));
    }

    @Test
    @DisplayName("提交名单：差量同步（新增排序/更新背号快照/软删缺省行）并刷新提交时间")
    void submitRoster_diffSync_addUpdateRemove() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EventTeam confirmed = reg(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(confirmed));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, "张三")));
        when(playerRepository.findById(6L)).thenReturn(Optional.of(player(6L, "李四")));
        when(playerTeamRepository.countCurrentEntry(5L, TEAM_ID)).thenReturn(1L);
        when(playerTeamRepository.countCurrentEntry(6L, TEAM_ID)).thenReturn(1L);
        EventRosterPlayer keep = rosterRow(11L, 5L, 3);
        EventRosterPlayer drop = rosterRow(12L, 7L, 4);
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(new ArrayList<>(List.of(keep, drop)));
        when(playerTeamRepository.findByTeamIdAndDeletedAtIsNullAndPlayerIdIn(TEAM_ID, List.of(5L, 6L)))
                .thenReturn(List.of(entry(5L, "7"), entry(6L, "21")));
        stubToDtoContext();
        CurrentUserHolder.set(UID, TENANT_ID);

        service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L, 6L));

        assertEquals(Integer.valueOf(0), keep.getSort());
        assertEquals("7", keep.getNumber());
        assertNotNull(drop.getDeletedAt());
        verify(eventRosterPlayerRepository).save(argThat(r -> Objects.equals(r.getPlayerId(), 6L)
                && Objects.equals(r.getSort(), 1) && "21".equals(r.getNumber())));
        assertNotNull(confirmed.getRosterSubmittedAt());
        verify(eventTeamRepository).save(confirmed);
    }

    @Test
    @DisplayName("提交名单：名单已锁定 → 403")
    void submitRoster_locked_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EventTeam locked = reg(EventTeam.STATUS_CONFIRMED);
        locked.setRosterLocked(Boolean.TRUE);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(locked));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).findById(any());
    }

    @Test
    @DisplayName("提交名单：未确认参加 → 400")
    void submitRoster_notConfirmed_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_INVITED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("请先确认参加"));
    }

    @Test
    @DisplayName("提交名单：空名单 → 400（至少 1 人）")
    void submitRoster_emptyList_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of()));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("至少"));
    }

    @Test
    @DisplayName("确认参加：invited → confirmed（写确认时间）")
    void confirm_invitedTeam_setsConfirmed() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EventTeam invited = reg(EventTeam.STATUS_INVITED);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(invited));
        stubToDtoContext();

        EventTeamDto dto = service.confirm(EVENT_ID, TEAM_ID);

        assertEquals(EventTeam.STATUS_CONFIRMED, invited.getStatus());
        assertNotNull(invited.getConfirmedAt());
        assertEquals(EventTeam.STATUS_CONFIRMED, dto.status());
        verify(eventTeamRepository).save(invited);
    }

    @Test
    @DisplayName("确认参加：已确认幂等返回（无写）")
    void confirm_confirmedTeam_idempotent() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));
        stubToDtoContext();

        service.confirm(EVENT_ID, TEAM_ID);

        verify(eventTeamRepository, never()).save(any(EventTeam.class));
    }

    @Test
    @DisplayName("谢绝：已确认球队不能直接谢绝 → 400")
    void decline_confirmedTeam_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.decline(EVENT_ID, TEAM_ID));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已确认参赛"));
    }

    @Test
    @DisplayName("移除参赛：名单已锁定 → 400（先解锁）")
    void removeEventTeam_locked_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EventTeam locked = reg(EventTeam.STATUS_CONFIRMED);
        locked.setRosterLocked(Boolean.TRUE);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(locked));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.removeEventTeam(EVENT_ID, TEAM_ID));

        assertEquals(400, ex.getCode());
        verify(eventRosterPlayerRepository, never())
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID);
    }

    @Test
    @DisplayName("移除参赛：软删登记与全部名单行")
    void removeEventTeam_softDeletesRegistrationAndRoster() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EventTeam confirmed = reg(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(confirmed));
        EventRosterPlayer r1 = rosterRow(11L, 5L, 0);
        EventRosterPlayer r2 = rosterRow(12L, 6L, 1);
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(List.of(r1, r2));
        CurrentUserHolder.set(UID, TENANT_ID);

        service.removeEventTeam(EVENT_ID, TEAM_ID);

        assertNotNull(confirmed.getDeletedAt());
        assertNotNull(r1.getDeletedAt());
        assertNotNull(r2.getDeletedAt());
        assertEquals(Long.valueOf(UID), confirmed.getDeletedBy());
        verify(eventTeamRepository).save(confirmed);
        verify(eventRosterPlayerRepository).save(r1);
        verify(eventRosterPlayerRepository).save(r2);
    }

    @Test
    @DisplayName("锁定/解锁：设置与清空锁定留痕")
    void lockRoster_setsFlags_andUnlockClears() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EventTeam confirmed = reg(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(confirmed));
        stubToDtoContext();
        CurrentUserHolder.set(UID, TENANT_ID);

        service.lockRoster(EVENT_ID, TEAM_ID, true);

        assertEquals(Boolean.TRUE, confirmed.getRosterLocked());
        assertEquals(Long.valueOf(UID), confirmed.getLockedBy());
        assertNotNull(confirmed.getLockedAt());

        service.lockRoster(EVENT_ID, TEAM_ID, false);

        assertEquals(Boolean.FALSE, confirmed.getRosterLocked());
        assertNull(confirmed.getLockedBy());
        assertNull(confirmed.getLockedAt());
    }

    @Test
    @DisplayName("我的报名：装配赛事名/赛季/模式与名单人数")
    void listTeamRegistrations_joinsEventInfo() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(TEAM_ID))
                .thenReturn(List.of(reg(EventTeam.STATUS_CONFIRMED)));
        when(eventRepository.findAllById(List.of(EVENT_ID))).thenReturn(List.of(event(EVENT_ID)));
        when(eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(2L);

        List<EventTeamDto> out = service.listTeamRegistrations(TEAM_ID);

        assertEquals(1, out.size());
        assertEquals("春季联赛", out.get(0).eventName());
        assertEquals("2026", out.get(0).season());
        assertEquals(2L, out.get(0).playerCount());
        assertEquals(EventTeam.STATUS_CONFIRMED, out.get(0).status());
    }
}
