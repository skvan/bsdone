package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.HitSprayDTO;
import com.bsball.model.entity.HitSpray;
import com.bsball.repository.HitSprayRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HitSprayService：rid 幂等去重")
class HitSprayServiceTest {

    @Mock
    private HitSprayRepository hitSprayRepository;

    @InjectMocks
    private HitSprayService hitSprayService;

    private static HitSprayDTO dto(Long gameId, String rid) {
        HitSprayDTO dto = new HitSprayDTO();
        dto.setGameId(gameId);
        dto.setPlayerId(9L);
        dto.setTeamId(1L);
        dto.setInning((short) 1);
        dto.setHalf("top");
        dto.setBatterOrder((short) 1);
        dto.setOutsBefore((short) 0);
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

    @Test
    @DisplayName("已存在同 gameId+rid：不再插入")
    void skipsExistingRid() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection()))
                .thenReturn(List.of(existing(10L, "rid-1")));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-1")));

        assertTrue(saved.isEmpty());
        verify(hitSprayRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("同一批内重复 rid：只保留一条")
    void deduplicatesBatchRids() {
        when(hitSprayRepository.findByGameIdAndRidIn(eq(10L), anyCollection())).thenReturn(List.of());
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
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, "rid-x"), dto(11L, "rid-x")));

        assertEquals(2, saved.size());
        verify(hitSprayRepository).findByGameIdAndRidIn(eq(10L), anyCollection());
        verify(hitSprayRepository).findByGameIdAndRidIn(eq(11L), anyCollection());
    }

    @Test
    @DisplayName("历史无 rid：保持原语义插入，且不查询幂等键")
    void insertsLegacyWithoutRid() {
        when(hitSprayRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<HitSpray> saved = hitSprayService.saveAll(List.of(dto(10L, null), dto(10L, "  ")));

        assertEquals(2, saved.size());
        assertNull(saved.get(0).getRid());
        assertNull(saved.get(1).getRid());
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
}
