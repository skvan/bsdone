package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.GameRevisionAuditDTO;
import com.bsball.model.entity.ErDecisionAdjustment;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.ErDecisionAdjustmentRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.PitcherRunOverrideRepository;
import com.bsball.repository.PlayerRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameRevisionAuditServiceTest {
    @Mock
    private ErDecisionAdjustmentRepository erDecisionAdjustmentRepository;
    @Mock
    private PitcherRunOverrideRepository pitcherRunOverrideRepository;
    @Mock
    private GameFieldingAdjustmentRepository fieldingAdjustmentRepository;
    @Mock
    private GamePitcherTransferRepository pitcherTransferRepository;
    @Mock
    private PlayerRepository playerRepository;

    private GameRevisionAuditService service;
    private Game game;

    @BeforeEach
    void setUp() {
        service = new GameRevisionAuditService(
                erDecisionAdjustmentRepository, pitcherRunOverrideRepository,
                fieldingAdjustmentRepository, pitcherTransferRepository,
                playerRepository);
        game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
    }

    @Test
    @DisplayName("修订记录聚合四类审计并按操作时间倒序")
    void listAggregatesFourAuditSourcesSortedByTime() {
        ErDecisionAdjustment er = new ErDecisionAdjustment();
        er.setId(1L);
        er.setRunnerId(55L);
        er.setAction("REASSIGN");
        er.setTargetPitcherId(701L);
        er.setStatus("ACTIVE");
        er.setReason("现场更正");
        er.setCreatedBy(9L);
        er.setCreatedAt(LocalDateTime.of(2026, 11, 5, 10, 0));

        PitcherRunOverride override = new PitcherRunOverride();
        override.setId(2L);
        override.setPlayerId(700L);
        override.setRunsAllowed(5);
        override.setEarnedRuns(3);
        override.setStatus("APPROVED");
        override.setNote("投手合计更正");
        override.setUpdatedBy(10L);
        override.setUpdatedAt(LocalDateTime.of(2026, 11, 5, 11, 0));

        GameFieldingAdjustment fielding = new GameFieldingAdjustment();
        fielding.setId(3L);
        fielding.setPlayerId(23L);
        fielding.setPosition("CF");
        fielding.setAssistCount(1);
        fielding.setInning(5);
        fielding.setTopBottom("bottom");
        fielding.setSourceSequence(167);
        fielding.setStatus("REVOKED");
        fielding.setNote("补记");
        fielding.setRevokedBy(11L);
        fielding.setRevokedAt(LocalDateTime.of(2026, 11, 5, 12, 0));

        GamePitcherTransfer transfer = new GamePitcherTransfer();
        transfer.setId(4L);
        transfer.setSourcePlayerId(700L);
        transfer.setTargetPlayerId(701L);
        transfer.setStatus("DELETED");
        transfer.setNote("转记");
        transfer.setSourceDeletedBy(12L);
        transfer.setSourceDeletedAt(LocalDateTime.of(2026, 11, 5, 13, 0));

        when(erDecisionAdjustmentRepository.findByTenantIdAndGameIdOrderByIdDesc(1L, 20L))
                .thenReturn(List.of(er));
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdOrderByIdDesc(1L, 20L))
                .thenReturn(List.of(override));
        when(fieldingAdjustmentRepository
                .findByTenantIdAndGameIdOrderByInningAscTopBottomAscSourceSequenceAscIdAsc(1L, 20L))
                .thenReturn(List.of(fielding));
        when(pitcherTransferRepository.findByTenantIdAndGameIdOrderByIdDesc(1L, 20L))
                .thenReturn(List.of(transfer));
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(55L, "跑者甲"), player(700L, "投手乙"), player(701L, "投手丙"),
                player(23L, "中外野")));

        List<GameRevisionAuditDTO> rows = service.list(game);

        assertEquals(4, rows.size());
        assertEquals("PITCHER_TRANSFER", rows.get(0).auditType());
        assertEquals("DELETE_SOURCE", rows.get(0).action());
        assertEquals("DELETED", rows.get(0).status());
        assertTrue(rows.get(0).summary().contains("投手乙"));
        assertTrue(rows.get(0).summary().contains("投手丙"));
        assertEquals(12L, rows.get(0).operatorId());
        assertEquals("FIELDING_ADJUSTMENT", rows.get(1).auditType());
        assertEquals("REVOKE", rows.get(1).action());
        assertTrue(rows.get(1).summary().contains("5下 #167"));
        assertEquals("PITCHER_RUN_OVERRIDE", rows.get(2).auditType());
        assertTrue(rows.get(2).summary().contains("R 5 / ER 3"));
        assertEquals("ER_DECISION", rows.get(3).auditType());
        assertTrue(rows.get(3).summary().contains("跑者甲"));
        assertTrue(rows.get(3).summary().contains("投手丙"));
        assertEquals("现场更正", rows.get(3).reason());
    }

    @Test
    @DisplayName("修订记录为空时返回空列表")
    void listReturnsEmptyWhenNoAuditRows() {
        when(erDecisionAdjustmentRepository.findByTenantIdAndGameIdOrderByIdDesc(1L, 20L))
                .thenReturn(List.of());
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdOrderByIdDesc(1L, 20L))
                .thenReturn(List.of());
        when(fieldingAdjustmentRepository
                .findByTenantIdAndGameIdOrderByInningAscTopBottomAscSourceSequenceAscIdAsc(1L, 20L))
                .thenReturn(List.of());
        when(pitcherTransferRepository.findByTenantIdAndGameIdOrderByIdDesc(1L, 20L))
                .thenReturn(List.of());

        assertTrue(service.list(game).isEmpty());
    }

    private static Player player(long id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        return player;
    }
}
