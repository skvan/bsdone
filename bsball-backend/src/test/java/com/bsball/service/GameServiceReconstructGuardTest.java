package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EarnedRunHalfInningRequest;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 半局重建的完整性守门。线上实测三场就是这么坏的：
 * game 261（判定 23 笔、记分 24 分，3局下 缺的那一分已被空快照抹掉证据）、
 * game 235（投手 1002 库里 R4/ER4，引擎只有 1/1）、game 248（投手 1042 库 R4/ER3，引擎 3/2），
 * 后两场的共同点是「记分栏走结果录入、判定只覆盖部分半局」——此时任何一次半局重建都会拿
 * 残缺分母绝对重算全部投手的 R/ER，并把相关投手的人工覆写作废。
 * 守门只认一个独立证人：重建后全场判定数不得低于记分栏总分。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService 半局重建：残缺分母不得覆写已落账的投手层")
class GameServiceReconstructGuardTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;
    private static final long GAME_ID = 235L;
    private static final long EVENT_ID = 17L;
    private static final long AWAY_TEAM_ID = 40L;
    private static final long HOME_TEAM_ID = 41L;

    @Mock
    private GameRepository gameRepository;
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
                // 重建守门不碰快照正文，只需满足构造器（Issue #686 后新增的第 15 参）
                Mockito.mock(com.bsball.repository.GameSnapshotRepository.class));
        CurrentUserHolder.set(Long.valueOf(USER_ID), Long.valueOf(TENANT_ID));

        game = new Game();
        game.setId(Long.valueOf(GAME_ID));
        game.setTenantId(Long.valueOf(TENANT_ID));
        game.setEventId(Long.valueOf(EVENT_ID));
        game.setAwayTeamId(Long.valueOf(AWAY_TEAM_ID));
        game.setHomeTeamId(Long.valueOf(HOME_TEAM_ID));
        game.setAwayScore(Integer.valueOf(2));
        game.setHomeScore(Integer.valueOf(4));   // 记分栏合计 6 分，是守门用的独立证人
        Event event = new Event();
        event.setId(Long.valueOf(EVENT_ID));
        event.setTenantId(Long.valueOf(TENANT_ID));

        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(Long.valueOf(TENANT_ID));
        when(gameRepository.findById(Long.valueOf(GAME_ID))).thenReturn(Optional.of(game));
        when(eventRepository.findById(Long.valueOf(EVENT_ID))).thenReturn(Optional.of(event));
        lenient().when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("game 235/248 形状：判定只覆盖 1 分却要对记分 6 分做全场重算 → 409 且不动数据")
    void reconstructEarnedRuns_rejectsWithPartialDenominatorAgainstScoreboard() {
        givenDecisions("top", 1, 0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("top", 1, null)));

        assertEquals(409, ex.getCode());
        verify(earnedRunReconstructionService, never()).replaceHalfInning(
                anyLong(), anyLong(), anyInt(), anyString(), anyLong(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("本次推送补齐分母（重建后判定数 = 记分栏总分）→ 放行")
    void reconstructEarnedRuns_allowsWhenRebuildCoversScoreboard() {
        givenDecisions("bottom", 6, 2);

        gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("bottom", 2, null));

        verify(earnedRunReconstructionService).replaceHalfInning(
                eq(Long.valueOf(TENANT_ID)), eq(Long.valueOf(GAME_ID)), eq(3), eq("bottom"),
                eq(Long.valueOf(USER_ID)), any(), eq(Long.valueOf(AWAY_TEAM_ID)),
                eq(true));   // 不送 halfComplete 的旧客户端等同「半局已打完」
    }

    @Test
    @DisplayName("本半局既有判定要先从全场扣掉，不能双计（6-2+1=5 < 6 → 拒）")
    void reconstructEarnedRuns_subtractsReplacedHalfInsteadOfDoubleCounting() {
        givenDecisions("bottom", 6, 2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("bottom", 1, null)));

        assertEquals(409, ex.getCode());
        // 信息里要带上算出来的判定数，记录员才能核对自己少推了哪一分
        assertTrue(ex.getMessage().contains(" 5 "), "应报「重建后只有 5 笔」：" + ex.getMessage());
    }

    @Test
    @DisplayName("显式 confirmPartialRebuild=true 时放行，但留下警告供事后复核")
    void reconstructEarnedRuns_allowsExplicitPartialConfirmation() {
        givenDecisions("top", 1, 0);

        gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("top", 1, Boolean.TRUE));

        verify(earnedRunReconstructionService).replaceHalfInning(
                anyLong(), anyLong(), anyInt(), anyString(), anyLong(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("尚未记分的场次（刚建场/第一局）完全不设限")
    void reconstructEarnedRuns_skipsGuardBeforeAnyRun() {
        game.setAwayScore(Integer.valueOf(0));
        game.setHomeScore(Integer.valueOf(0));

        gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("top", 0, null));

        verify(earnedRunReconstructionService).replaceHalfInning(
                anyLong(), anyLong(), anyInt(), anyString(), anyLong(), any(), any(), anyBoolean());
        verify(earnedRunReconstructionService, never()).countLiveDecisions(anyLong(), anyLong());
    }

    @Test
    @DisplayName("推上来的整半局一棒都没有：只在该半局本来有得分时被拒（清空零分半局属正常）")
    void reconstructEarnedRuns_rejectsEmptyPushWhileScoreboardHasRuns() {
        // 该半局本来有 2 笔判定，推成空列表后全场只剩 4 笔，而记分栏仍是 6 分
        givenDecisions("top", 6, 2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("top", 0, null)));

        assertEquals(409, ex.getCode());

        // 同一半局本来就没得分（6 笔判定全部在其他半局）时，清空重建不该报错
        givenDecisions("bottom", 6, 0);
        gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID), request("bottom", 0, null));
        verify(earnedRunReconstructionService).replaceHalfInning(
                anyLong(), anyLong(), anyInt(), anyString(), anyLong(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("半局未完成（halfComplete=false）：不做完整性守门，且把 false 透传给服务")
    void reconstructEarnedRuns_halfIncompleteSkipsGuardAndPassesFlagThrough() {
        // 记分 6 分、判定表空着（不 stub 即为 0 笔）：完整重建会被守门拒掉，
        // 但预览不写库，因此不得碰守门
        gameService.reconstructEarnedRuns(Long.valueOf(GAME_ID),
                request("top", 1, null, Boolean.FALSE));

        verify(earnedRunReconstructionService, never()).countLiveDecisions(anyLong(), anyLong());
        verify(earnedRunReconstructionService).replaceHalfInning(
                eq(Long.valueOf(TENANT_ID)), eq(Long.valueOf(GAME_ID)), eq(3), eq("top"),
                eq(Long.valueOf(USER_ID)), any(), eq(Long.valueOf(HOME_TEAM_ID)),
                eq(false));
    }

    private void givenDecisions(String half, int liveTotal, int liveInThisHalf) {
        when(earnedRunReconstructionService.countLiveDecisions(
                Long.valueOf(TENANT_ID), Long.valueOf(GAME_ID))).thenReturn(liveTotal);
        when(earnedRunReconstructionService.countLiveDecisionsOfHalf(
                Long.valueOf(TENANT_ID), Long.valueOf(GAME_ID), 3, half)).thenReturn(liveInThisHalf);
    }

    /** 组装一次「第 3 局某半局、有 runs 个得分跑者」的整半局重建请求。 */
    private static EarnedRunHalfInningRequest request(String half, int runs, Boolean confirm) {
        return request(half, runs, confirm, null);
    }

    private static EarnedRunHalfInningRequest request(
            String half, int runs, Boolean confirm, Boolean halfComplete) {
        EarnedRunHalfInningRequest request = new EarnedRunHalfInningRequest();
        request.setInning(Integer.valueOf(3));
        request.setHalf(half);
        List<EarnedRunPlay> plays = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            plays.add(new EarnedRunPlay(Long.valueOf(100L + i), i + 1, 0, 0, false,
                    List.of(ScoringRunner.normal(Long.valueOf(500L + i), Long.valueOf(700L)))));
        }
        if (plays.isEmpty()) {
            plays.add(new EarnedRunPlay(Long.valueOf(100L), 1, 1, 1, false, List.of()));
        }
        request.setPlays(plays);
        request.setConfirmPartialRebuild(confirm);
        request.setHalfComplete(halfComplete);
        return request;
    }
}
