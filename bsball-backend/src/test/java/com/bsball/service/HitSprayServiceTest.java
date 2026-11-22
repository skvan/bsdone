package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.HitSprayDTO;
import com.bsball.model.entity.HitSpray;
import com.bsball.repository.HitSprayRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HitSprayService：rid 幂等与同打席键去重")
class HitSprayServiceTest {

    @Mock
    private HitSprayRepository hitSprayRepository;

    @InjectMocks
    private HitSprayService hitSprayService;

    @BeforeEach
    void setUpUser() {
        CurrentUserHolder.set(99L, 1L);
    }

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    private static HitSprayDTO dto(Long gameId, String rid) {
        return dto(gameId, rid, 1, "top", 1, 9L, 0, "1");
    }

    private static HitSprayDTO dto(Long gameId, String rid, int inning, String half,
                                   int batterOrder, long playerId, int outsBefore, String runnersBefore) {
        HitSprayDTO dto = new HitSprayDTO();
        dto.setGameId(gameId);
        dto.setPlayerId(playerId);
        dto.setTeamId(1L);
        dto.setInning((short) inning);
        dto.setHalf(half);
        dto.setBatterOrder((short) batterOrder);
        dto.setOutsBefore((short) outsBefore);
        dto.setRunnersBefore(runnersBefore);
        dto.setResultCode("H1");
        dto.setBipCode("bip:g:1b");
        dto.setRbi((short) 0);
        dto.setSprayX(150.0);
        dto.setSprayY(260.0);
        dto.setSprayZone("LF");
        dto.setSprayDepth("深");
        dto.setRid(rid);
        return dto;
    }

    private static HitSpray existing(Long gameId, String rid) {
        HitSpray entity = new HitSpray();
        entity.setGameId(gameId);
        entity.setRid(rid);
        return entity;
    }

    private static HitSpray row(long id, Long gameId, String rid) {
        return row(id, gameId, rid, 1, "top", 1, 9L, 0, "1");
    }

    private static HitSpray row(long id, Long gameId, String rid, int inning, String half,
                                int batterOrder, long playerId, int outsBefore, String runnersBefore) {
        HitSpray entity = new HitSpray();
        entity.setId(id);
        entity.setGameId(gameId);
        entity.setPlayerId(playerId);
        entity.setTeamId(1L);
        entity.setInning((short) inning);
        entity.setHalf(half);
        entity.setBatterOrder((short) batterOrder);
        entity.setOutsBefore((short) outsBefore);
        entity.setRunnersBefore(runnersBefore);
        entity.setResultCode("H1");
        entity.setRid(rid);
        return entity;
    }

    @Test
    @DisplayName("已存在同 gameId+rid：不再插入")
    void skipsExistingRid() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection()))
                .thenReturn(List.of(existing(10L, "rid-1")));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-1")));

        assertTrue(saved.isEmpty());
        verify(hitSprayRepository, never()).saveAll(anyList());
        verify(hitSprayRepository, never()).findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(anyLong());
    }

    @Test
    @DisplayName("同一批内重复 rid：只保留一条")
    void deduplicatesBatchRids() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-1"), dto(10L, "rid-1")));

        assertEquals(1, saved.size());
        assertEquals("rid-1", saved.get(0).getRid());
        ArgumentCaptor<List<HitSpray>> captor = ArgumentCaptor.forClass(List.class);
        verify(hitSprayRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    @DisplayName("不同场次同 rid：各自插入，不跨场误判")
    void keepsSameRidAcrossGames() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndRidIn(eq(11L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(11L)).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-x"), dto(11L, "rid-x")));

        assertEquals(2, saved.size());
        verify(hitSprayRepository).findByGameIdAndRidIn(eq(10L), anyCollection());
        verify(hitSprayRepository).findByGameIdAndRidIn(eq(11L), anyCollection());
    }

    @Test
    @DisplayName("历史无 rid：不同打席按原语义插入，且不查询 rid 幂等键")
    void insertsLegacyWithoutRid() {
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(
                dto(10L, null),
                dto(10L, "  ", 1, "top", 2, 9L, 0, "1")));

        assertEquals(2, saved.size());
        assertNull(saved.get(0).getRid());
        assertNull(saved.get(1).getRid());
        verify(hitSprayRepository, never()).findByGameIdAndRidIn(anyLong(), anyCollection());
    }

    @Test
    @DisplayName("历史同键无 rid：同一批只落最后一条")
    void deduplicatesLegacySameAtBatWithinBatch() {
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        HitSprayDTO first = dto(10L, null);
        first.setResultCode("H1");
        HitSprayDTO last = dto(10L, null);
        last.setResultCode("E");

        List<HitSpray> saved = hitSprayService.saveAll(List.of(first, last));

        assertEquals(1, saved.size());
        assertEquals("E", saved.get(0).getResultCode());
        verify(hitSprayRepository, never()).findByGameIdAndRidIn(anyLong(), anyCollection());
    }

    @Test
    @DisplayName("save 单条：已存在 rid 返回 null，不插入")
    void saveReturnsNullForDuplicate() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection()))
                .thenReturn(List.of(existing(10L, "rid-1")));

        assertNull(hitSprayService.save(dto(10L, "rid-1")));
        verify(hitSprayRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("同打席新 rid：软删旧行并插入新行，不原地覆盖旧 rid")
    void replacesSameAtBatWithNewRid() {
        HitSpray old = row(501L, 10L, "rid-old");
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(old));
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-new")));

        assertEquals(1, saved.size());
        assertEquals("rid-new", saved.get(0).getRid());
        assertNotNull(old.getDeletedAt());
        assertEquals(99L, old.getDeletedBy());
        assertTrue(old.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

        ArgumentCaptor<List<HitSpray>> captor = ArgumentCaptor.forClass(List.class);
        verify(hitSprayRepository, times(2)).saveAll(captor.capture());
        List<List<HitSpray>> calls = captor.getAllValues();
        assertEquals(List.of(old), calls.get(0));
        assertEquals(1, calls.get(1).size());
        assertEquals("rid-new", calls.get(1).get(0).getRid());
    }

    @Test
    @DisplayName("已软删行的同 rid 重试：仍被识别并跳过，不复活")
    void skipsRetryForSoftDeletedRid() {
        HitSpray softDeleted = existing(10L, "rid-1");
        softDeleted.setDeletedAt(LocalDateTime.now().minusHours(1));
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection()))
                .thenReturn(List.of(softDeleted));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-1")));

        assertTrue(saved.isEmpty());
        verify(hitSprayRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("同一批次同打席键：只落最后一条")
    void dedupesSameAtBatWithinBatchKeepsLast() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        HitSprayDTO first = dto(10L, "rid-a");
        first.setResultCode("H1");
        HitSprayDTO last = dto(10L, "rid-b");
        last.setResultCode("E");

        List<HitSpray> saved = hitSprayService.saveAll(List.of(first, last));

        assertEquals(1, saved.size());
        assertEquals("rid-b", saved.get(0).getRid());
        assertEquals("E", saved.get(0).getResultCode());
    }

    @Test
    @DisplayName("runnersBefore 的 null 与空串视为同一打席键")
    void normalizesNullAndEmptyRunners() {
        HitSpray old = row(502L, 10L, "rid-old", 1, "top", 1, 9L, 0, null);
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(old));
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-new", 1, "top", 1, 9L, 0, "")));

        assertEquals(1, saved.size());
        assertNotNull(old.getDeletedAt());
    }

    @Test
    @DisplayName("同球员同局但 outs/runners 不同：属于合法双打席，两条都插入")
    void keepsLegitimateTwinPlateAppearances() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(
                dto(10L, "rid-a", 1, "top", 2, 112L, 0, "1,2"),
                dto(10L, "rid-b", 1, "top", 2, 112L, 2, "2,3")));

        assertEquals(2, saved.size());
        verify(hitSprayRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("打席键字段缺失：不做键去重，直接插入")
    void skipsAtBatDedupeWhenKeyIncomplete() {
        HitSprayDTO dto = dto(10L, "rid-1");
        dto.setBatterOrder(null);
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto));

        assertEquals(1, saved.size());
        verify(hitSprayRepository, never()).findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(anyLong());
    }

    @Test
    @DisplayName("展示读取：三个方法都走 deleted_at IS NULL 仓储查询")
    void readQueriesFilterDeletedAt() {
        when(hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(5L)).thenReturn(List.of());
        when(hitSprayRepository.findByPlayerIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(7L, 5L))
                .thenReturn(List.of());
        when(hitSprayRepository.findByTeamIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L, 5L))
                .thenReturn(List.of());

        hitSprayService.getByGameId(5L);
        hitSprayService.getByPlayerAndGame(7L, 5L);
        hitSprayService.getByTeamAndGame(1L, 5L);

        verify(hitSprayRepository).findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(5L);
        verify(hitSprayRepository).findByPlayerIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(7L, 5L);
        verify(hitSprayRepository).findByTeamIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L, 5L);
    }
}
