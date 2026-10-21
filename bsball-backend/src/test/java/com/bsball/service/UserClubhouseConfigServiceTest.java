package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.UserClubhouseConfig;
import com.bsball.repository.UserClubhouseConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserClubhouseConfigServiceTest {
    @Mock
    private UserClubhouseConfigRepository configRepository;

    private UserClubhouseConfigService service;

    @BeforeEach
    void setUp() {
        service = new UserClubhouseConfigService(configRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("无配置时返回默认配置且不在读接口落库")
    void returnsDefaultsWithoutWriting() {
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.empty());

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals(1, config.get("version"));
        assertEquals("v3-fixed", config.get("layout"));
        assertNotNull(config.get("performance"));
        assertEquals(4, ((List<?>) config.get("ranks")).size());
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
    }

    @Test
    @DisplayName("保存时拒绝白名单外的指标")
    void rejectsUnknownMetric() {
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("metricKey", "batting.unknown");
        performance.put("viz", "line");
        Map<String, Object> body = Map.of("performance", performance);

        assertThrows(BusinessException.class, () -> service.save(7L, 1L, body));
    }

    @Test
    @DisplayName("保存时拒绝不兼容的图表类型")
    void rejectsIncompatibleViz() {
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("metricKey", "batting.avg");
        performance.put("viz", "donut");
        Map<String, Object> body = Map.of("performance", performance);

        assertThrows(BusinessException.class, () -> service.save(7L, 1L, body));
    }

    @Test
    @DisplayName("保存时要求四个不重复的排名指标")
    void rejectsDuplicateOrShortRanks() {
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("metricKey", "batting.h");
        performance.put("viz", "bar");
        Map<String, Object> body = Map.of(
                "performance", performance,
                "ranks", List.of(
                        Map.of("metricKey", "batting.avg"),
                        Map.of("metricKey", "batting.avg"),
                        Map.of("metricKey", "batting.obp"),
                        Map.of("metricKey", "batting.rbi")));

        assertThrows(BusinessException.class, () -> service.save(7L, 1L, body));

        Map<String, Object> shortBody = Map.of(
                "performance", performance,
                "ranks", List.of(Map.of("metricKey", "batting.avg")));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, shortBody));

        Map<String, Object> emptyRanks = Map.of(
                "performance", performance,
                "ranks", List.of());
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, emptyRanks));
    }

    @Test
    @DisplayName("合法配置按白名单收敛并通过原子 upsert 保存")
    void savesNormalizedConfig() {
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("metricKey", "batting.h");
        performance.put("viz", "bar");
        Map<String, Object> body = Map.of(
                "layout", "evil-layout",
                "performance", performance,
                "ranks", List.of(
                        Map.of("metricKey", "batting.avg"),
                        Map.of("metricKey", "batting.obp"),
                        Map.of("metricKey", "pitching.era"),
                        Map.of("metricKey", "batting.rbi")));

        Map<String, Object> saved = service.save(7L, 1L, body);

        assertEquals("v3-fixed", saved.get("layout"));
        @SuppressWarnings("unchecked")
        Map<String, Object> savedPerformance = (Map<String, Object>) saved.get("performance");
        assertEquals("batting.h", savedPerformance.get("metricKey"));
        assertEquals("bar", savedPerformance.get("viz"));
        assertEquals(4, ((List<?>) saved.get("ranks")).size());
        assertTrue(saved.containsKey("version"));
        verify(configRepository).upsert(eq(7L), eq(1L), anyString());
    }

    @Test
    @DisplayName("损坏的已存 JSON 回退默认配置且不覆盖写库")
    void invalidStoredJsonFallsBack() {
        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson("{broken");
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals("v3-fixed", config.get("layout"));
        assertEquals(4, ((List<?>) config.get("ranks")).size());
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
    }
}
