package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSubstitutionEventRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * importGameResult（历史补录／legacy 建赛）的投手失分分解把关测试。
 *
 * <p>导入新建的比赛没有 ER 引擎判定可依附，引擎无从重算，因此约定为：
 * 四栏（R/ER/UER/PENDING）齐备时必须对得上，否则拒绝；任一栏缺失则跳过判定，
 * 以免只送 er/pitchR 的旧导入端被新规则误伤。纯单元测试（Mockito，不启动 Spring）。
 */
@ExtendWith(MockitoExtension.class)
class GameServiceImportPitcherTotalsTest {
    private static final long TENANT_ID = 1L;
    private static final long EVENT_ID = 700L;
    private static final long HOME_ID = 100L;
    private static final long AWAY_ID = 200L;

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

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerStatRepository, gameSubstitutionEventRepository,
                eventRepository, stadiumRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                tenantQueryPolicyService, earnedRunReconstructionService, eventRegistrationService,
                gameFieldingPosStatRepository,
                org.mockito.Mockito.mock(GameFieldingAdjustmentService.class),
                org.mockito.Mockito.mock(GameRevisionAuditService.class));
    }

    /** 只桩读上下文（租户+赛事）；拒绝类用例不会走到建赛写入，多余桩定会被 strict stubs 判失败。 */
    private void stubReadContext() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event e = new Event();
        e.setId(Long.valueOf(EVENT_ID));
        e.setTenantId(Long.valueOf(TENANT_ID));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));
    }

    /** 读上下文 + 允许建赛落库（放行类用例） */
    private void stubWritableContext() {
        stubReadContext();
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** 投手列：isPitcher=1，四栏按需给值（null 代表客户端没送） */
    private static SaveGameResultDTO.StatPart pitcher(Long playerId, Integer r, Integer er, Integer uer, Integer pend) {
        SaveGameResultDTO.StatPart part = new SaveGameResultDTO.StatPart();
        part.setTeamId(Long.valueOf(AWAY_ID));
        part.setPlayerId(playerId);
        part.setIsPitcher(Integer.valueOf(1));
        part.setPitchR(r);
        part.setEr(er);
        part.setUnearnedR(uer);
        part.setPendingR(pend);
        return part;
    }

    private static SaveGameResultDTO dtoWith(SaveGameResultDTO.StatPart... parts) {
        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gamePart = new SaveGameResultDTO.GamePart();
        gamePart.setHomeTeamId(Long.valueOf(HOME_ID));
        gamePart.setAwayTeamId(Long.valueOf(AWAY_ID));
        // 弃权等特殊赛果口径：跳过头号先发/守位与 P-DH 校验，让本组测试只看失分分解把关
        gamePart.setIsSpecialResult(Boolean.TRUE);
        dto.setGame(gamePart);
        List<SaveGameResultDTO.StatPart> stats = new ArrayList<>();
        for (SaveGameResultDTO.StatPart part : parts) {
            stats.add(part);
        }
        dto.setStats(stats);
        return dto;
    }

    private List<GamePlayerStat> savedStats() {
        ArgumentCaptor<GamePlayerStat> captor = ArgumentCaptor.forClass(GamePlayerStat.class);
        verify(gamePlayerStatRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("导入：四栏对得上（R=ER+UER+PENDING）放行，且新增两栏确实落库")
    void consistentSplit_isAcceptedAndPersisted() {
        stubWritableContext();

        gameService.importGameResult(Long.valueOf(EVENT_ID), dtoWith(pitcher(Long.valueOf(3001L), 4, 2, 1, 1)));

        List<GamePlayerStat> saved = savedStats();
        assertEquals(1, saved.size());
        GamePlayerStat stat = saved.get(0);
        assertEquals(Integer.valueOf(4), stat.getPitchR());
        assertEquals(Integer.valueOf(2), stat.getEr());
        assertEquals(Integer.valueOf(1), stat.getUnearnedR());
        assertEquals(Integer.valueOf(1), stat.getPendingR());
    }

    @Test
    @DisplayName("导入：R 与分解对不上（缺 1 分）返回 400，比赛与统计都不落库")
    void inconsistentSplit_isRejectedBeforeAnyWrite() {
        stubReadContext();

        BusinessException ex = assertThrows(BusinessException.class, () -> gameService.importGameResult(
                Long.valueOf(EVENT_ID), dtoWith(pitcher(Long.valueOf(3001L), 4, 2, 1, 0))));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("失分分解不一致"), ex.getMessage());
        verify(gameRepository, never()).save(any(Game.class));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("导入：自责分大于失分返回 400")
    void earnedGreaterThanRuns_isRejected() {
        stubReadContext();

        BusinessException ex = assertThrows(BusinessException.class, () -> gameService.importGameResult(
                Long.valueOf(EVENT_ID), dtoWith(pitcher(Long.valueOf(3002L), 2, 3, 0, 0))));

        assertEquals(400, ex.getCode());
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("导入：非自责分为负值返回 400")
    void negativeUnearned_isRejected() {
        stubReadContext();

        assertThrows(BusinessException.class, () -> gameService.importGameResult(
                Long.valueOf(EVENT_ID), dtoWith(pitcher(Long.valueOf(3003L), 4, 2, -1, 3))));

        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("导入：旧客户端没送 UER/PEND 时跳过判定，且不会把两栏写成 0")
    void legacyClientWithoutSplit_isSkippedAndLeftNull() {
        stubWritableContext();

        gameService.importGameResult(Long.valueOf(EVENT_ID), dtoWith(pitcher(Long.valueOf(3004L), 4, 3, null, null)));

        List<GamePlayerStat> saved = savedStats();
        assertEquals(1, saved.size());
        assertNull(saved.get(0).getUnearnedR());
        assertNull(saved.get(0).getPendingR());
        assertEquals(Integer.valueOf(4), saved.get(0).getPitchR());
    }

    @Test
    @DisplayName("导入：没送失分合计（pitchR 缺栏）时跳过判定")
    void missingRunTotal_isSkipped() {
        stubWritableContext();

        gameService.importGameResult(Long.valueOf(EVENT_ID), dtoWith(pitcher(Long.valueOf(3005L), null, 1, 0, 0)));

        assertEquals(1, savedStats().size());
    }

    @Test
    @DisplayName("导入：打者列不受投手失分把关影响")
    void battingRow_isNotChecked() {
        stubWritableContext();
        SaveGameResultDTO.StatPart batter = new SaveGameResultDTO.StatPart();
        batter.setTeamId(Long.valueOf(HOME_ID));
        batter.setPlayerId(Long.valueOf(4001L));
        batter.setIsPitcher(Integer.valueOf(0));
        batter.setPitchR(Integer.valueOf(9));
        batter.setEr(Integer.valueOf(0));

        gameService.importGameResult(Long.valueOf(EVENT_ID), dtoWith(batter));

        assertEquals(1, savedStats().size());
    }

    @Test
    @DisplayName("导入：同 payload 内混一行不一致投手 → 整份拒绝（不做部分落地）")
    void oneBadPitcherRow_rejectsWholeImport() {
        stubReadContext();

        assertThrows(BusinessException.class, () -> gameService.importGameResult(Long.valueOf(EVENT_ID),
                dtoWith(pitcher(Long.valueOf(3006L), 1, 1, 0, 0),
                        pitcher(Long.valueOf(3007L), 5, 1, 1, 1))));

        verify(gameRepository, never()).save(any(Game.class));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }
}
