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
        // 条数倒退正是撤销/重做会撞到的那一道（#713），所以文案得说出常见成因与出路，
        // 而不是只留一句「已拒绝覆盖」让人去猜是哪份存档没进去。
        assertTrue(ex.getMessage().contains("从云端恢复录入现场"),
                "条数倒退的 409 要给得出路：" + ex.getMessage());
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
    @DisplayName("payload 只带 playEvents 不带 teamsData：不按猜测拦得分，条数一道照旧")
    void saveLiveSnapshot_payloadWithoutScoreSkipsScoreGuardOnly() {
        // 把「没提比分」当成「主张 0 分」会误伤；但「没提比分」绝不等于「可以少事件」。
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

        BusinessException ex = assertThrows(BusinessException.class, () -> gameService.saveLiveSnapshot(
                Long.valueOf(GAME_ID), "{\"playEvents\":[" + repeatedEvents(3, 0) + "]}"));
        assertEquals(409, ex.getCode(),
                "同一形状但事件变短，仍要被条数守门拦住：" + ex.getMessage());
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
