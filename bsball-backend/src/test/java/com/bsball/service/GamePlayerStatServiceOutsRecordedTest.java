package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
@DisplayName("GamePlayerStatService：outs_recorded 随 IP 派生")
class GamePlayerStatServiceOutsRecordedTest {

    private static final long GAME_ID = 245L;
    private static final long ROW_ID = 5184L;

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
    @DisplayName("create：按 ip 写入 outs_recorded")
    void create_derivesOutsFromIp() {
        GamePlayerStat row = new GamePlayerStat();
        row.setGameId(GAME_ID);
        row.setTeamId(48L);
        row.setPlayerId(1324L);
        row.setIp(Double.valueOf(1.2D));

        service.create(row);

        assertEquals(Integer.valueOf(5), row.getOutsRecorded());
    }

    @Test
    @DisplayName("update：按新 ip 重算 outs_recorded")
    void update_derivesOutsFromIp() {
        GamePlayerStat existing = row(Double.valueOf(1.0D), Integer.valueOf(3));
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(existing));

        GamePlayerStat incoming = row(Double.valueOf(2.1D), Integer.valueOf(0));
        incoming.setGameId(GAME_ID);
        service.update(ROW_ID, incoming);

        assertEquals(Integer.valueOf(7), incoming.getOutsRecorded());
    }

    @Test
    @DisplayName("update：载荷缺 ip 时保留既有 outs_recorded")
    void update_missingIp_preservesOuts() {
        GamePlayerStat existing = row(Double.valueOf(3.0D), Integer.valueOf(9));
        when(gamePlayerStatRepository.findById(ROW_ID)).thenReturn(Optional.of(existing));

        GamePlayerStat incoming = row(null, Integer.valueOf(0));
        incoming.setGameId(GAME_ID);
        service.update(ROW_ID, incoming);

        assertEquals(Integer.valueOf(9), incoming.getOutsRecorded());
    }

    private GamePlayerStat row(Double ip, Integer outsRecorded) {
        GamePlayerStat row = new GamePlayerStat();
        row.setId(ROW_ID);
        row.setGameId(GAME_ID);
        row.setTeamId(48L);
        row.setPlayerId(1324L);
        row.setIsPitcher(Integer.valueOf(1));
        row.setIp(ip);
        row.setOutsRecorded(outsRecorded);
        return row;
    }
}
