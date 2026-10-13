package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EventRosterPlayerDto;
import com.bsball.model.dto.EventTeamDto;
import com.bsball.model.dto.RosterPlayersDto;
import com.bsball.model.dto.TeamOptionDto;
import com.bsball.model.dto.TeamPlayerOptionDto;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.EventRosterPlayer;
import com.bsball.model.entity.EventTeam;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.EventRepository;
import com.bsball.repository.EventRosterPlayerRepository;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GamePlayerStatRepository;
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
 * 锁定守卫与在役校验、软删赛事拦截、守卫越权无写。纯单元测试（Mockito mock，不启动 Spring）。
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
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private ResourceGuard resourceGuard;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    private EventRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new EventRegistrationService(eventRepository, teamRepository, playerRepository,
                playerTeamRepository, eventTeamRepository, eventRosterPlayerRepository,
                gamePlayerStatRepository, playerService, resourceGuard, tenantQueryPolicyService);
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
        return team(id, tenantId, "雷霆队");
    }

    private static Team team(long id, long tenantId, String name) {
        Team t = new Team();
        t.setId(Long.valueOf(id));
        t.setTenantId(Long.valueOf(tenantId));
        t.setName(name);
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
        return reg(EVENT_ID, TEAM_ID, status);
    }

    private static EventTeam reg(long eventId, long teamId, String status) {
        EventTeam r = new EventTeam();
        r.setId(Long.valueOf(1L));
        r.setTenantId(Long.valueOf(TENANT_ID));
        r.setEventId(Long.valueOf(eventId));
        r.setTeamId(Long.valueOf(teamId));
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

    /** 本队 current 在役经历行（默认 current=true）。 */
    private static PlayerTeam entry(long playerId, String number) {
        PlayerTeam e = new PlayerTeam();
        e.setPlayerId(Long.valueOf(playerId));
        e.setTeamId(Long.valueOf(TEAM_ID));
        e.setNumber(number);
        e.setCurrent(Boolean.TRUE);
        return e;
    }

    /** 邀请上下文：租户 + 赛事行锁查询 + 球队。 */
    private void stubInviteContext() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
    }

    /** 球队侧上下文：租户 + 赛事存活查询。 */
    private void stubEventContext() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
    }

    private void stubToDtoContext() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        when(eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(2L);
    }

    @Test
    @DisplayName("邀请：新球队创建为 invited（带邀请人/时间/备注），且走过赛事管理守卫")
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
        verify(resourceGuard).assertCanManageEvent(EVENT_ID);
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
        when(eventRepository.findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(teamRepository.findById(200L)).thenReturn(Optional.of(team(200L, 2L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteTeams(EVENT_ID, List.of(200L), null));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("邀请：空列表 → 400（服务层校验，避免 500 文案）")
    void inviteTeams_emptyList_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteTeams(EVENT_ID, List.of(), null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("请选择要邀请的球队"));
        verify(eventTeamRepository, never()).save(any(EventTeam.class));
    }

    @Test
    @DisplayName("邀请：备注超长 → 400")
    void inviteTeams_remarkTooLong_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteTeams(EVENT_ID, List.of(TEAM_ID), "x".repeat(501)));

        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("邀请：守卫拒绝（403）时无任何写入")
    void inviteTeams_guardForbidden_noWrite() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException(403, "无权管理该赛事")).when(resourceGuard).assertCanManageEvent(EVENT_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteTeams(EVENT_ID, List.of(TEAM_ID), null));

        assertEquals(403, ex.getCode());
        verify(eventTeamRepository, never()).save(any(EventTeam.class));
    }

    @Test
    @DisplayName("提交名单：球员当前不在本队（批量校验）→ 400 并列明球员，无写入")
    void submitRoster_playerNotCurrentInTeam_rejected() {
        stubEventContext();
        when(eventTeamRepository.findForUpdate(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));
        when(playerRepository.findAllById(List.of(5L))).thenReturn(List.of(player(5L, "张三")));
        PlayerTeam former = entry(5L, "7");
        former.setCurrent(Boolean.FALSE);
        when(playerTeamRepository.findByTeamIdAndDeletedAtIsNullAndPlayerIdIn(TEAM_ID, List.of(5L)))
                .thenReturn(List.of(former));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("张三"));
        verify(eventRosterPlayerRepository, never()).save(any(EventRosterPlayer.class));
        verify(resourceGuard).assertCanStewardOrManageTeam(TEAM_ID);
    }

    @Test
    @DisplayName("提交名单：差量同步（新增排序/更新背号快照/软删缺省行）并刷新提交时间")
    void submitRoster_diffSync_addUpdateRemove() {
        stubEventContext();
        EventTeam confirmed = reg(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findForUpdate(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(confirmed));
        when(playerRepository.findAllById(List.of(5L, 6L)))
                .thenReturn(List.of(player(5L, "张三"), player(6L, "李四")));
        when(playerTeamRepository.findByTeamIdAndDeletedAtIsNullAndPlayerIdIn(TEAM_ID, List.of(5L, 6L)))
                .thenReturn(List.of(entry(5L, "7"), entry(6L, "21")));
        EventRosterPlayer keep = rosterRow(11L, 5L, 3);
        EventRosterPlayer drop = rosterRow(12L, 7L, 4);
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(new ArrayList<>(List.of(keep, drop)));
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
    @DisplayName("提交名单：名单已锁定 → 403，无球员查询")
    void submitRoster_locked_rejected() {
        stubEventContext();
        EventTeam locked = reg(EventTeam.STATUS_CONFIRMED);
        locked.setRosterLocked(Boolean.TRUE);
        when(eventTeamRepository.findForUpdate(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(locked));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("提交名单：未确认参加 → 400")
    void submitRoster_notConfirmed_rejected() {
        stubEventContext();
        when(eventTeamRepository.findForUpdate(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_INVITED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("请先确认参加"));
    }

    @Test
    @DisplayName("提交名单：空名单 → 400（至少 1 人）")
    void submitRoster_emptyList_rejected() {
        stubEventContext();
        when(eventTeamRepository.findForUpdate(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of()));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("至少"));
    }

    @Test
    @DisplayName("提交名单：赛事已软删（归还）→ 404，球队侧不可再写")
    void submitRoster_deletedEvent_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event deleted = event(EVENT_ID);
        deleted.setDeletedAt(LocalDateTime.now());
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(deleted));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitRoster(EVENT_ID, TEAM_ID, List.of(5L)));

        assertEquals(404, ex.getCode());
        verify(eventTeamRepository, never()).findForUpdate(EVENT_ID, TEAM_ID);
    }

    @Test
    @DisplayName("确认参加：invited → confirmed（写确认时间），走过球队侧守卫")
    void confirm_invitedTeam_setsConfirmed() {
        stubEventContext();
        EventTeam invited = reg(EventTeam.STATUS_INVITED);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(invited));
        stubToDtoContext();

        EventTeamDto dto = service.confirm(EVENT_ID, TEAM_ID);

        assertEquals(EventTeam.STATUS_CONFIRMED, invited.getStatus());
        assertNotNull(invited.getConfirmedAt());
        assertEquals(EventTeam.STATUS_CONFIRMED, dto.status());
        verify(eventTeamRepository).save(invited);
        verify(resourceGuard).assertCanStewardOrManageTeam(TEAM_ID);
    }

    @Test
    @DisplayName("确认参加：已确认幂等返回（返回体状态正确、无写）")
    void confirm_confirmedTeam_idempotent() {
        stubEventContext();
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));
        stubToDtoContext();

        EventTeamDto dto = service.confirm(EVENT_ID, TEAM_ID);

        assertEquals(EventTeam.STATUS_CONFIRMED, dto.status());
        verify(eventTeamRepository, never()).save(any(EventTeam.class));
    }

    @Test
    @DisplayName("确认参加：已谢绝 → 400 引导重邀")
    void confirm_declinedTeam_rejected() {
        stubEventContext();
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_DECLINED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirm(EVENT_ID, TEAM_ID));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("重新邀请"));
    }

    @Test
    @DisplayName("谢绝：invited → declined（写谢绝时间）")
    void decline_invitedTeam_setsDeclined() {
        stubEventContext();
        EventTeam invited = reg(EventTeam.STATUS_INVITED);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(invited));
        stubToDtoContext();

        EventTeamDto dto = service.decline(EVENT_ID, TEAM_ID);

        assertEquals(EventTeam.STATUS_DECLINED, invited.getStatus());
        assertNotNull(invited.getDeclinedAt());
        assertEquals(EventTeam.STATUS_DECLINED, dto.status());
        verify(eventTeamRepository).save(invited);
        verify(resourceGuard).assertCanStewardOrManageTeam(TEAM_ID);
    }

    @Test
    @DisplayName("谢绝：守卫拒绝（403）时无任何写入")
    void decline_guardForbidden_noWrite() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException(403, "无权管理该球队")).when(resourceGuard).assertCanStewardOrManageTeam(TEAM_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.decline(EVENT_ID, TEAM_ID));

        assertEquals(403, ex.getCode());
        verify(eventTeamRepository, never()).save(any(EventTeam.class));
    }

    @Test
    @DisplayName("谢绝：已确认球队不能直接谢绝 → 400")
    void decline_confirmedTeam_rejected() {
        stubEventContext();
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.decline(EVENT_ID, TEAM_ID));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已确认参赛"));
    }

    @Test
    @DisplayName("移除参赛：名单已锁定 → 403（先解锁）")
    void removeEventTeam_locked_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        EventTeam locked = reg(EventTeam.STATUS_CONFIRMED);
        locked.setRosterLocked(Boolean.TRUE);
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(Optional.of(locked));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.removeEventTeam(EVENT_ID, TEAM_ID));

        assertEquals(403, ex.getCode());
        verify(eventRosterPlayerRepository, never())
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID);
    }

    @Test
    @DisplayName("移除参赛：软删登记与全部名单行，走过赛事管理守卫")
    void removeEventTeam_softDeletesRegistrationAndRoster() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
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
        verify(resourceGuard).assertCanManageEvent(EVENT_ID);
    }

    @Test
    @DisplayName("锁定/解锁：设置与清空锁定留痕")
    void lockRoster_setsFlags_andUnlockClears() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
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
        verify(resourceGuard, org.mockito.Mockito.times(2)).assertCanManageEvent(EVENT_ID);
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

    @Test
    @DisplayName("我的报名：已软删赛事的登记不展示")
    void listTeamRegistrations_skipsDeletedEvents() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        EventTeam r2 = reg(701L, TEAM_ID, EventTeam.STATUS_CONFIRMED);
        EventTeam r1 = reg(EventTeam.STATUS_CONFIRMED);
        when(eventTeamRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(TEAM_ID)).thenReturn(List.of(r2, r1));
        Event deleted = event(701L);
        deleted.setDeletedAt(LocalDateTime.now());
        when(eventRepository.findAllById(List.of(701L, EVENT_ID))).thenReturn(List.of(deleted, event(EVENT_ID)));
        when(eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID)).thenReturn(0L);

        List<EventTeamDto> out = service.listTeamRegistrations(TEAM_ID);

        assertEquals(1, out.size());
        assertEquals(Long.valueOf(EVENT_ID), out.get(0).eventId());
    }

    @Test
    @DisplayName("赛事参赛列表：装配球队名与名单人数（含空名单队）")
    void listEventTeams_assemblesTeamAndCounts() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        EventTeam r1 = reg(EventTeam.STATUS_CONFIRMED);
        EventTeam r2 = reg(EVENT_ID, 200L, EventTeam.STATUS_INVITED);
        when(eventTeamRepository.findByEventIdAndDeletedAtIsNullOrderByIdAsc(EVENT_ID)).thenReturn(List.of(r1, r2));
        when(teamRepository.findAllById(List.of(100L, 200L)))
                .thenReturn(List.of(team(100L, TENANT_ID), team(200L, TENANT_ID, "雄鹰队")));
        when(eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, 100L)).thenReturn(3L);
        when(eventRosterPlayerRepository.countByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, 200L)).thenReturn(0L);

        List<EventTeamDto> out = service.listEventTeams(EVENT_ID);

        assertEquals(2, out.size());
        assertEquals("雷霆队", out.get(0).teamName());
        assertEquals(3L, out.get(0).playerCount());
        assertEquals("雄鹰队", out.get(1).teamName());
        assertEquals(0L, out.get(1).playerCount());
    }

    @Test
    @DisplayName("名单明细：背号快照缺失回落档案背号；已离队球员 currentInTeam=false")
    void roster_marksFormerPlayerAndFallsBackNumber() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(eventTeamRepository.findByEventIdAndTeamIdAndDeletedAtIsNull(EVENT_ID, TEAM_ID))
                .thenReturn(Optional.of(reg(EventTeam.STATUS_CONFIRMED)));
        EventRosterPlayer r1 = rosterRow(11L, 5L, 0);
        r1.setNumber(null);
        EventRosterPlayer r2 = rosterRow(12L, 6L, 1);
        r2.setNumber("8");
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(List.of(r1, r2));
        Player p5 = player(5L, "张三");
        p5.setNumber("7");
        Player p6 = player(6L, "李四");
        when(playerRepository.findAllById(List.of(5L, 6L))).thenReturn(List.of(p5, p6));
        when(playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullAndCurrentTrue(List.of(5L, 6L)))
                .thenReturn(List.of(entry(6L, "8")));

        List<EventRosterPlayerDto> out = service.roster(EVENT_ID, TEAM_ID);

        assertEquals(2, out.size());
        assertEquals("7", out.get(0).number(), "快照缺失应回落档案背号");
        assertEquals(Boolean.FALSE, out.get(0).currentInTeam(), "已离队球员应标记不在队");
        assertEquals("8", out.get(1).number());
        assertEquals(Boolean.TRUE, out.get(1).currentInTeam());
    }

    @Test
    @DisplayName("限制判定：赛事存在任意参赛记录即启用（含仅邀请）")
    void eventRestricted_matchesAnyRegistrationRow() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(true);
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(701L)).thenReturn(false);

        assertTrue(service.eventRestricted(EVENT_ID));
        assertFalse(service.eventRestricted(701L));
        assertFalse(service.eventRestricted(null));
    }

    @Test
    @DisplayName("阵容池：受限赛事只返回报名球员（背号快照优先、缺失回落档案）")
    void rosterPlayers_restricted_returnsRosterOnly() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(true);
        EventRosterPlayer r1 = rosterRow(11L, 5L, 0);
        r1.setNumber("7");
        EventRosterPlayer r2 = rosterRow(12L, 6L, 1);
        r2.setNumber(null);
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(List.of(r1, r2));
        Player p6 = player(6L, "李四");
        p6.setNumber("21");
        when(playerRepository.findAllById(List.of(5L, 6L))).thenReturn(List.of(player(5L, "张三"), p6));

        RosterPlayersDto out = service.rosterPlayers(EVENT_ID, TEAM_ID);

        assertEquals(Boolean.TRUE, out.restricted());
        assertEquals(2, out.players().size());
        assertEquals("7", out.players().get(0).number());
        assertEquals("21", out.players().get(1).number());
    }

    @Test
    @DisplayName("阵容池：未受限赛事回落本队全量在役（复用球队球员选项目径）")
    void rosterPlayers_notRestricted_fallsBackToFullTeam() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EVENT_ID)));
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(false);
        when(playerService.listTeamPlayerOptions(TEAM_ID)).thenReturn(List.of(
                new TeamPlayerOptionDto(5L, "张三", "7", List.of("P"), "R", "R", "active")));

        RosterPlayersDto out = service.rosterPlayers(EVENT_ID, TEAM_ID);

        assertEquals(Boolean.FALSE, out.restricted());
        assertEquals(1, out.players().size());
        assertEquals(List.of("P"), out.players().get(0).positions());
    }

    @Test
    @DisplayName("门①：受限赛事下快照含名单外球员 → 400（消息含姓名与球队名）")
    void assertLineupSnapshotInRoster_blocksNonRosterPlayer() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(true);
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(List.of(rosterRow(11L, 5L, 0)));
        when(playerRepository.findById(6L)).thenReturn(Optional.of(player(6L, "李四")));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        String json = "{\"homeLineup\":[{\"id\":5},{\"id\":6}],\"homeBench\":[{\"id\":5}]}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertLineupSnapshotInRoster(EVENT_ID, TEAM_ID, null, json));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("李四"));
        assertTrue(ex.getMessage().contains("雷霆队"));
    }

    @Test
    @DisplayName("门①：全为报名球员（含替补与守备投手）通过；非法 JSON 不拦")
    void assertLineupSnapshotInRoster_passesForRosterPlayers() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(true);
        when(eventRosterPlayerRepository.findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID))
                .thenReturn(List.of(rosterRow(11L, 5L, 0), rosterRow(12L, 6L, 1)));
        String json = "{\"homeLineup\":[{\"id\":5}],\"homeBench\":[{\"id\":6}],\"homeFieldingPitcherId\":6}";

        service.assertLineupSnapshotInRoster(EVENT_ID, TEAM_ID, null, json);
        service.assertLineupSnapshotInRoster(EVENT_ID, TEAM_ID, null, "not-a-json");
        // 非法 JSON 应解析失败即短路（仅第一次调用命中名单查询）
        verify(eventRosterPlayerRepository, org.mockito.Mockito.times(1))
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID);
    }

    @Test
    @DisplayName("门①：未受限赛事跳过校验（含名单外球员也不拦）")
    void assertLineupSnapshotInRoster_notRestricted_skips() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(false);

        service.assertLineupSnapshotInRoster(EVENT_ID, TEAM_ID, null, "{\"homeLineup\":[{\"id\":999}]}");

        verify(eventRosterPlayerRepository, never())
                .findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(EVENT_ID, TEAM_ID);
    }

    @Test
    @DisplayName("门②：本场首次引入名单外球员 → 400")
    void assertStatsPartsInRoster_blocksNewNonRosterPlayer() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(true);
        when(eventRosterPlayerRepository.findByEventIdAndDeletedAtIsNull(EVENT_ID))
                .thenReturn(List.of(rosterRow(11L, 5L, 0)));
        when(gamePlayerStatRepository.findByGameId(900L)).thenReturn(List.of());
        when(playerRepository.findById(6L)).thenReturn(Optional.of(player(6L, "李四")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assertStatsPartsInRoster(
                EVENT_ID, 900L, List.of(new EventRegistrationService.StatsRef(100L, 6L))));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("李四"));
    }

    @Test
    @DisplayName("门②：已有本场统计行的球员更新不受限（首次引入口径）")
    void assertStatsPartsInRoster_allowsExistingStatPlayer() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(true);
        when(eventRosterPlayerRepository.findByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(List.of());
        GamePlayerStat stat = new GamePlayerStat();
        stat.setGameId(900L);
        stat.setTeamId(100L);
        stat.setPlayerId(7L);
        when(gamePlayerStatRepository.findByGameId(900L)).thenReturn(List.of(stat));

        service.assertStatsPartsInRoster(EVENT_ID, 900L,
                List.of(new EventRegistrationService.StatsRef(100L, 7L)));
    }

    @Test
    @DisplayName("门②：未受限赛事/空 gameId 跳过")
    void assertStatsPartsInRoster_skipsWhenNotRestricted() {
        when(eventTeamRepository.existsByEventIdAndDeletedAtIsNull(EVENT_ID)).thenReturn(false);

        service.assertStatsPartsInRoster(EVENT_ID, 900L,
                List.of(new EventRegistrationService.StatsRef(100L, 999L)));
        service.assertStatsPartsInRoster(EVENT_ID, null,
                List.of(new EventRegistrationService.StatsRef(100L, 999L)));

        verify(eventRosterPlayerRepository, never()).findByEventIdAndDeletedAtIsNull(EVENT_ID);
    }

    @Test
    @DisplayName("赛事可邀请球队：返回本租户全部球队并过赛事管理守卫（Issue #315）")
    void listInvitableTeams_returnsTenantTeams() {
        stubEventContext();
        List<TeamOptionDto> teams = List.of(
                new TeamOptionDto(1L, "暴力羊", null, null),
                new TeamOptionDto(3L, "野钻", null, null));
        when(teamRepository.findForSelectByTenantId(TENANT_ID)).thenReturn(teams);

        List<TeamOptionDto> out = service.listInvitableTeams(EVENT_ID);

        assertEquals(teams, out);
        verify(resourceGuard).assertCanManageEvent(EVENT_ID);
    }

    @Test
    @DisplayName("赛事可邀请球队：守卫拒绝（403）时零查询")
    void listInvitableTeams_guardForbidden_noQuery() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException(403, "无权管理该赛事")).when(resourceGuard).assertCanManageEvent(EVENT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.listInvitableTeams(EVENT_ID));

        assertEquals(403, ex.getCode());
        verify(teamRepository, never()).findForSelectByTenantId(any());
    }
}
