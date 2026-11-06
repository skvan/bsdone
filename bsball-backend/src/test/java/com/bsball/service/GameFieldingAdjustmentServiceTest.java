package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.FieldingAdjustmentCandidateDTO;
import com.bsball.model.dto.FieldingAdjustmentCreateRequest;
import com.bsball.model.dto.FieldingAdjustmentListDTO;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Player;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameFieldingAdjustmentServiceTest {
    @Mock
    private GameFieldingAdjustmentRepository adjustmentRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private GameFieldingPosStatRepository gameFieldingPosStatRepository;
    @Mock
    private PlayerRepository playerRepository;

    private GameFieldingAdjustmentService service;
    private Game game;
    private GamePlayerStat fielderStat;
    private GameFieldingPosStat positionRow;

    @BeforeEach
    void setUp() {
        service = new GameFieldingAdjustmentService(
                adjustmentRepository, gamePlayerStatRepository,
                gameFieldingPosStatRepository, playerRepository);
        game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        game.setHomeTeamId(100L);
        game.setAwayTeamId(200L);
        game.setStatus("final");

        fielderStat = new GamePlayerStat();
        fielderStat.setId(300L);
        fielderStat.setTenantId(1L);
        fielderStat.setGameId(20L);
        fielderStat.setTeamId(200L);
        fielderStat.setPlayerId(23L);
        fielderStat.setIsPitcher(0);
        fielderStat.setPosition("CF");
        fielderStat.setPo(1);
        fielderStat.setA(2);
        fielderStat.setE(1);
        fielderStat.setTc(4);

        positionRow = new GameFieldingPosStat();
        positionRow.setId(400L);
        positionRow.setTenantId(1L);
        positionRow.setGameId(20L);
        positionRow.setTeamId(200L);
        positionRow.setPlayerId(23L);
        positionRow.setPosition("CF");
        positionRow.setPo(1);
        positionRow.setA(2);
        positionRow.setE(1);
        positionRow.setOuts(27);

        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(gameFieldingPosStatRepository.save(any(GameFieldingPosStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(adjustmentRepository.save(any(GameFieldingAdjustment.class)))
                .thenAnswer(invocation -> {
                    GameFieldingAdjustment row = invocation.getArgument(0);
                    if (row.getId() == null) row.setId(77L);
                    return row;
                });
    }

    @Test
    @DisplayName("读取补记时按球员汇总已生效 A，供 /stats 判断统计是否已含补记")
    void approvedAssistTotalsByGameAggregatesPerPlayer() {
        GameFieldingAdjustment first = adjustment("APPROVED");
        first.setPlayerId(23L);
        GameFieldingAdjustment second = adjustment("APPROVED");
        second.setPlayerId(23L);
        GameFieldingAdjustment third = adjustment("APPROVED");
        third.setPlayerId(24L);
        when(adjustmentRepository.findByGameIdAndStatus(20L, "APPROVED"))
                .thenReturn(List.of(first, second, third));

        java.util.Map<Long, Integer> totals = service.approvedAssistTotalsByGame(20L);

        assertEquals(2, totals.get(23L));
        assertEquals(1, totals.get(24L));
        assertNull(totals.get(25L));
    }

    @Test
    @DisplayName("候选补记：只返回失误 Play，并只带守备方球员")
    void listCandidatesReturnsErrorPlaysForDefensiveSide() {
        game.setLiveSnapshotJson("""
                {"playEventsComplete":true,"playEvents":[
                  {"id":"play-1","inning":5,"half":"bottom","sequence":167,
                   "outsBefore":1,"basesBefore":{"1":true,"2":true,"3":false},
                   "resultCode":"E","resultText":"失误"},
                  {"id":"play-2","inning":6,"half":"top","sequence":200,
                   "outsBefore":0,"basesBefore":{"1":false,"2":false,"3":false},
                   "resultCode":"1B","resultText":"一垒安打"},
                  {"id":"play-3","inning":7,"half":"top","sequence":220,
                   "outsBefore":2,"basesBefore":{"1":false,"2":false,"3":true},
                   "resultCode":"ADVANCE_FE","resultText":"趁传进垒"}
                ]}
                """);
        GamePlayerStat homeStat = new GamePlayerStat();
        homeStat.setTenantId(1L);
        homeStat.setGameId(20L);
        homeStat.setTeamId(100L);
        homeStat.setPlayerId(31L);
        homeStat.setPosition("LF");
        homeStat.setNumber("31");
        homeStat.setListOrder(1);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat, homeStat));
        when(playerRepository.findAllById(any()))
                .thenReturn(List.of(player(23L, "客中外"), player(31L, "主左外")));

        List<FieldingAdjustmentCandidateDTO> result = service.listCandidates(game);

        assertEquals(2, result.size());
        FieldingAdjustmentCandidateDTO bottom = result.get(0);
        assertEquals("play-1", bottom.sourceEventId());
        assertEquals(200L, bottom.teamId());
        assertEquals("5下 #167", bottom.inningLabel());
        assertEquals("一、二垒有人", bottom.baseLabel());
        assertEquals(List.of(23L), bottom.players().stream()
                .map(FieldingAdjustmentCandidateDTO.PlayerOption::id).toList());
        FieldingAdjustmentCandidateDTO top = result.get(1);
        assertEquals("play-3", top.sourceEventId());
        assertEquals(100L, top.teamId());
        assertEquals("7上 #220", top.inningLabel());
        assertEquals("三垒有人", top.baseLabel());
        assertEquals(List.of(31L), top.players().stream()
                .map(FieldingAdjustmentCandidateDTO.PlayerOption::id).toList());
    }

    @Test
    @DisplayName("候选补记：无快照或没有失误 Play 时返回空列表")
    void listCandidatesReturnsEmptyWithoutSnapshotOrErrorPlay() {
        assertEquals(List.of(), service.listCandidates(game));
        game.setLiveSnapshotJson(snapshot("1B", "一垒安打"));
        assertEquals(List.of(), service.listCandidates(game));
    }

    @Test
    @DisplayName("删除统计行守卫：有已生效补记的球员返回 true")
    void hasApprovedPlayerAdjustmentDelegates() {
        when(adjustmentRepository.countByTenantIdAndGameIdAndPlayerIdAndStatus(
                1L, 20L, 23L, "APPROVED")).thenReturn(1L);

        assertTrue(service.hasApprovedPlayerAdjustment(1L, 20L, 23L));
        assertFalse(service.hasApprovedPlayerAdjustment(0L, 20L, 23L));
    }

    @Test
    @DisplayName("补记助杀：从源 Play 推导守备方并同步球员 A/TC 与位置 A")
    void createCreditsPlayerAndPosition() {
        game.setLiveSnapshotJson(snapshot("E", "5下 失误：CF 主中外 (#23) 记 E，二垒跑者 #31 得分"));
        when(adjustmentRepository
                .findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
                        1L, 20L, "play-1", 23L))
                .thenReturn(Optional.empty());
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(23L, "主中外")));

        FieldingAdjustmentCreateRequest request = request("play-1", 23L, "CF");
        FieldingAdjustmentListDTO result = service.create(game, 9L, request);

        assertEquals("APPROVED", result.status());
        assertEquals(200L, result.teamId(), "下半局由客队守备");
        assertEquals(5, result.inning());
        assertEquals("bottom", result.half());
        assertEquals(167, result.sequence());
        assertEquals(23L, result.playerId());
        assertEquals("主中外", result.playerName());
        assertEquals("CF", result.position());
        assertEquals(3, fielderStat.getA());
        assertEquals(5, fielderStat.getTc());
        assertEquals(3, positionRow.getA());
        assertTrue(result.sourceValid());
        verify(adjustmentRepository).save(any(GameFieldingAdjustment.class));
        verify(gamePlayerStatRepository, org.mockito.Mockito.atLeastOnce()).save(fielderStat);
        verify(gameFieldingPosStatRepository, org.mockito.Mockito.atLeastOnce()).save(positionRow);
    }

    @Test
    @DisplayName("同一 Play 同一守备员不能重复补记（换守位也不行）")
    void createRejectsDuplicateApprovedAdjustment() {
        game.setLiveSnapshotJson(snapshot("E", "失误：CF 记 E"));
        when(adjustmentRepository
                .existsByTenantIdAndGameIdAndSourceEventIdAndPlayerIdAndStatus(
                        1L, 20L, "play-1", 23L, "APPROVED"))
                .thenReturn(true);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(game, 9L, request("play-1", 23L, "LF")));

        assertEquals(409, error.getCode());
        assertEquals(2, fielderStat.getA());
        verify(adjustmentRepository, never()).save(any(GameFieldingAdjustment.class));
    }

    @Test
    @DisplayName("同场投手行与野手行并存时只补记可计入球员行")
    void createUpdatesOnlyCountedPlayerRow() {
        GamePlayerStat pitcherRow = new GamePlayerStat();
        pitcherRow.setId(301L);
        pitcherRow.setTenantId(1L);
        pitcherRow.setGameId(20L);
        pitcherRow.setTeamId(200L);
        pitcherRow.setPlayerId(23L);
        pitcherRow.setIsPitcher(1);
        pitcherRow.setPosition("P");
        pitcherRow.setA(2);
        pitcherRow.setTc(4);
        game.setLiveSnapshotJson(snapshot("E", "失误：CF 记 E"));
        when(adjustmentRepository
                .findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
                        1L, 20L, "play-1", 23L))
                .thenReturn(Optional.empty());
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(fielderStat, pitcherRow));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(23L, "主中外")));

        service.create(game, 9L, request("play-1", 23L, "CF"));

        assertEquals(3, fielderStat.getA());
        assertEquals(5, fielderStat.getTc());
        assertEquals(2, pitcherRow.getA(), "投手并行行不应重复补记");
        assertEquals(4, pitcherRow.getTc());
    }

    @Test
    @DisplayName("撤销补记：保留审计行并从球员 A/TC 与位置 A 扣回")
    void revokeSubtractsCreditsAndKeepsAuditRow() {
        GameFieldingAdjustment row = adjustment("APPROVED");
        row.setId(77L);
        fielderStat.setA(3);
        fielderStat.setTc(5);
        positionRow.setA(3);
        when(adjustmentRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(Optional.of(row));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));

        FieldingAdjustmentListDTO result = service.revoke(game, 77L, 9L);

        assertEquals("REVOKED", result.status());
        assertEquals(9L, row.getRevokedBy());
        assertNotNull(row.getRevokedAt());
        assertEquals(2, fielderStat.getA());
        assertEquals(4, fielderStat.getTc());
        assertEquals(2, positionRow.getA());
    }

    @Test
    @DisplayName("源 Play 不在快照中时拒绝新增补记")
    void createRejectsMissingSourcePlay() {
        game.setLiveSnapshotJson(snapshot("E", "失误：CF 记 E"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(game, 9L, request("missing-play", 23L, "CF")));

        assertEquals(400, error.getCode());
        verify(adjustmentRepository, never()).save(any(GameFieldingAdjustment.class));
    }

    @Test
    @DisplayName("非失误 Play 不能补记助杀")
    void createRejectsNonErrorPlay() {
        game.setLiveSnapshotJson(snapshot("K", "5下 三振"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(game, 9L, request("play-1", 23L, "CF")));

        assertEquals(400, error.getCode());
        verify(adjustmentRepository, never()).save(any(GameFieldingAdjustment.class));
    }

    @Test
    @DisplayName("补记对象必须在本场守备方统计行中")
    void createRejectsPlayerOutsideFieldingTeam() {
        game.setLiveSnapshotJson(snapshot("E", "失误：CF 记 E"));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(game, 9L, request("play-1", 23L, "CF")));

        assertEquals(400, error.getCode());
    }

    @Test
    @DisplayName("save-live 前复核：源 Play 丢失时先失效并扣回，避免重放失效补记")
    void refreshApprovedValidityInvalidatesBeforeReplay() {
        GameFieldingAdjustment row = adjustment("APPROVED");
        row.setId(77L);
        fielderStat.setA(3);
        fielderStat.setTc(5);
        positionRow.setA(3);
        game.setLiveSnapshotJson("""
                {"playEventsComplete":true,"playEvents":[]}
                """);
        when(adjustmentRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(row));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));

        service.refreshApprovedValidity(game);

        assertEquals("INVALIDATED", row.getStatus());
        assertEquals(2, fielderStat.getA());
        assertEquals(4, fielderStat.getTc());
        assertEquals(2, positionRow.getA());
    }

    @Test
    @DisplayName("完整快照中源 Play 丢失时，列表复核自动失效并扣回统计")
    void listInvalidatesAdjustmentWhenSourcePlayDisappears() {
        GameFieldingAdjustment row = adjustment("APPROVED");
        row.setId(77L);
        fielderStat.setA(3);
        fielderStat.setTc(5);
        positionRow.setA(3);
        game.setLiveSnapshotJson("""
                {"playEventsComplete":true,"playEvents":[]}
                """);
        when(adjustmentRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(row));
        when(adjustmentRepository
                .findByTenantIdAndGameIdOrderByInningAscTopBottomAscSourceSequenceAscIdAsc(1L, 20L))
                .thenReturn(List.of(row));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));

        List<FieldingAdjustmentListDTO> rows = service.list(game);

        assertEquals("INVALIDATED", rows.get(0).status());
        assertFalse(rows.get(0).sourceValid());
        assertNotNull(row.getInvalidReason());
        assertEquals(2, fielderStat.getA());
        assertEquals(4, fielderStat.getTc());
        assertEquals(2, positionRow.getA());
    }

    @Test
    @DisplayName("保存覆盖统计后按有效补记重放球员与位置 A/TC")
    void reapplyAddsApprovedAdjustmentsAfterStatsSave() {
        GameFieldingAdjustment row = adjustment("APPROVED");
        when(adjustmentRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(row));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));

        service.reapplyPlayerStats(game, java.util.Set.of("200_23_0"));
        service.reapplyPositionStats(game);

        assertEquals(3, fielderStat.getA());
        assertEquals(5, fielderStat.getTc());
        assertEquals(3, positionRow.getA());
    }

    @Test
    @DisplayName("部分统计载荷未写入的行不重放补记，避免重复 +1")
    void reapplySkipsUntouchedStatRows() {
        GameFieldingAdjustment row = adjustment("APPROVED");
        when(adjustmentRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(row));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));

        service.reapplyPlayerStats(game, java.util.Set.of("999_23_0"));

        assertEquals(2, fielderStat.getA());
        assertEquals(4, fielderStat.getTc());
        verify(gamePlayerStatRepository, never()).save(fielderStat);
    }

    @Test
    @DisplayName("撤销后重新补记复用原行并重新生效")
    void createReactivatesRevokedAdjustment() {
        GameFieldingAdjustment row = adjustment("REVOKED");
        row.setId(77L);
        row.setRevokedBy(9L);
        game.setLiveSnapshotJson(snapshot("E", "失误：CF 记 E"));
        when(adjustmentRepository
                .findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
                        1L, 20L, "play-1", 23L))
                .thenReturn(Optional.of(row));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(fielderStat));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of(positionRow));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(23L, "主中外")));

        FieldingAdjustmentListDTO result = service.create(game, 9L, request("play-1", 23L, "CF"));

        assertEquals("APPROVED", result.status());
        assertNull(row.getRevokedBy());
        assertEquals(3, fielderStat.getA());
        assertEquals(3, positionRow.getA());
    }

    private static String snapshot(String resultCode, String resultText) {
        return """
                {"playEventsComplete":true,"playEvents":[{
                  "id":"play-1","inning":5,"half":"bottom","sequence":167,
                  "outsBefore":1,"resultCode":"%s","resultText":"%s"
                }]}
                """.formatted(resultCode, resultText.replace("\"", "\\\""));
    }

    private static FieldingAdjustmentCreateRequest request(
            String sourceEventId, Long playerId, String position) {
        FieldingAdjustmentCreateRequest request = new FieldingAdjustmentCreateRequest();
        request.setSourceEventId(sourceEventId);
        request.setPlayerId(playerId);
        request.setPosition(position);
        request.setNote("外野传本垒，捕手接球失误");
        return request;
    }

    private static GameFieldingAdjustment adjustment(String status) {
        GameFieldingAdjustment row = new GameFieldingAdjustment();
        row.setTenantId(1L);
        row.setGameId(20L);
        row.setSourceEventId("play-1");
        row.setSourceSequence(167);
        row.setInning(5);
        row.setTopBottom("bottom");
        row.setTeamId(200L);
        row.setPlayerId(23L);
        row.setPosition("CF");
        row.setAssistCount(1);
        row.setStatus(status);
        row.setSourceText("5下 失误：CF 主中外 (#23) 记 E");
        return row;
    }

    private static Player player(Long id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        return player;
    }
}
