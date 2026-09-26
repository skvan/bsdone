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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.StadiumRepository;
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
    private EventRepository eventRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private DataScopeService dataScopeService;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    private GameService gameService;

    /** setUp 中由 findById 回傳的既有比賽（供欄位級斷言） */
    private Game existingGame;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, eventRepository,
                stadiumRepository, dataScopeService, tenantQueryPolicyService);
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
        when(dataScopeService.resolve(any(), anyLong())).thenReturn(EffectiveDataScope.unrestricted());
        when(gamePlayerStatRepository.save(any(GamePlayerStat.class))).thenAnswer(inv -> inv.getArgument(0));
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
