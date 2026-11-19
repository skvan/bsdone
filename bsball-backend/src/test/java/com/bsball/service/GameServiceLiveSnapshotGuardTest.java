package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSnapshotRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 现场快照覆盖守门：线上 game 261 在赛后第七天被一次阵容板保存盖成「只剩 2 条 playEvents、
 * 比分 0」，逐棒证据消失后 3局下 那一分再也无法追溯；game 260 同样停在 1 局空壳。
 * 这里守住「有逐棒证据就不许被更不完整的快照覆盖」，同时不能挡住正常收尾与开局。
 *
 * <p>快照正文自 Issue #686 起不再挂在 Game 实体上，而是走 GameSnapshotRepository 的局部实体，
 * 所以本测试的「服务器现有快照」一律用 repository 打桩，断言看的是 updateSnapshotJson 有没有被调用，
 * 不再读 Game.getLiveSnapshotJson()（那个 getter 已不存在）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService 快照保存：已结算场次的逐棒证据不可被空壳覆盖")
class GameServiceLiveSnapshotGuardTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 261L;
    private static final long EVENT_ID = 17L;

    /** 记分 18:6、快照有 24 条 playEvents（teamsData 合计 24 分）的已结算现场。 */
    private static final String RICH_SNAPSHOT = "{\"gameId\":" + GAME_ID + ",\"playEvents\":["
            + repeatedEvents(24, 24) + "],\"teamsData\":{\"home\":{\"score\":6},\"away\":{\"score\":18}}}";

    /** 阵容板 payload 的形状：只有名单，没有 playEvents、没有任何得分。 */
    private static final String LINEUP_ONLY_SNAPSHOT = "{\"gameId\":" + GAME_ID
            + ",\"homeLineup\":[1,2],\"awayLineup\":[3,4]}";

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameSnapshotRepository gameSnapshotRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private GameSubstitutionEventRepository gameSubstitutionEventRepository;
    @Mock
    private GameFieldingPosStatRepository gameFieldingPosStatRepository;
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
    @Mock
    private GameFieldingAdjustmentService fieldingAdjustmentService;

    private GameService gameService;
    private Game game;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository,
                gameSubstitutionEventRepository, eventRepository, stadiumRepository,
                accountScopeService, scopeQuerySupport, resourceGuard, tenantQueryPolicyService,
                earnedRunReconstructionService, eventRegistrationService, gameFieldingPosStatRepository,
                fieldingAdjustmentService, Mockito.mock(GameRevisionAuditService.class),
                gameSnapshotRepository);
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));

        game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        game.setAwayScore(Integer.valueOf(18));
        game.setHomeScore(Integer.valueOf(6));
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));

        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));
        lenient().when(gameSnapshotRepository.updateSnapshotJson(any(), any(), any()))
                .thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("阵容板 payload（未提到 playEvents）：并入现有证据后放行，不是顶回也不是清空")
    void saveLiveSnapshot_mergesLineupOnlyPayloadAndKeepsEvidence() {
        givenExistingSnapshot(RICH_SNAPSHOT);

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), LINEUP_ONLY_SNAPSHOT);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        String saved = written.getValue();
        // 证据不丢：24 条逐棒记录与比分都跟着回去
        assertEquals(24, playEventCount(saved), "阵容板保存不能把逐棒记录抹掉：" + saved);
        assertTrue(saved.contains("\"teamsData\""), "比分段也要并回来：" + saved);
        // 它自己的改动也不能被吃掉
        assertTrue(saved.contains("homeLineup"), "阵容板改的名单要留住：" + saved);
    }

    @Test
    @DisplayName("快照得分落后于记分栏（game 260 形状：现场停在 1 局）不得覆盖")
    void saveLiveSnapshot_rejectsSnapshotBehindScoreboard() {
        givenExistingSnapshot(RICH_SNAPSHOT);
        String stale = "{\"playEvents\":[" + repeatedEvents(9, 2) + "],"
                + "\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":2}}}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), stale));

        assertEquals(409, ex.getCode());
        verify(gameSnapshotRepository, never()).updateSnapshotJson(any(), any(), any());
    }

    @Test
    @DisplayName("非法快照 JSON 不抛 NPE：当作读不出得分拒绝覆盖")
    void saveLiveSnapshot_rejectsUnparsablePayloadWithoutCrashing() {
        givenExistingSnapshot(RICH_SNAPSHOT);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{不是 JSON"));

        assertEquals(409, ex.getCode());
    }

    @Test
    @DisplayName("结算后的最后一次完整存档要放行（playEvents 只增不减、得分对得上）")
    void saveLiveSnapshot_allowsCompleteOrLaterSnapshot() {
        givenExistingSnapshot(RICH_SNAPSHOT);
        // scoreDelta 的数字形状（{away,home} 之外的另一种线上写法）与更多事件数
        String advance = "{\"playEvents\":[" + repeatedEvents(26, 26) + "],"
                + "\"teamsData\":{\"home\":{\"score\":6},\"away\":{\"score\":18}}}";

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), advance);

        // 放行时要把本次正文真的写进快照列（#686 后这是唯一落库途径）
        verify(gameSnapshotRepository).updateSnapshotJson(Long.valueOf(GAME_ID), advance,
                Long.valueOf(USER_ID));
    }

    @Test
    @DisplayName("旧快照没有逐棒记录时不设限：刚建场/从未现场记录的场次照常保存")
    void saveLiveSnapshot_allowsWhenNoEvidenceToLose() {
        givenExistingSnapshot(LINEUP_ONLY_SNAPSHOT);

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), RICH_SNAPSHOT);

        verify(gameSnapshotRepository).updateSnapshotJson(Long.valueOf(GAME_ID), RICH_SNAPSHOT,
                Long.valueOf(USER_ID));
    }

    @Test
    @DisplayName("服务器快照自己落后于记分时阵容板仍要能保存：倒退只看 payload 自己带的段落")
    void saveLiveSnapshot_lineupPayloadNotBlamedForLaggingServerSnapshot() {
        // game 260 型：逐棒记录有 24 条，但快照里的 teamsData 停在 2 分（现场中断过、比分栏被人
        // 工补写过）。合并之后正文得分就是这 2 分，拿它去比记分 24 分会把阵容板顶回 409——
        // 而阵容板连一个分都没提，不能为服务器自己的落后负责。
        givenExistingSnapshot("{\"gameId\":" + GAME_ID + ",\"playEvents\":["
                + repeatedEvents(24, 2) + "],\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":2}}}");

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), LINEUP_ONLY_SNAPSHOT);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        String saved = written.getValue();
        assertEquals(24, playEventCount(saved), "证据不能因为这道误判而丢：" + saved);
        assertTrue(saved.contains("homeLineup"), "阵容板改的名单要留住：" + saved);
    }

    @Test
    @DisplayName("自带空 playEvents 数组的收缩仍拒：比分 0:0 也不允许 24 条掉到 0 条")
    void saveLiveSnapshot_countGuardAppliesEvenWithoutScore() {
        game.setAwayScore(Integer.valueOf(0));
        game.setHomeScore(Integer.valueOf(0));
        givenExistingSnapshot(RICH_SNAPSHOT);
        // 与上一例的差别就在「有没有自己带 playEvents」：带了（哪怕是空数组）就是真要覆写，
        // 没带才是“我没说到这件事”。game 261 属于前者（被盖成只剩 2 条）。
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{\"playEvents\":[]}"));

        verify(gameSnapshotRepository, never()).updateSnapshotJson(any(), any(), any());
        assertEquals(409, ex.getCode(),
                "playEvents 从 24 条掉到 0 条仍须拒绝（与记分无关）：" + ex.getMessage());
    }

    /** 服务器现有快照正文：#686 后只能从 GameSnapshotRepository 读，不能再塞给 Game 实体。 */
    private void givenExistingSnapshot(String existingJson) {
        when(gameSnapshotRepository.findSnapshotJsonById(Long.valueOf(GAME_ID)))
                .thenReturn(Optional.of(existingJson));
    }

    /** 落库正文里的 playEvents 条数：服务端的计数方法是 private，测试自己解析一份对照。 */
    private static int playEventCount(String json) {
        try {
            return new ObjectMapper().readTree(json).path("playEvents").size();
        }
        catch (Exception ex) {
            throw new AssertionError("落库正文解不开：" + json, ex);
        }
    }

    /** 生成 n 条 playEvents，其中 m 条带 1 分（scoreDelta 为 {away,home} 对象形状）。 */
    private static String repeatedEvents(int n, int m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            boolean scores = i < m;
            sb.append("{\"inning\":").append(i + 1).append(",\"half\":\"top\",\"scoreDelta\":{\"away\":")
                    .append(scores ? 1 : 0).append(",\"home\":0}}");
        }
        return sb.toString();
    }
}
