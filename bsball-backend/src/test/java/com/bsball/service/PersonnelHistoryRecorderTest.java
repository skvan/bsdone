package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.entity.HistoryRecord;
import com.bsball.model.entity.Player;
import com.bsball.repository.HistoryRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonnelHistoryRecorder：球员球队流转与档案事件")
class PersonnelHistoryRecorderTest {

    @Mock
    private HistoryRecordRepository historyRecordRepository;

    private PersonnelHistoryRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new PersonnelHistoryRecorder(historyRecordRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("流转：空集合到单队 → 记 join 并返回记录ID")
    void transitions_fromEmptyToSingle_recordsJoin() {
        List<HistoryRecord> saved = captureSaves();
        Player p = player(1L, 9L);

        Long joinRecordId = recorder.recordPlayerTeamTransitions(p, Set.of(), Set.of(5L));

        assertEquals(1, saved.size());
        HistoryRecord r = saved.get(0);
        assertEquals("join", r.getType());
        assertEquals("player", r.getTargetType());
        assertEquals(1L, r.getTargetId().longValue());
        assertEquals(9L, r.getTenantId().longValue());
        assertEquals("team", r.getRelatedObjectType());
        assertEquals(5L, r.getRelatedObjectId().longValue());
        assertNotNull(joinRecordId);
        assertEquals(saved.get(0).getId(), joinRecordId);
    }

    @Test
    @DisplayName("流转：单队到空集合 → 记 leave 且不返回记录ID")
    void transitions_fromSingleToEmpty_recordsLeave() {
        List<HistoryRecord> saved = captureSaves();
        Player p = player(1L, 9L);

        Long joinRecordId = recorder.recordPlayerTeamTransitions(p, Set.of(5L), Set.of());

        assertEquals(1, saved.size());
        assertEquals("leave", saved.get(0).getType());
        assertEquals(5L, saved.get(0).getRelatedObjectId().longValue());
        assertNull(joinRecordId);
    }

    @Test
    @DisplayName("流转：单队到另一单队 → 记 transfer 且载荷含前后球队")
    void transitions_singleToSingle_recordsTransferWithPayload() throws Exception {
        List<HistoryRecord> saved = captureSaves();
        Player p = player(1L, 9L);

        Long joinRecordId = recorder.recordPlayerTeamTransitions(p, Set.of(5L), Set.of(6L));

        assertEquals(1, saved.size());
        HistoryRecord r = saved.get(0);
        assertEquals("transfer", r.getType());
        assertEquals(6L, r.getRelatedObjectId().longValue());
        Map<?, ?> payload = new ObjectMapper().readValue(r.getChangePayloadJson(), Map.class);
        assertEquals(5, ((Number) payload.get("fromTeamId")).intValue());
        assertEquals(6, ((Number) payload.get("toTeamId")).intValue());
        assertEquals(saved.get(0).getId(), joinRecordId);
    }

    @Test
    @DisplayName("流转：新增当前球队 → 记 join")
    void transitions_addedCurrentTeam_recordsJoin() {
        List<HistoryRecord> saved = captureSaves();
        Player p = player(1L, 9L);

        recorder.recordPlayerTeamTransitions(p, Set.of(5L), Set.of(5L, 6L));

        assertEquals(1, saved.size());
        assertEquals("join", saved.get(0).getType());
        assertEquals(6L, saved.get(0).getRelatedObjectId().longValue());
    }

    @Test
    @DisplayName("流转：移除当前球队 → 记 leave")
    void transitions_removedCurrentTeam_recordsLeave() {
        List<HistoryRecord> saved = captureSaves();
        Player p = player(1L, 9L);

        recorder.recordPlayerTeamTransitions(p, Set.of(5L, 6L), Set.of(5L));

        assertEquals(1, saved.size());
        assertEquals("leave", saved.get(0).getType());
        assertEquals(6L, saved.get(0).getRelatedObjectId().longValue());
    }

    @Test
    @DisplayName("流转：单队换多队 → 移除记 leave、新增逐个记 join")
    void transitions_multipleChanges_recordsPerTeam() {
        List<HistoryRecord> saved = captureSaves();
        Player p = player(1L, 9L);

        Long joinRecordId = recorder.recordPlayerTeamTransitions(p, Set.of(5L), Set.of(6L, 7L));

        assertEquals(3, saved.size());
        assertEquals(List.of("join", "join", "leave"), saved.stream().map(HistoryRecord::getType).toList());
        Set<Long> related = saved.stream().map(HistoryRecord::getRelatedObjectId).collect(Collectors.toSet());
        assertEquals(Set.of(5L, 6L, 7L), related);
        assertNotNull(joinRecordId);
        assertEquals(saved.get(1).getId(), joinRecordId);
    }

    @Test
    @DisplayName("流转：集合未变化或入参为空 → 不写记录并返回 null")
    void transitions_noChangeOrEmptyInput_noRecords() {
        Player p = player(1L, 9L);

        assertNull(recorder.recordPlayerTeamTransitions(p, Set.of(5L), Set.of(5L)));
        assertNull(recorder.recordPlayerTeamTransitions(p, null, null));
        verifyNoInteractions(historyRecordRepository);
    }

    @Test
    @DisplayName("流转：球员为空或缺租户 → 不写记录")
    void transitions_missingPlayerOrTenant_noRecords() {
        Player noTenant = new Player();
        noTenant.setId(1L);

        assertNull(recorder.recordPlayerTeamTransitions(null, Set.of(), Set.of(5L)));
        assertNull(recorder.recordPlayerTeamTransitions(noTenant, Set.of(), Set.of(5L)));
        verifyNoInteractions(historyRecordRepository);
    }

    @Test
    @DisplayName("档案更新：仅镜像球队变化不再产生团队事件")
    void afterPlayerUpdate_teamMirrorChangeOnly_writesNothing() {
        Player before = player(1L, 9L);
        before.setTeamId(5L);
        before.setStatus("active");
        before.setName("张三");
        Player after = player(1L, 9L);
        after.setTeamId(6L);
        after.setStatus("active");
        after.setName("张三");

        recorder.afterPlayerUpdate(before, after);

        verifyNoInteractions(historyRecordRepository);
    }

    @Test
    @DisplayName("档案更新：退役与档案变更事件保留")
    void afterPlayerUpdate_retireAndProfileStillRecorded() {
        List<HistoryRecord> saved = captureSaves();
        Player before = player(1L, 9L);
        before.setTeamId(5L);
        before.setStatus("active");
        before.setName("张三");
        Player after = player(1L, 9L);
        after.setTeamId(5L);
        after.setStatus("retired");
        after.setName("李四");

        recorder.afterPlayerUpdate(before, after);

        assertEquals(2, saved.size());
        assertEquals("retire", saved.get(0).getType());
        assertEquals("profile_update", saved.get(1).getType());
    }

    private List<HistoryRecord> captureSaves() {
        List<HistoryRecord> captured = new ArrayList<>();
        when(historyRecordRepository.save(any(HistoryRecord.class))).thenAnswer(invocation -> {
            HistoryRecord r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(100L + captured.size());
            }
            captured.add(r);
            return r;
        });
        return captured;
    }

    private static Player player(Long id, Long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        return p;
    }
}
