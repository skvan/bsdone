package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.fasterxml.jackson.databind.JsonNode;
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
 * 这里守住「有逐棒证据就不许被一份自称打了更少分的快照盖掉」（得分一道），同时不能挡住正常收尾与开局。
 * 条数那道（事件变短即拒）已按 #713 的拍板改成放行：它跟撤销/重做正面冲突，而 261 那种覆盖靠得分
 * 一道就拦得住，本文件的《条数变短放行》那一例钉的就是这个新口径。
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
        // 拒收文案必须指向一个画面真做得出的动作（#713）：前端会把这句话原样弹给记录员，
        // 说「请重录」或只说「已拒绝」都会让人继续往一个进不去的存档里录下去。
        assertTrue(ex.getMessage().contains("从云端恢复录入现场"),
                "得分倒退的 409 要给得出路：" + ex.getMessage());
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
    @DisplayName("条数变短放行（#713 拍板）：撤销后的保存能把云端带回较短那一份")
    void saveLiveSnapshot_shrinkingEvidenceIsAllowed() {
        // 这一例在 #713 拍板前断的是反（409）。改放行的根据有两个：
        // ① 拒收会把场次锁死——撤销过的本地状态永远存不进去（实测：录一拍→保存→撤销→保存
        //    推上来的就是较短的一份，而记分栏不会跟着撤销回退），只能靠「从云端恢复」放弃本地；
        // ② 261 那型本来就不靠这一道——盖掉证据的那份 payload 同时主张「比分 0」，归得分守门。
        // 保留的是落库以 payload 为准（不并回旧证据）：放行要说做到底，不然只是把 24 条偷偷留回来。
        // （形状选 3 条而不是 0 条：清零已被复拍后的那一格挡回去了，见 wipingAllEventsIsRejected。）
        game.setAwayScore(Integer.valueOf(0));
        game.setHomeScore(Integer.valueOf(0));
        givenExistingSnapshot(RICH_SNAPSHOT);

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID),
                "{\"playEvents\":[" + repeatedEvents(3, 0) + "]}");

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        assertEquals(3, playEventCount(written.getValue()),
                "变短的 payload 就是落库的那一份：" + written.getValue());
    }

    @Test
    @DisplayName("清空整份证据仍拒收（#713 复拍）：撤销不可能把 N>1 条退到 0 条")
    void saveLiveSnapshot_wipingAllEventsIsRejected() {
        // 放行买的是「撤销能保存」，不该顺带把 260/261 那种空壳也买进去：一份带键但零条的
        // playEvents（前端 pa() 无条件序列化，本地归零时就是这么上来的）如果没有撤销/重做意图，
        // 仍按空壳拒收。记分栏刻意留 0：证明拦住它的是条数这道而不是得分那道，得分那道在这种
        // 场次根本不跑。
        game.setAwayScore(Integer.valueOf(0));
        game.setHomeScore(Integer.valueOf(0));
        givenExistingSnapshot(RICH_SNAPSHOT);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{\"playEvents\":[]}"));

        assertEquals(409, ex.getCode(), "清零仍要拒：" + ex.getMessage());
        assertTrue(ex.getMessage().contains("24 条"), "要说清云端现在有多少条：" + ex.getMessage());
        assertTrue(ex.getMessage().contains("从云端恢复录入现场"), "出路要写在文案里：" + ex.getMessage());
        verify(gameSnapshotRepository, never()).updateSnapshotJson(any(), any(), any());
    }

    @Test
    @DisplayName("撤销/重做意图：云端 24 条、payload 0 条也放行")
    void saveLiveSnapshot_wipingAllEventsWithUndoIntentIsAllowed() {
        // 整场撤销到 0 是合法的现场恢复：前端在 applyHistoryEntry 后显式带意图，
        // 后端只对未标记的旧设备/本地归零存档维持清零拒收。
        game.setAwayScore(Integer.valueOf(0));
        game.setHomeScore(Integer.valueOf(0));
        givenExistingSnapshot(RICH_SNAPSHOT);

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{\"playEvents\":[]}", Boolean.TRUE);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        assertEquals(0, playEventCount(written.getValue()),
                "撤销意图下云端要跟着回到空的那一份：" + written.getValue());
    }

    @Test
    @DisplayName("云端只剩 1 条时撤到 0 条要放行：那是合法的撤销，不能拿清零判据误伤它")
    void saveLiveSnapshot_undoLastEventToZeroIsAllowed() {
        // 拒收判据写的是 incoming==0 && existing>1。这一例钉住 >1 那一半，否则下一个人
        // 会把它简化成「0 条一律拒」，于是刚录了一拍就撤销的场次又回到 #713 的锁死。
        givenExistingSnapshot("{\"gameId\":" + GAME_ID + ",\"playEvents\":["
                + repeatedEvents(1, 1) + "],\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":1}}}");

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{\"playEvents\":[]}");

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        assertEquals(0, playEventCount(written.getValue()),
                "云端要跟着回到空的这一份：" + written.getValue());
    }

    @Test
    @DisplayName("真撤销形状（记分栏已有分、总分仍对得上）：变短要进得去，那才是 #713 要解的锁死")
    void saveLiveSnapshot_undoOfNonScoringPlayIsAllowed() {
        // 上一例把记分栏改成 0，验证的是「没证人时不拦」；这一例才是撤销过的现场实际发上来的东西：
        // 撤销不会回退记分栏（只有保存比赛才写它），而本地 teamsData 仍与记分栏相等，
        // 所以得分一道不跑（24==24）、条数一道放行——两道的分工就钉在这一例里。
        givenExistingSnapshot(RICH_SNAPSHOT);
        String undone = "{\"playEvents\":[" + repeatedEvents(3, 3) + "],"
                + "\"teamsData\":{\"home\":{\"score\":6},\"away\":{\"score\":18}}}";

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), undone);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        assertEquals(3, playEventCount(written.getValue()),
                "云端要跟着回到本地那份条数，不能只口头放行：" + written.getValue());
    }

    @Test
    @DisplayName("撤销掉一个有分的打席：仍被得分一道拒收（分数发生过，证据不能凭空消失）")
    void saveLiveSnapshot_undoOfScoringPlayIsStillRejected() {
        // 放行只解到「不丢分的撤销」。这一例钉住剩下的那一半，免得下一个人以为 #713 已全部解开；
        // 出路仍是原本那两条：从云端恢复，或先清空比分。
        givenExistingSnapshot(RICH_SNAPSHOT);
        String undoneRun = "{\"playEvents\":[" + repeatedEvents(3, 2) + "],"
                + "\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":2}}}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), undoneRun));

        assertEquals(409, ex.getCode(), "变短同时丢分仍要拒：" + ex.getMessage());
        verify(gameSnapshotRepository, never()).updateSnapshotJson(any(), any(), any());
    }

    @Test
    @DisplayName("证书 playEventsComplete 跟着证据一起留：阵容板保存不该把可信度降级")
    void saveLiveSnapshot_keepsPlayEventsCompleteCertificate() {
        // playEventsComplete 是「这份 playEvents 值不值得信」的证书：现场页存快照时写进去，
        // 云端恢复时按 ===true 决定 earnedRunPlayEventsTrusted。它一丢，恢复出来的场次会把
        // 本地完整的逐棒记录当成不可信，半局定案的兜底从此 no-op——等于阵容板保存一次
        // 就把 #712 的缺口扩大到本来没这个毛病的场次。
        givenExistingSnapshot("{\"playEventsComplete\":true,\"playEvents\":["
                + repeatedEvents(24, 24) + "],\"teamsData\":{\"home\":{\"score\":6},\"away\":{\"score\":18}}}");

        gameService.saveLineupSnapshot(Long.valueOf(GAME_ID), LINEUP_ONLY_SNAPSHOT);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        assertTrue(readJson(written.getValue()).path("playEventsComplete").asBoolean(false),
                "payload 没提到的证书不能因为没提到就被抹掉：" + written.getValue());

        // 反过来也一样：payload 明确说不可信时以它为准，不能拿服务器的 true 去洗
        // （这一段走现场端点：现场页才是会写证书的那个人）
        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{\"playEventsComplete\":false}");
        ArgumentCaptor<String> downgraded = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository, Mockito.times(2)).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), downgraded.capture(), eq(Long.valueOf(USER_ID)));
        JsonNode last = readJson(downgraded.getAllValues().get(1));
        assertTrue(last.path("playEventsComplete").isBoolean()
                        && !last.path("playEventsComplete").asBoolean(),
                "显式提交的 false 不该被服务器现值盖掉：" + last);
        assertEquals(24, last.path("playEvents").size(), "同时证据照旧要留：" + last);
    }

    @Test
    @DisplayName("阵容板 startEntry 的开局草稿（teamsData 全 0）：放行，比分用服务器现值")
    void saveLineupSnapshot_bootstrapZeroScoreKeepsServerScoreAndEvidence() {
        // 阵容板进入录入时会推一份 bootstrap：gameState 停在 1 局 0 出局、双方比分全 0，
        // 但不带 playEvents。它对比分的「主张」其实是开局默认值：拿它跑得分守门会把
        // 「已记分场次从阵容板进入录入」顶回 409（base 上根本没守门，那是本分支新拦的路），
        // 不管它又会让零分真的盖掉记分——正解是两份都不采纳，落库仍用服务器的比分与证据。
        givenExistingSnapshot(RICH_SNAPSHOT);
        String bootstrap = "{\"gameStarted\":true,\"gameState\":{\"inning\":1,\"outs\":0},"
                + "\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":0}}}";

        gameService.saveLineupSnapshot(Long.valueOf(GAME_ID), bootstrap);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        JsonNode saved = readJson(written.getValue());
        assertEquals(24, saved.path("playEvents").size(), "证据不能丢：" + saved);
        assertEquals(18, saved.path("teamsData").path("away").path("score").asInt(),
                "记分栏不能被零分盖掉：" + saved);
        assertTrue(saved.has("gameState"), "阵容板自己写的字段要留住：" + saved);
    }

    @Test
    @DisplayName("服务器快照没有比分段时：开局草稿的那份 0 分要从正文里删掉，不是留着")
    void saveLineupSnapshot_dropsBootstrapZeroScoreWhenServerHasNoScoreSection() {
        // 豁免的根据是「阵容板不主张比分」，那就得做到底：服务器没有现值可并时，
        // 留着 payload 的 0 分等于把谁都没写过的比分落库。云端恢复只在 teamsData 存在时
        // 覆盖比分（Vl()），那份假 0 会一路跟到现场页，之后每次自动存档都被得分守门顶回。
        givenExistingSnapshot("{\"playEventsComplete\":true,\"playEvents\":["
                + repeatedEvents(24, 24) + "]}");

        gameService.saveLineupSnapshot(Long.valueOf(GAME_ID),
                "{\"gameStarted\":true,\"gameState\":{\"inning\":1,\"outs\":0},"
                        + "\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":0}}}");

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        JsonNode saved = readJson(written.getValue());
        assertFalse(saved.has("teamsData"), "不主张比分的写入者不该留下一份 0 分：" + saved);
        assertEquals(24, saved.path("playEvents").size(), "证据与证书照旧不能动：" + saved);
        assertTrue(saved.path("playEventsComplete").asBoolean(false), "证书要跟着证据一起留：" + saved);
    }

    @Test
    @DisplayName("阵容板端点自带一套等量证据：豁免不成立，仍要被得分交叉验证拦住")
    void saveLineupSnapshot_payloadCarryingOwnEvidenceIsStillCrossCheckedAgainstScoreboard() {
        // 豁免只看形状不看调用方：两个真实阵容板写入者从不带 playEvents。带了就不是
        // 阵容板的形态，即使从 lineup-snapshot 端点进来，也不能绕过比分对帐——否则把 261
        // 那批真证据等量换成凭空捏造的一批，在这条路上不会被卡住。
        givenExistingSnapshot(RICH_SNAPSHOT);
        String forged = "{\"playEvents\":[" + repeatedEvents(24, 0) + "],"
                + "\"teamsData\":{\"home\":{\"score\":0},\"away\":{\"score\":0}}}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveLineupSnapshot(Long.valueOf(GAME_ID), forged));

        assertEquals(409, ex.getCode(), "条数持平不能当作证据可信的凭据：" + ex.getMessage());
        verify(gameSnapshotRepository, never()).updateSnapshotJson(any(), any(), any());
    }

    @Test
    @DisplayName("整份换掉 playEvents 时不继承服务器替旧证据开的证书")
    void saveLiveSnapshot_replacedEvidenceDoesNotInheritOldCertificate() {
        // 证书说的是「现在这份 playEvents 值不值得信」，不是「这个场次历史上信过」。
        // payload 换了一整套事件却没带证书，落库正文就该没有证书（恢复时维持预设口径），
        // 而不是被 graft 上一纸替旧证据开的可信章。
        givenExistingSnapshot("{\"playEventsComplete\":true,\"playEvents\":["
                + repeatedEvents(24, 24) + "],\"teamsData\":{\"home\":{\"score\":6},\"away\":{\"score\":18}}}");
        String freshEvidence = "{\"playEvents\":[" + repeatedEvents(25, 25) + "],"
                + "\"teamsData\":{\"home\":{\"score\":6},\"away\":{\"score\":18}}}";

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), freshEvidence);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        JsonNode saved = readJson(written.getValue());
        assertEquals(25, saved.path("playEvents").size(), "本次带的事件以本次为准：" + saved);
        assertFalse(saved.has("playEventsComplete"),
                "换了证据就不该继承旧证书：" + saved);
    }

    @Test
    @DisplayName("payload 只带 playEvents 不带 teamsData：不按猜测拦得分，变短也不拦")
    void saveLiveSnapshot_payloadWithoutScoreSkipsScoreGuardOnly() {
        // 把「没提比分」当成「主张 0 分」会误伤；条数那道也已于 #713 拍板放行（见上一例）。
        // 保留本例是因为它钉的是「落库以 payload 为准、没提的比分保持服务器现值」这个形状。
        givenExistingSnapshot(RICH_SNAPSHOT);
        String evidenceOnly = "{\"playEvents\":[" + repeatedEvents(25, 0) + "]}";

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), evidenceOnly);

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), written.capture(), eq(Long.valueOf(USER_ID)));
        JsonNode saved = readJson(written.getValue());
        assertEquals(25, saved.path("playEvents").size(), "本次带的事件以本次为准：" + saved);
        assertEquals(18, saved.path("teamsData").path("away").path("score").asInt(),
                "没提的比分保持服务器现值：" + saved);

        gameService.saveLiveSnapshot(Long.valueOf(GAME_ID), "{\"playEvents\":[" + repeatedEvents(3, 0) + "]}");
        ArgumentCaptor<String> shrunk = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository, Mockito.times(2)).updateSnapshotJson(
                eq(Long.valueOf(GAME_ID)), shrunk.capture(), eq(Long.valueOf(USER_ID)));
        JsonNode shorter = readJson(shrunk.getAllValues().get(1));
        assertEquals(3, shorter.path("playEvents").size(), "变短不再被拦：" + shorter);
        assertEquals(18, shorter.path("teamsData").path("away").path("score").asInt(),
                "没提的比分仍保持服务器现值：" + shorter);
    }

    /** 服务器现有快照正文：#686 后只能从 GameSnapshotRepository 读，不能再塞给 Game 实体。 */
    private void givenExistingSnapshot(String existingJson) {
        when(gameSnapshotRepository.findSnapshotJsonById(Long.valueOf(GAME_ID)))
                .thenReturn(Optional.of(existingJson));
    }

    /** 落库正文解成树：要断言的是「哪个键带着什么值落库」，不是子串。 */
    private static JsonNode readJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        }
        catch (Exception ex) {
            throw new AssertionError("落库正文解不开：" + json, ex);
        }
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
