package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Player;
import com.bsball.repository.EventRepository;
import com.bsball.repository.EventTeamRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubhouseServiceTest {
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerTeamRepository playerTeamRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventTeamRepository eventTeamRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private StatsService statsService;
    @Mock
    private UserClubhouseConfigService configService;

    private ClubhouseService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new ClubhouseService(
                playerRepository,
                playerTeamRepository,
                teamRepository,
                eventRepository,
                eventTeamRepository,
                gameRepository,
                statsService,
                configService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("未认领球员的账号访问驾驶舱返回 404")
    void unclaimedUserGets404() {
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getDashboard(99L, null, null));
    }

    @Test
    @DisplayName("已认领但无代表球队时返回默认配置与空数据")
    void claimedPlayerWithoutTeamsReturnsDefaults() {
        CurrentUserHolder.set(7L, 1L);
        Player player = new Player();
        player.setId(10L);
        player.setTenantId(1L);
        player.setName("林嘉豪");
        when(playerRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(player));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(10L))
                .thenReturn(List.of());
        when(teamRepository.findAllById(any())).thenReturn(List.of());
        when(configService.getOrCreate(7L, 1L)).thenReturn(ClubhouseMetricCatalog.defaultConfig());
        Map<String, Object> emptyStats = new LinkedHashMap<>();
        emptyStats.put("batting", new LinkedHashMap<>());
        emptyStats.put("pitching", new LinkedHashMap<>());
        emptyStats.put("fielding", new LinkedHashMap<>());
        when(statsService.getPlayerStats(10L, "BASEBALL")).thenReturn(emptyStats);
        when(statsService.getPlayerGameLog(10L, 3, "BASEBALL")).thenReturn(List.of());

        Map<String, Object> result = service.getDashboard(7L, null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> playerView = (Map<String, Object>) result.get("player");
        assertEquals("林嘉豪", playerView.get("name"));
        assertTrue(((List<?>) result.get("teams")).isEmpty());
        assertNotNull(result.get("catalog"));
        assertEquals(4, ((List<?>) result.get("ranks")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> performance = (Map<String, Object>) result.get("performance");
        assertEquals(false, performance.get("available"));
        assertTrue(((List<?>) result.get("schedule")).isEmpty());
    }
}
