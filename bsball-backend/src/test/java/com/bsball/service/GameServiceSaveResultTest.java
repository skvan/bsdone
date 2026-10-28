/*
 * GameService#saveResult 行落點迴歸測試。
 * 參考 DataScopeServiceTest 的純單元寫法：Repository 與協作 Service 一律用 Mockito mock，
 * 不啟動 Spring 容器、不連資料庫、不存取網路。
 *
 * 守護的行為（issue #65）：同一球員可有「打者列(isPitcher=0) + 投手列(isPitcher=1)」兩列並存，
 * saveResult 不得因 payload 少帶或錯帶 id 就把另一列物理刪除，也不得把既有列的 isPitcher 就地翻轉。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService.saveResult：投手兼打者的兩列去留與身分")
class GameServiceSaveResultTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 222L;
    private static final long EVENT_ID = 14L;
    private static final long TEAM_ID = 50L;
    private static final long PLAYER_ID = 1192L;
    /** 打者列 id */
    private static final long BATTER_ROW_ID = 4719L;
    /** 投手列 id */
    private static final long PITCHER_ROW_ID = 4726L;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private GameSubstitutionEventRepository gameSubstitutionEventRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private EarnedRunReconstructionService earnedRunReconstructionService;
    @Mock
    private EventRegistrationService eventRegistrationService;

    private GameService gameService;

    /** setUp 中由 findById 回傳的既有比賽（供欄位級斷言） */
    private Game existingGame;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository, eventRepository,
                stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService);
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));

        Game game = new Game();
        existingGame = game;
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        // 跳過先發守位檢查，讓本測試聚焦在列的落點與去留
        game.setIsSpecialResult(Boolean.TRUE);
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));

        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));
        // lenient：先發守位查重的攔截用例（#274）在寫入前就拋 400，走不到 save，嚴格樁會誤報未使用
        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        // CurrentUserHolder 基於 ThreadLocal，測試後必須清理，避免污染其他用例
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("投手筆錯帶打者列 id：投手列不被刪除、兩列身分不翻轉")
    void saveResult_misplacedId_keepsPitcherRowAndIdentity() {
        GamePlayerStat batter = batterRow();
        GamePlayerStat pitcher = pitcherRow();
        givenExistingRows(batter, pitcher);
        // 現行 admin 結果頁的 payload：兩筆都帶打者列 id（Jn() 的 statId 優先序寫反）
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(Arrays.asList(
                part(Long.valueOf(BATTER_ROW_ID), Long.valueOf(PLAYER_ID), 0, Integer.valueOf(3), null),
                part(Long.valueOf(BATTER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(3), Double.valueOf(5.2D)))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(0, deletedRows().size(), "兩列都應被本次落點命中，不允許任何刪除");
        assertSame(batter, savedInstance(BATTER_ROW_ID), "打者筆應落在打者列");
        assertSame(pitcher, savedInstance(PITCHER_ROW_ID), "投手筆應改落投手列，而不是覆寫打者列");
        assertEquals(Integer.valueOf(0), batter.getIsPitcher(), "打者列的 isPitcher 必須仍是 0");
        assertEquals(Integer.valueOf(1), pitcher.getIsPitcher(), "投手列的 isPitcher 必須仍是 1");
        assertEquals(Double.valueOf(5.2D), pitcher.getIp(), "投手數值應落在投手列");
    }

    @Test
    @DisplayName("正確 payload：各寫各自那列，投手數值落在投手列")
    void saveResult_correctPayload_writesEachRow() {
        GamePlayerStat batter = batterRow();
        GamePlayerStat pitcher = pitcherRow();
        givenExistingRows(batter, pitcher);
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(Arrays.asList(
                part(Long.valueOf(BATTER_ROW_ID), Long.valueOf(PLAYER_ID), 0, Integer.valueOf(4), null),
                part(Long.valueOf(PITCHER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(0), Double.valueOf(6.1D)))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(0, deletedRows().size());
        assertEquals(Integer.valueOf(4), batter.getAb());
        assertEquals(Double.valueOf(6.1D), pitcher.getIp());
        assertEquals(Integer.valueOf(0), batter.getIsPitcher());
        assertEquals(Integer.valueOf(1), pitcher.getIsPitcher());
    }

    @Test
    @DisplayName("球員整位從 payload 消失：他的列仍會被刪除（保留原語義）")
    void saveResult_playerGone_stillDeletes() {
        givenExistingRows(batterRow(), pitcherRow());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        // 另一位球員，與既有兩列無關
        dto.setStats(new ArrayList<>(List.of(
                part(null, Long.valueOf(777L), 0, Integer.valueOf(1), null))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        List<GamePlayerStat> deleted = deletedRows();
        assertEquals(2, deleted.size(), "該球員的兩列都應被刪除");
        assertEquals(BATTER_ROW_ID, deleted.get(0).getId().longValue());
        assertEquals(PITCHER_ROW_ID, deleted.get(1).getId().longValue());
    }

    @Test
    @DisplayName("編輯保存：清空「場次/場地」（傳 null）應真正寫入 null（issue #28）")
    void saveResult_nullGameNumberAndVenue_clearsThem() {
        existingGame.setGameNumber(Integer.valueOf(3));
        existingGame.setVenue("熊貓紀念球場");
        givenExistingRows();
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setGame(gamePart(null, null));
        dto.setStats(new ArrayList<>(List.of(
                part(null, Long.valueOf(777L), 0, Integer.valueOf(1), null))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertNull(existingGame.getGameNumber(), "清空後 gameNumber 應為 null");
        assertNull(existingGame.getVenue(), "清空後 venue 應為 null");
    }

    @Test
    @DisplayName("編輯保存：帶值時仍正常寫入「場次/場地」")
    void saveResult_presentGameNumberAndVenue_writesThem() {
        givenExistingRows();
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setGame(gamePart(Integer.valueOf(7), "新生公園棒球場"));
        dto.setStats(new ArrayList<>(List.of(
                part(null, Long.valueOf(777L), 0, Integer.valueOf(2), null))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(7), existingGame.getGameNumber());
        assertEquals("新生公園棒球場", existingGame.getVenue());
    }

    // ------------------------------------------------------------------ 先發守位查重排除項（issue #274）

    @Test
    @DisplayName("多个 EP 落在前 9 棒不误报守备位置重复")
    void saveResult_multipleEpsInTopNine_notMisreported() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        // 11 人陣（8 守位 + DH + 2 EP）；兩個 EP 都排在前 9 棒——修復前會把 EP 當守備位而誤報重複
        dto.setStats(List.of(
                starterPart(1, "SS"), starterPart(2, "2B"), starterPart(3, "1B"), starterPart(4, "3B"),
                starterPart(5, "EP"), starterPart(6, "EP"), starterPart(7, "LF"), starterPart(8, "CF"),
                starterPart(9, "RF"), starterPart(10, "C"), starterPart(11, "DH")));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        verify(gamePlayerStatRepository, times(11)).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("DH 不参与守备查重")
    void saveResult_singleDh_notFieldingChecked() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        // 9 人 DH 制：真實守位各不相同，DH 落在前 9 棒內也不應被當作重複項
        dto.setStats(List.of(
                starterPart(1, "SS"), starterPart(2, "2B"), starterPart(3, "1B"), starterPart(4, "3B"),
                starterPart(5, "LF"), starterPart(6, "CF"), starterPart(7, "RF"), starterPart(8, "C"),
                starterPart(9, "DH")));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        verify(gamePlayerStatRepository, times(9)).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("真实守备位重复仍拦截")
    void saveResult_duplicatedFieldingPosition_stillRejected() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        SaveGameResultDTO dto = new SaveGameResultDTO();
        // SS 佔兩棒（其餘守位各一、含一個 DH）：排除 DH/EP 不得把真實守位重複一併放過
        dto.setStats(List.of(
                starterPart(1, "SS"), starterPart(2, "SS"), starterPart(3, "1B"), starterPart(4, "3B"),
                starterPart(5, "LF"), starterPart(6, "CF"), starterPart(7, "RF"), starterPart(8, "C"),
                starterPart(9, "DH")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveResult(Long.valueOf(GAME_ID), dto));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("守备位置重复"));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
        verify(gamePlayerStatRepository, never()).findByGameId(anyLong());
    }

    // ------------------------------------------------------------------ 生产比赛 250 热修：final 补录 / 二刀流打序行

    @Test
    @DisplayName("已完赛（final）补录：先发不足 9 人不再拦截，统计正常落库")
    void saveResult_finalGame_skipsStarterValidation() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        existingGame.setStatus("final");
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        // 赛后补录只带部分球员，不应再要求先发 9 人齐全
        dto.setStats(List.of(starterPart(1, "SS"), starterPart(2, "2B")));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        verify(gamePlayerStatRepository, times(2)).save(any(GamePlayerStat.class));
        // 先发校验放行不代表门②放行：秩序册统计校验仍必须执行
        verify(eventRegistrationService).assertStatsPartsInRoster(eq(Long.valueOf(EVENT_ID)), eq(Long.valueOf(GAME_ID)), anyCollection());
    }

    @Test
    @DisplayName("现存比赛为 live、本次请求置 final：按 final 放行（同一事务内完赛）")
    void saveResult_liveToFinalInOneRequest_skipsStarterValidation() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        existingGame.setStatus("live");
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of());
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setStatus("final");
        dto.setGame(gamePart);
        dto.setStats(List.of(starterPart(1, "SS"), starterPart(2, "2B")));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        verify(gamePlayerStatRepository, times(2)).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("现存比赛为 final、本次请求改回 live：仍按 live 校验并拦截")
    void saveResult_finalToLiveInOneRequest_stillValidates() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        existingGame.setStatus("final");
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setStatus("live");
        dto.setGame(gamePart);
        dto.setStats(List.of(starterPart(1, "SS"), starterPart(2, "2B")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveResult(Long.valueOf(GAME_ID), dto));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("有打序的先发不足 9 人"));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("进行中（live）：先发不足 9 人仍拦截")
    void saveResult_liveGame_stillRequiresNineStarters() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        existingGame.setStatus("live");
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(List.of(starterPart(1, "SS"), starterPart(2, "2B")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveResult(Long.valueOf(GAME_ID), dto));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("有打序的先发不足 9 人"));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("二刀流打序行（isPitcher=1）不再被剔除：9 人打序仍可通过守位校验")
    void saveResult_twoWayBattingRows_stillCounted() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        existingGame.setStatus("live");
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(List.of());
        // 结果录入页会把「打者兼投手」合并成一行（isPitcher=1 且带打序）；
        // 旧实现先剔除 isPitcher=1 的行，9 人打序被算成 7 人而误报。
        SaveGameResultDTO.StatPart twoWayFourth = starterPart(4, "SS");
        twoWayFourth.setIsPitcher(Integer.valueOf(1));
        SaveGameResultDTO.StatPart twoWayNinth = starterPart(9, "C");
        twoWayNinth.setIsPitcher(Integer.valueOf(1));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(List.of(
                starterPart(1, "2B"), starterPart(2, "LF"), starterPart(3, "3B"), twoWayFourth,
                starterPart(5, "CF"), starterPart(6, "1B"), starterPart(7, "RF"), starterPart(8, "P"),
                twoWayNinth));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        verify(gamePlayerStatRepository, times(9)).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("同一球员同时带打者行与投手行：按球员去重后不足 9 人仍拦截")
    void saveResult_duplicateRowsPerPlayer_countedOnce() {
        existingGame.setIsSpecialResult(Boolean.FALSE);
        existingGame.setStatus("live");
        // 只有 8 名不同球员；第 8 棒二刀流同时提交打者行与投手行，凑出的 9 行不能算 9 名先发
        SaveGameResultDTO.StatPart pitcherRow = starterPart(8, "P");
        pitcherRow.setIsPitcher(Integer.valueOf(1));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(List.of(
                starterPart(1, "2B"), starterPart(2, "LF"), starterPart(3, "3B"), starterPart(4, "SS"),
                starterPart(5, "CF"), starterPart(6, "1B"), starterPart(7, "RF"), starterPart(8, "C"),
                pitcherRow));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveResult(Long.valueOf(GAME_ID), dto));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("有打序的先发不足 9 人"));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    /* ---------- fixtures ---------- */

    private GamePlayerStat batterRow() {
        GamePlayerStat row = new GamePlayerStat();
        row.setId(Long.valueOf(BATTER_ROW_ID));
        row.setGameId(Long.valueOf(GAME_ID));
        row.setTeamId(Long.valueOf(TEAM_ID));
        row.setPlayerId(Long.valueOf(PLAYER_ID));
        row.setIsPitcher(Integer.valueOf(0));
        row.setBattingOrder(Integer.valueOf(5));
        row.setAb(Integer.valueOf(3));
        row.setH(Integer.valueOf(2));
        return row;
    }

    private GamePlayerStat pitcherRow() {
        GamePlayerStat row = new GamePlayerStat();
        row.setId(Long.valueOf(PITCHER_ROW_ID));
        row.setGameId(Long.valueOf(GAME_ID));
        row.setTeamId(Long.valueOf(TEAM_ID));
        row.setPlayerId(Long.valueOf(PLAYER_ID));
        row.setIsPitcher(Integer.valueOf(1));
        row.setPitcherOrder(Integer.valueOf(3));
        row.setIp(Double.valueOf(0.1D));
        row.setNp(Integer.valueOf(40));
        return row;
    }

    @Test
    @DisplayName("引擎场次保存结果：payload 漏带的投手行不得被物理删除")
    void saveResult_engineManaged_keepsMissingPitcherRow() {
        GamePlayerStat batter = batterRow();
        GamePlayerStat pitcher = pitcherRow();
        givenExistingRows(batter, pitcher);
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.TRUE);
        SaveGameResultDTO dto = new SaveGameResultDTO();
        // 只带打者行，投手行缺席
        dto.setStats(new ArrayList<>(List.of(
                part(Long.valueOf(BATTER_ROW_ID), Long.valueOf(PLAYER_ID), 0, Integer.valueOf(4), null))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(0, deletedRows().size(), "引擎维护的投手行不得因 payload 漏带被删除");
    }

    @Test
    @DisplayName("引擎场次保存结果：客户端 er/pitchR 不得覆盖引擎累计值")
    void saveResult_engineManaged_ignoresClientEarnedAndPitchRuns() {
        GamePlayerStat pitcher = pitcherRow();
        pitcher.setEr(Integer.valueOf(3));
        pitcher.setPitchR(Integer.valueOf(5));
        givenExistingRows(pitcher);
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.TRUE);

        SaveGameResultDTO.StatPart part = part(Long.valueOf(PITCHER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(0), Double.valueOf(6.1D));
        part.setEr(Integer.valueOf(99));
        part.setPitchR(Integer.valueOf(88));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(part)));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(3), pitcher.getEr(), "引擎场次 ER 只能由引擎写");
        assertEquals(Integer.valueOf(5), pitcher.getPitchR(), "引擎场次 R 只能由引擎写");
    }

    @Test
    @DisplayName("无引擎数据的 legacy 场次：saveResult 仍可手工修正 ER/R")
    void saveResult_legacy_writesClientEarnedAndPitchRuns() {
        GamePlayerStat pitcher = pitcherRow();
        pitcher.setEr(Integer.valueOf(3));
        pitcher.setPitchR(Integer.valueOf(5));
        givenExistingRows(pitcher);
        when(earnedRunReconstructionService.hasEngineData(Long.valueOf(GAME_ID)))
                .thenReturn(Boolean.FALSE);

        SaveGameResultDTO.StatPart part = part(Long.valueOf(PITCHER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(0), Double.valueOf(6.1D));
        part.setEr(Integer.valueOf(4));
        part.setPitchR(Integer.valueOf(6));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(part)));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(4), pitcher.getEr());
        assertEquals(Integer.valueOf(6), pitcher.getPitchR());
    }

    @Test
    @DisplayName("saveResult：纸质表单新增字段全部落库")
    void saveResult_paperFormFields_written() {
        GamePlayerStat pitcher = pitcherRow();
        givenExistingRows(pitcher);
        SaveGameResultDTO.StatPart p = part(Long.valueOf(PITCHER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(0), Double.valueOf(6.1D));
        p.setGdp(Integer.valueOf(1));
        p.setIbb(Integer.valueOf(2));
        p.setIo(Integer.valueOf(1));
        p.setPitchDoubles(Integer.valueOf(2));
        p.setPitchTriples(Integer.valueOf(1));
        p.setPitchSh(Integer.valueOf(1));
        p.setPitchSf(Integer.valueOf(1));
        p.setPitchIo(Integer.valueOf(1));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(p)));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(1), pitcher.getGdp());
        assertEquals(Integer.valueOf(2), pitcher.getIbb());
        assertEquals(Integer.valueOf(1), pitcher.getIo());
        assertEquals(Integer.valueOf(2), pitcher.getPitchDoubles());
        assertEquals(Integer.valueOf(1), pitcher.getPitchTriples());
        assertEquals(Integer.valueOf(1), pitcher.getPitchSh());
        assertEquals(Integer.valueOf(1), pitcher.getPitchSf());
        assertEquals(Integer.valueOf(1), pitcher.getPitchIo());
    }

    @Test
    @DisplayName("saveResult：载荷缺投手四死栏时保留既有值（与 saveLive 同口径，#387）")
    void saveResult_missingPitchingFourDeath_preservesExisting() {
        GamePlayerStat pitcher = pitcherRow();
        givenExistingRows(pitcher);
        pitcher.setPitchBbHp(Integer.valueOf(7));
        pitcher.setPitchBb(Integer.valueOf(5));
        pitcher.setPitchIbb(Integer.valueOf(1));
        pitcher.setPitchHbp(Integer.valueOf(2));

        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(
                part(Long.valueOf(PITCHER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(16), Double.valueOf(3.0D)))));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(7), pitcher.getPitchBbHp(), "四死合计缺栏不得被抹成 NULL");
        assertEquals(Integer.valueOf(5), pitcher.getPitchBb());
        assertEquals(Integer.valueOf(1), pitcher.getPitchIbb());
        assertEquals(Integer.valueOf(2), pitcher.getPitchHbp(), "触身球缺栏不得被抹成 NULL");
    }

    @Test
    @DisplayName("saveResult：显式送 0 仍能清零")
    void saveResult_explicitZero_stillClears() {
        GamePlayerStat pitcher = pitcherRow();
        givenExistingRows(pitcher);
        pitcher.setPitchBbHp(Integer.valueOf(7));
        pitcher.setPitchBb(Integer.valueOf(5));
        pitcher.setPitchIbb(Integer.valueOf(1));
        pitcher.setPitchHbp(Integer.valueOf(2));

        SaveGameResultDTO.StatPart p =
                part(Long.valueOf(PITCHER_ROW_ID), Long.valueOf(PLAYER_ID), 1, Integer.valueOf(16), Double.valueOf(3.0D));
        p.setPitchBbHp(Integer.valueOf(0));
        p.setPitchBb(Integer.valueOf(0));
        p.setPitchIbb(Integer.valueOf(0));
        p.setPitchHbp(Integer.valueOf(0));
        SaveGameResultDTO dto = new SaveGameResultDTO();
        dto.setStats(new ArrayList<>(List.of(p)));

        gameService.saveResult(Long.valueOf(GAME_ID), dto);

        assertEquals(Integer.valueOf(0), pitcher.getPitchBbHp());
        assertEquals(Integer.valueOf(0), pitcher.getPitchBb());
        assertEquals(Integer.valueOf(0), pitcher.getPitchIbb());
        assertEquals(Integer.valueOf(0), pitcher.getPitchHbp());
    }

    private SaveGameResultDTO.StatPart part(Long id, Long playerId, int isPitcher, Integer ab, Double ip) {
        SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
        part.setId(id);
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(playerId);
        part.setIsPitcher(Integer.valueOf(isPitcher));
        part.setAb(ab);
        part.setIp(ip);
        return part;
    }

    private SaveGameResultDTO.GamePart gamePart(Integer gameNumber, String venue) {
        SaveGameResultDTO.GamePart g = new SaveGameResultDTO.GamePart();
        g.setEventId(Long.valueOf(EVENT_ID));
        g.setGameNumber(gameNumber);
        g.setVenue(venue);
        return g;
    }

    /** 先發打者筆：同隊、帶打序與位置（isPitcher=0），球員 id 由打序推得 */
    private SaveGameResultDTO.StatPart starterPart(int battingOrder, String position) {
        SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
        part.setTeamId(Long.valueOf(TEAM_ID));
        part.setPlayerId(Long.valueOf(2000L + battingOrder));
        part.setIsPitcher(Integer.valueOf(0));
        part.setBattingOrder(Integer.valueOf(battingOrder));
        part.setPosition(position);
        return part;
    }

    private void givenExistingRows(GamePlayerStat... rows) {
        final List<GamePlayerStat> list = Arrays.asList(rows);
        when(gamePlayerStatRepository.findByGameId(Long.valueOf(GAME_ID))).thenReturn(list);
        // 修正前的 saveResult 靠 existsById/findById 定位既有列，修正後改由 findByGameId 回傳的列直接比對。
        // 這裡一律補上 lenient stub，讓同一份測試在未修正的代碼上也能重現真實行為：
        // 缺了它們，舊實作會把兩筆都當成新增列（走 new GamePlayerStat），
        // 「正確 payload 原本就正常」那條回歸斷言就會變成與被測代碼無關的偽失敗。
        lenient().when(gamePlayerStatRepository.existsById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return id != null && containsId(list, id);
        });
        lenient().when(gamePlayerStatRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return firstById(list, id);
        });
    }

    private static boolean containsId(List<GamePlayerStat> rows, Long id) {
        return firstById(rows, id).isPresent();
    }

    private static Optional<GamePlayerStat> firstById(List<GamePlayerStat> rows, Long id) {
        for (GamePlayerStat row : rows) {
            if (id != null && id.equals(row.getId())) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    private List<GamePlayerStat> deletedRows() {
        ArgumentCaptor<GamePlayerStat> captor = ArgumentCaptor.forClass(GamePlayerStat.class);
        verify(gamePlayerStatRepository, atLeast(0)).delete(captor.capture());
        return captor.getAllValues();
    }

    private GamePlayerStat savedInstance(long id) {
        ArgumentCaptor<GamePlayerStat> captor = ArgumentCaptor.forClass(GamePlayerStat.class);
        verify(gamePlayerStatRepository, atLeastOnce()).save(captor.capture());
        for (GamePlayerStat s : captor.getAllValues()) {
            if (s.getId() != null && s.getId().longValue() == id) {
                return s;
            }
        }
        return null;
    }
}
