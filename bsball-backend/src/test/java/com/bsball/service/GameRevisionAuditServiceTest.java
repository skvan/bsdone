package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.GameRevisionAuditDTO;
import com.bsball.model.dto.GameStatAdjustmentDTO;
import com.bsball.model.dto.StatBaseCompareDTO;
import com.bsball.model.entity.ErDecisionAdjustment;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.GameStatAdjustment;
import com.bsball.model.entity.GameStatBase;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.ErDecisionAdjustmentRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameStatAdjustmentRepository;
import com.bsball.repository.GameStatBaseRepository;
import com.bsball.repository.PitcherRunOverrideRepository;
import com.bsball.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private GameFieldingPosStatRepository gameFieldingPosStatRepository;
    @Mock
    private GameStatBaseRepository gameStatBaseRepository;
    @Mock
    private GameStatAdjustmentRepository gameStatAdjustmentRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private GameRevisionAuditService service;
    private Game game;

    @BeforeEach
    void setUp() {
        service = new GameRevisionAuditService(
                erDecisionAdjustmentRepository, pitcherRunOverrideRepository,
                fieldingAdjustmentRepository, pitcherTransferRepository,
                playerRepository, gamePlayerStatRepository,
                gameFieldingPosStatRepository, gameStatBaseRepository,
                gameStatAdjustmentRepository, transactionManager, objectMapper);
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
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

    @Test
    @DisplayName("基座快照首次写入，已有基座不覆盖")
    void captureBaseWritesOnceAndKeepsFirstSnapshot() {
        game.setStatus("final");
        GamePlayerStat stat = new GamePlayerStat();
        stat.setId(500L);
        stat.setTenantId(1L);
        stat.setGameId(20L);
        stat.setTeamId(1L);
        stat.setPlayerId(700L);
        stat.setIsPitcher(1);
        stat.setPitchPa(11);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(stat));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        List<GameStatBase> saved = new ArrayList<>();
        when(gameStatBaseRepository
                .findByTenantIdAndGameIdOrderByPlayerIdAscIsPitcherAsc(1L, 20L))
                .thenAnswer(invocation -> new ArrayList<>(saved));
        when(gameStatBaseRepository.save(any(GameStatBase.class))).thenAnswer(invocation -> {
            GameStatBase row = invocation.getArgument(0);
            row.setId(900L);
            saved.add(row);
            return row;
        });

        service.captureBase(game, 9L, "RESULT");
        service.captureBase(game, 9L, "RESULT");

        verify(gameStatBaseRepository).save(any(GameStatBase.class));
        assertEquals(1, saved.size());
        assertEquals("RESULT", saved.get(0).getSource());
        assertTrue(saved.get(0).getStatJson().contains("\"pitchPa\":11"));
    }

    @Test
    @DisplayName("非 final 保存不写基座")
    void captureBaseSkipsNonFinalGame() {
        game.setStatus("live");

        service.captureBase(game, 9L, "RESULT");

        verifyNoInteractions(gameStatBaseRepository, gamePlayerStatRepository,
                gameFieldingPosStatRepository);
    }

    @Test
    @DisplayName("无基座时对比返回 legacy=true")
    void compareBaseReturnsLegacyWhenNoSnapshot() {
        when(gameStatBaseRepository
                .findByTenantIdAndGameIdOrderByPlayerIdAscIsPitcherAsc(1L, 20L))
                .thenReturn(List.of());

        StatBaseCompareDTO result = service.compareBase(game);

        assertTrue(result.legacy());
        assertTrue(result.rows().isEmpty());
    }

    @Test
    @DisplayName("基座对比返回差异字段与值")
    void compareBaseReturnsChangedFields() throws Exception {
        GamePlayerStat baseStat = new GamePlayerStat();
        baseStat.setTeamId(1L);
        baseStat.setPlayerId(700L);
        baseStat.setIsPitcher(1);
        baseStat.setPitchPa(11);
        GameStatBase base = new GameStatBase();
        base.setPlayerId(700L);
        base.setTeamId(1L);
        base.setIsPitcher(1);
        base.setSource("RESULT");
        base.setCapturedAt(LocalDateTime.of(2026, 11, 5, 10, 0));
        base.setStatJson(objectMapper.writeValueAsString(baseStat));
        when(gameStatBaseRepository
                .findByTenantIdAndGameIdOrderByPlayerIdAscIsPitcherAsc(1L, 20L))
                .thenReturn(List.of(base));

        GamePlayerStat current = new GamePlayerStat();
        current.setTenantId(1L);
        current.setGameId(20L);
        current.setTeamId(1L);
        current.setPlayerId(700L);
        current.setIsPitcher(1);
        current.setPitchPa(13);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(current));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(700L, "投手甲")));

        StatBaseCompareDTO result = service.compareBase(game);

        assertFalse(result.legacy());
        assertEquals(1, result.rows().size());
        StatBaseCompareDTO.PlayerDiff row = result.rows().get(0);
        assertEquals("投手甲", row.playerName());
        assertEquals(1, row.fields().size());
        assertEquals("pitchPa", row.fields().get(0).key());
        assertEquals("PA(投手)", row.fields().get(0).label());
        assertEquals(11d, row.fields().get(0).baseValue());
        assertEquals(13d, row.fields().get(0).effectiveValue());
    }

    @Test
    @DisplayName("统一修订台账写入字段级变更")
    @SuppressWarnings("unchecked")
    void recordStatChangesWritesFieldRows() {
        service.recordStatChanges(game, List.of(
                new GameRevisionAuditService.StatChange(
                        1L, 700L, 1, 500L, "np", "NP", 27, 30),
                new GameRevisionAuditService.StatChange(
                        1L, 700L, 1, 500L, "pitchPa", "PA(投手)", 11, 13)
        ), 9L);

        ArgumentCaptor<List<GameStatAdjustment>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(gameStatAdjustmentRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        GameStatAdjustment first = captor.getValue().get(0);
        assertEquals("PLAYER_STAT", first.getScope());
        assertEquals("UPDATE_FIELD", first.getAction());
        assertEquals("RESULT_SAVE", first.getSourceType());
        assertEquals("NP", first.getFieldLabel());
        assertEquals("27", first.getBeforeValue());
        assertEquals("30", first.getAfterValue());
        assertEquals(9L, first.getOperatorId());
        assertEquals("ACTIVE", first.getStatus());
    }

    @Test
    @DisplayName("统一台账写入失败不抛出，不阻断结果保存")
    void recordStatChangesSwallowsWriteFailure() {
        when(gameStatAdjustmentRepository.saveAll(any()))
                .thenThrow(new RuntimeException("ledger unavailable"));

        service.recordStatChanges(game, List.of(
                new GameRevisionAuditService.StatChange(
                        1L, 700L, 1, 500L, "np", "NP", 27, 30)
        ), 9L);

        verify(gameStatAdjustmentRepository).saveAll(any());
    }

    @Test
    @DisplayName("统一修订台账按时间倒序返回并带球员名")
    void listAdjustmentsMapsRows() {
        GameStatAdjustment row = new GameStatAdjustment();
        row.setId(1L);
        row.setPlayerId(700L);
        row.setFieldKey("np");
        row.setFieldLabel("NP");
        row.setBeforeValue("27");
        row.setAfterValue("30");
        row.setOperatedAt(LocalDateTime.of(2026, 11, 5, 10, 0));
        when(gameStatAdjustmentRepository
                .findByTenantIdAndGameIdOrderByOperatedAtDescIdDesc(1L, 20L))
                .thenReturn(List.of(row));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(700L, "投手甲")));

        List<GameStatAdjustmentDTO> result = service.listAdjustments(game);

        assertEquals(1, result.size());
        assertEquals("投手甲", result.get(0).playerName());
        assertEquals("np", result.get(0).fieldKey());
        assertEquals("27", result.get(0).beforeValue());
        assertEquals("30", result.get(0).afterValue());
    }

    private static Player player(long id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        return player;
    }
}
