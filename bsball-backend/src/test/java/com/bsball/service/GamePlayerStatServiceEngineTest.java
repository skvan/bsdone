package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamePlayerStatService：引擎场次 ER 四字段只读")
class GamePlayerStatServiceEngineTest {

    private static final long GAME_ID = 245L;
    private static final long ROW_ID = 5184L;
    private static final long PLAYER_ID = 1324L;

    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private EventRegistrationService eventRegistrationService;
    @Mock
    private EarnedRunReconstructionService earnedRunReconstructionService;

    private GamePlayerStatService service;

    @BeforeEach
    void setUp() {
        service = new GamePlayerStatService(gamePlayerStatRepository, gameRepository,
                eventRegistrationService, earnedRunReconstructionService);
        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("引擎场次 update：保留既有 R/ER/UER/PENDING")
    void update_engineManaged_preservesEngineTotals() {
        GamePlayerStat existing = row(3, 5, 1, 0);
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(existing));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);

        GamePlayerStat incoming = row(99, 88, 9, 9);
        service.update(ROW_ID, incoming);

        assertEquals(Integer.valueOf(5), incoming.getPitchR());
        assertEquals(Integer.valueOf(3), incoming.getEr());
        assertEquals(Integer.valueOf(1), incoming.getUnearnedR());
        assertEquals(Integer.valueOf(0), incoming.getPendingR());
    }

    @Test
    @DisplayName("引擎场次 update：拒绝改变投手行身份")
    void update_engineManaged_rejectsIdentityChange() {
        GamePlayerStat existing = row(3, 5, 1, 0);
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(existing));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);

        GamePlayerStat incoming = row(99, 88, 9, 9);
        incoming.setIsPitcher(Integer.valueOf(0));

        assertThrows(BusinessException.class, () -> service.update(ROW_ID, incoming));
    }

    @Test
    @DisplayName("引擎场次 update：拒绝把统计行移到另一场比赛")
    void update_engineManaged_rejectsGameMove() {
        GamePlayerStat existing = row(3, 5, 1, 0);
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(existing));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);

        GamePlayerStat incoming = row(3, 5, 1, 0);
        incoming.setGameId(Long.valueOf(999L));

        assertThrows(BusinessException.class, () -> service.update(ROW_ID, incoming));
    }

    @Test
    @DisplayName("legacy 场次 update：允许手工修正 R/ER/UER/PENDING")
    void update_legacy_writesIncomingTotals() {
        GamePlayerStat existing = row(3, 5, 1, 0);
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(existing));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(false);

        GamePlayerStat incoming = row(4, 6, 0, 0);
        service.update(ROW_ID, incoming);

        assertEquals(Integer.valueOf(6), incoming.getPitchR());
        assertEquals(Integer.valueOf(4), incoming.getEr());
    }

    @Test
    @DisplayName("引擎场次不能删除仍有数据的投手行")
    void delete_engineManagedPitcherWithData_rejected() {
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(row(3, 5, 1, 0)));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(ROW_ID));
        verify(gamePlayerStatRepository, never()).deleteById(ROW_ID);
    }

    @Test
    @DisplayName("引擎场次允许删除全零且无自责分判定的投手行")
    void delete_engineManagedZeroPitcher_allowed() {
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(row(0, 0, 0, 0)));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);
        when(earnedRunReconstructionService.hasResponsiblePitcherDecisions(GAME_ID, PLAYER_ID))
                .thenReturn(false);

        service.delete(ROW_ID);

        verify(gamePlayerStatRepository).deleteById(ROW_ID);
    }

    @Test
    @DisplayName("引擎场次全零投手行仅残留隐藏 pitchPa 时允许删除")
    void delete_engineManagedZeroPitcherWithPitchPaOnly_allowed() {
        GamePlayerStat zero = row(0, 0, 0, 0);
        zero.setPitchPa(Integer.valueOf(1));
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(zero));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);
        when(earnedRunReconstructionService.hasResponsiblePitcherDecisions(GAME_ID, PLAYER_ID))
                .thenReturn(false);

        service.delete(ROW_ID);

        verify(gamePlayerStatRepository).deleteById(ROW_ID);
    }

    @Test
    @DisplayName("引擎场次全零投手行若仍有自责分判定则拒绝删除")
    void delete_engineManagedZeroPitcherWithDecisions_rejected() {
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(row(0, 0, 0, 0)));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);
        when(earnedRunReconstructionService.hasResponsiblePitcherDecisions(GAME_ID, PLAYER_ID))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(ROW_ID));
        verify(gamePlayerStatRepository, never()).deleteById(ROW_ID);
    }

    @Test
    @DisplayName("legacy 场次投手行不受全零守卫影响")
    void delete_legacyPitcherWithData_allowed() {
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(row(3, 5, 1, 0)));
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(false);

        service.delete(ROW_ID);

        verify(gamePlayerStatRepository).deleteById(ROW_ID);
    }

    @Test
    @DisplayName("create 忽略请求里的 id，避免 merge 绕过守卫")
    void create_ignoresIncomingId() {
        GamePlayerStat incoming = row(9, 9, 9, 9);
        incoming.setId(Long.valueOf(8888L));

        GamePlayerStat created = service.create(incoming);

        org.junit.jupiter.api.Assertions.assertNull(created.getId());
    }

    @Test
    @DisplayName("引擎场次 create：ER 四字段归零，由引擎后续写入")
    void create_engineManaged_zeroesEngineTotals() {
        when(earnedRunReconstructionService.hasEngineData(GAME_ID)).thenReturn(true);

        GamePlayerStat created = service.create(row(9, 9, 9, 9));

        assertEquals(Integer.valueOf(0), created.getPitchR());
        assertEquals(Integer.valueOf(0), created.getEr());
        assertEquals(Integer.valueOf(0), created.getUnearnedR());
        assertEquals(Integer.valueOf(0), created.getPendingR());
        verify(gamePlayerStatRepository).save(created);
    }

    private static GamePlayerStat row(int er, int pitchR, int unearnedR, int pendingR) {
        GamePlayerStat row = new GamePlayerStat();
        row.setId(Long.valueOf(ROW_ID));
        row.setGameId(Long.valueOf(GAME_ID));
        row.setTeamId(Long.valueOf(57L));
        row.setPlayerId(Long.valueOf(PLAYER_ID));
        row.setIsPitcher(Integer.valueOf(1));
        row.setEr(Integer.valueOf(er));
        row.setPitchR(Integer.valueOf(pitchR));
        row.setUnearnedR(Integer.valueOf(unearnedR));
        row.setPendingR(Integer.valueOf(pendingR));
        return row;
    }
}
