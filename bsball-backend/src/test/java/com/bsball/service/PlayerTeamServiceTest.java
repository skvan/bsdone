package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.PlayerTeamService.PlayerTeamSyncPlan;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
@DisplayName("PlayerTeamService：球员多队经历的计划、镜像与落库")
class PlayerTeamServiceTest {

    private static final long TENANT = 9L;
    private static final long PLAYER_ID = 1L;

    @Mock
    private PlayerTeamRepository playerTeamRepository;

    @Mock
    private TeamRepository teamRepository;

    private PlayerTeamService service;

    @BeforeEach
    void setUp() {
        service = new PlayerTeamService(playerTeamRepository, teamRepository);
    }

    @Test
    @DisplayName("提供 teamEntries：新增段、复用段更新、缺失段软删")
    void plan_entriesProvided_addsReusesAndDeletes() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        e1.setPositionsList(List.of("C"));
        PlayerTeam e2 = entry(12L, 6L, "8", false, 1);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1, e2));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));
        when(teamRepository.findById(7L)).thenReturn(Optional.of(team(7L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, List.of(
                dto(5L, "77", List.of("P"), true),
                dto(7L, "9", List.of("C"), false)), true);

        assertEquals(1, plan.toCreate().size());
        PlayerTeam created = plan.toCreate().get(0);
        assertEquals(7L, created.getTeamId().longValue());
        assertEquals(TENANT, created.getTenantId().longValue());
        assertEquals("9", created.getNumber());
        assertEquals(Boolean.FALSE, created.getCurrent());
        assertEquals(List.of("C"), created.getPositionsList());

        assertEquals(List.of(e1), plan.toUpdate());
        assertEquals("77", e1.getNumber());
        assertEquals(List.of("P"), e1.getPositionsList());

        assertEquals(List.of(e2), plan.toDelete());
        assertSame(e1, plan.primary());
        assertEquals(Set.of(5L), plan.beforeCurrentTeamIds());
        assertEquals(Set.of(5L), plan.afterCurrentTeamIds());
    }

    @Test
    @DisplayName("提供 teamEntries：取消勾选仅取消当前、经历保留并清空镜像")
    void plan_entriesProvided_uncheckKeepsHistoryAndClearsMirror() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        e1.setPositionsList(List.of("P"));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, List.of(dto(5L, "7", List.of("P"), false)), true);

        assertEquals(Boolean.FALSE, e1.getCurrent());
        assertEquals(List.of(e1), plan.toUpdate());
        assertTrue(plan.toDelete().isEmpty());
        assertNull(plan.primary());
        assertTrue(plan.afterCurrentTeamIds().isEmpty());

        p.setTeamId(5L);
        p.setNumber("7");
        service.applyMirror(p, plan);
        assertNull(p.getTeamId());
        assertNull(p.getNumber());
        assertNull(p.getPositions());
    }

    @Test
    @DisplayName("提供 teamEntries：重复球队被拒绝（400）")
    void plan_entriesProvided_duplicateTeamRejected() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.plan(p, List.of(
                dto(5L, "7", List.of("P"), true),
                dto(5L, "8", List.of("C"), false)), true));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("提供 teamEntries：球队不存在被拒绝（400）")
    void plan_entriesProvided_teamNotFoundRejected() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.plan(p, List.of(dto(8L, "7", List.of("P"), true)), true));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("球队不存在"));
    }

    @Test
    @DisplayName("提供 teamEntries：球队与当前租户不一致被拒绝（400）")
    void plan_entriesProvided_teamTenantMismatchRejected() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of());
        when(teamRepository.findById(8L)).thenReturn(Optional.of(team(8L, 99L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.plan(p, List.of(dto(8L, "7", List.of("P"), true)), true));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("租户"));
    }

    @Test
    @DisplayName("旧字段兼容：指定新球队时新增一条当前段，其他段不动")
    void plan_legacy_teamChangeAddsCurrentEntry() {
        Player p = player(PLAYER_ID, TENANT);
        p.setTeamId(6L);
        p.setNumber("8");
        p.setPositionsList(List.of("1B"));
        PlayerTeam e1 = entry(11L, 5L, "7", false, 0);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1));
        when(teamRepository.findById(6L)).thenReturn(Optional.of(team(6L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, null, false);

        assertEquals(1, plan.toCreate().size());
        PlayerTeam created = plan.toCreate().get(0);
        assertEquals(6L, created.getTeamId().longValue());
        assertEquals(Boolean.TRUE, created.getCurrent());
        assertEquals("8", created.getNumber());
        assertEquals(List.of("1B"), created.getPositionsList());
        assertTrue(plan.toUpdate().isEmpty());
        assertTrue(plan.toDelete().isEmpty());
        assertEquals(Set.of(6L), plan.afterCurrentTeamIds());
        assertTrue(plan.beforeCurrentTeamIds().isEmpty());

        service.applyMirror(p, plan);
        assertEquals(6L, p.getTeamId().longValue());
        assertEquals("8", p.getNumber());
    }

    @Test
    @DisplayName("旧字段兼容：清空球队时取消全部当前段")
    void plan_legacy_clearTeamUnchecksAll() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        PlayerTeam e2 = entry(12L, 6L, "8", true, 1);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1, e2));

        PlayerTeamSyncPlan plan = service.plan(p, null, false);

        assertEquals(List.of(e1, e2), plan.toUpdate());
        assertEquals(Boolean.FALSE, e1.getCurrent());
        assertEquals(Boolean.FALSE, e2.getCurrent());
        assertTrue(plan.toDelete().isEmpty());
        assertNull(plan.primary());
        assertTrue(plan.afterCurrentTeamIds().isEmpty());

        service.applyMirror(p, plan);
        assertNull(p.getTeamId());
        assertNull(p.getNumber());
    }

    @Test
    @DisplayName("旧字段兼容：同一球队更新该段背号与守备位置")
    void plan_legacy_sameTeamUpdatesNumberAndPositions() {
        Player p = player(PLAYER_ID, TENANT);
        p.setTeamId(5L);
        p.setNumber("77");
        p.setPositionsList(List.of("P"));
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        e1.setPositionsList(List.of("C"));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, null, false);

        assertEquals(List.of(e1), plan.toUpdate());
        assertEquals("77", e1.getNumber());
        assertEquals(List.of("P"), e1.getPositionsList());
        assertTrue(plan.toCreate().isEmpty());
        assertTrue(plan.toDelete().isEmpty());
    }

    @Test
    @DisplayName("旧字段兼容：值未变化时不产生任何更新")
    void plan_legacy_noChangeProducesNoUpdates() {
        Player p = player(PLAYER_ID, TENANT);
        p.setTeamId(5L);
        p.setNumber("7");
        p.setPositionsList(List.of("C"));
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        e1.setPositionsList(List.of("C"));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, null, false);

        assertTrue(plan.toCreate().isEmpty());
        assertTrue(plan.toUpdate().isEmpty());
        assertTrue(plan.toDelete().isEmpty());
        assertSame(e1, plan.primary());
    }

    @Test
    @DisplayName("提供 teamEntries：软删过的同队段被复活复用")
    void plan_entriesProvided_revivesSoftDeletedEntry() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of());
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));
        PlayerTeam tombstone = entry(20L, 5L, "7", false, 0);
        tombstone.setDeletedAt(LocalDateTime.now());
        tombstone.setDeletedBy(3L);
        when(playerTeamRepository.findFirstByPlayerIdAndTeamIdAndDeletedAtIsNotNullOrderByIdDesc(PLAYER_ID, 5L))
                .thenReturn(Optional.of(tombstone));

        PlayerTeamSyncPlan plan = service.plan(p, List.of(dto(5L, "9", List.of("P"), true)), true);

        assertTrue(plan.toCreate().isEmpty());
        assertEquals(List.of(tombstone), plan.toUpdate());
        assertNull(tombstone.getDeletedAt());
        assertNull(tombstone.getDeletedBy());
        assertEquals("9", tombstone.getNumber());
        assertEquals(Boolean.TRUE, tombstone.getCurrent());
    }

    @Test
    @DisplayName("多个当前段：主注册取排序值最小者")
    void plan_multiCurrent_primaryByLowestSort() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e1 = entry(11L, 5L, "7", true, 1);
        e1.setPositionsList(List.of("P"));
        PlayerTeam e2 = entry(12L, 6L, "8", true, 0);
        e2.setPositionsList(List.of("C"));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e1, e2));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));
        when(teamRepository.findById(6L)).thenReturn(Optional.of(team(6L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, List.of(
                dto(5L, "7", List.of("P"), true),
                dto(6L, "8", List.of("C"), true)), true);

        assertSame(e2, plan.primary());
        service.applyMirror(p, plan);
        assertEquals(6L, p.getTeamId().longValue());
        assertEquals("8", p.getNumber());
        assertEquals(List.of("C"), p.getPositionsList());
    }

    @Test
    @DisplayName("多个当前段：排序相同时主注册取 ID 较小者")
    void plan_multiCurrent_tieByLowestId() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        e1.setPositionsList(List.of("P"));
        PlayerTeam e2 = entry(12L, 6L, "8", true, 0);
        e2.setPositionsList(List.of("C"));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(PLAYER_ID))
                .thenReturn(List.of(e2, e1));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));
        when(teamRepository.findById(6L)).thenReturn(Optional.of(team(6L, TENANT)));

        PlayerTeamSyncPlan plan = service.plan(p, List.of(
                dto(5L, "7", List.of("P"), true),
                dto(6L, "8", List.of("C"), true)), true);

        assertSame(e1, plan.primary());
    }

    @Test
    @DisplayName("落库计划：新增段补球员ID、更新与软删逐行保存")
    void persistPlan_savesCreatesUpdatesAndSoftDeletes() {
        PlayerTeam created = new PlayerTeam();
        created.setTeamId(5L);
        PlayerTeam updated = new PlayerTeam();
        updated.setTeamId(6L);
        PlayerTeam deleted = new PlayerTeam();
        deleted.setTeamId(7L);
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(
                List.of(created), List.of(updated), List.of(deleted), null, Set.of(), Set.of(), List.of());

        service.persistPlan(PLAYER_ID, plan);

        assertEquals(PLAYER_ID, created.getPlayerId().longValue());
        assertNotNull(deleted.getDeletedAt());
        verify(playerTeamRepository).save(created);
        verify(playerTeamRepository).save(updated);
        verify(playerTeamRepository).save(deleted);
    }

    @Test
    @DisplayName("填充输出：按排序整理并映射球队名与守备位置")
    void attachEntries_fillsNamesAndSorts() {
        Player p = player(PLAYER_ID, TENANT);
        PlayerTeam e2 = entry(12L, 6L, "8", false, 1);
        PlayerTeam e1 = entry(11L, 5L, "7", true, 0);
        e1.setPositionsList(List.of("P"));
        when(playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullOrderBySortAscIdAsc(List.of(PLAYER_ID)))
                .thenReturn(List.of(e2, e1));
        when(teamRepository.findAllById(any())).thenReturn(List.of(team(5L, TENANT), team(6L, TENANT)));

        service.attachEntries(p);

        List<PlayerTeamEntryDto> entries = p.getTeamEntries();
        assertEquals(2, entries.size());
        assertEquals(5L, entries.get(0).teamId().longValue());
        assertEquals("球队5", entries.get(0).teamName());
        assertEquals("7", entries.get(0).number());
        assertEquals(List.of("P"), entries.get(0).positions());
        assertEquals(Boolean.TRUE, entries.get(0).current());
        assertEquals(6L, entries.get(1).teamId().longValue());
    }

    @Test
    @DisplayName("填充输出：无经历时给出空列表")
    void attachEntries_noEntriesSetsEmptyList() {
        Player p = player(PLAYER_ID, TENANT);
        when(playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullOrderBySortAscIdAsc(List.of(PLAYER_ID)))
                .thenReturn(List.of());

        service.attachEntries(p);

        assertNotNull(p.getTeamEntries());
        assertTrue(p.getTeamEntries().isEmpty());
    }

    @Test
    @DisplayName("当前球队集合：按经历顺序返回去重集合")
    void currentTeamIds_returnsOrderedSet() {
        when(playerTeamRepository.findCurrentTeamIdsByPlayerId(PLAYER_ID)).thenReturn(List.of(6L, 5L, 6L));

        Set<Long> ids = service.currentTeamIds(PLAYER_ID);

        assertEquals(List.of(6L, 5L), new ArrayList<>(ids));
    }

    @Test
    @DisplayName("是否在队：存在当前段即视为当前球队")
    void isCurrentlyInTeam_delegatesToRepository() {
        when(playerTeamRepository.countCurrentEntry(PLAYER_ID, 5L)).thenReturn(1L);

        assertTrue(service.isCurrentlyInTeam(PLAYER_ID, 5L));
        assertFalse(service.isCurrentlyInTeam(PLAYER_ID, 6L));
    }

    @Test
    @DisplayName("当前段计数：球队集合为空时短路返回 0")
    void countCurrentEntriesInTeams_emptyTeamsReturnsZero() {
        assertEquals(0L, service.countCurrentEntriesInTeams(PLAYER_ID, List.of()));
        verifyNoInteractions(playerTeamRepository);
    }

    @Test
    @DisplayName("当前段计数：非空集合委托仓库查询")
    void countCurrentEntriesInTeams_delegatesToRepository() {
        when(playerTeamRepository.countCurrentEntryInTeams(PLAYER_ID, List.of(5L, 6L))).thenReturn(2L);

        assertEquals(2L, service.countCurrentEntriesInTeams(PLAYER_ID, List.of(5L, 6L)));
    }

    @Test
    @DisplayName("批量当前球队映射：按球员分组且不含无经历球员")
    void currentTeamIdsByPlayerIds_groupsByPlayer() {
        PlayerTeam forPlayer2 = entry(14L, 7L, "9", true, 0);
        forPlayer2.setPlayerId(2L);
        when(playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullAndCurrentTrue(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(
                        entry(11L, 5L, "7", true, 0),
                        entry(12L, 6L, "8", true, 1),
                        forPlayer2));

        Map<Long, Set<Long>> map = service.currentTeamIdsByPlayerIds(List.of(1L, 2L, 3L));

        assertEquals(Set.of(5L, 6L), map.get(1L));
        assertEquals(Set.of(7L), map.get(2L));
        assertFalse(map.containsKey(3L));
    }

    @Test
    @DisplayName("批量当前球队映射：空入参短路返回空映射")
    void currentTeamIdsByPlayerIds_emptyInputReturnsEmptyMap() {
        assertEquals(Map.of(), service.currentTeamIdsByPlayerIds(List.of()));
        verifyNoInteractions(playerTeamRepository);
    }

    private static Player player(Long id, long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        return p;
    }

    private static PlayerTeam entry(Long id, long teamId, String number, boolean current, int sort) {
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

    private static PlayerTeamEntryDto dto(Long teamId, String number, List<String> positions, Boolean current) {
        return new PlayerTeamEntryDto(null, teamId, null, number, positions, current, null);
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }
}
