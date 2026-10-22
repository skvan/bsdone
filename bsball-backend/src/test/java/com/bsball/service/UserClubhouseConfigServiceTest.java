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
    @DisplayName("无配置时返回 v2 默认配置且不在读接口落库")
    void returnsDefaultsWithoutWriting() {
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.empty());

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals(2, config.get("version"));
        assertEquals("v3-modular", config.get("layout"));
        assertNotNull(config.get("performance"));
        assertEquals(4, ((List<?>) config.get("ranks")).size());
        assertEquals(8, ((List<?>) config.get("modules")).size());
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
    @DisplayName("保存时拒绝未知或重复模块")
    void rejectsUnknownOrDuplicateModules() {
        Map<String, Object> performance = Map.of("metricKey", "batting.avg", "viz", "line");
        Map<String, Object> unknown = Map.of(
                "performance", performance,
                "ranks", validRanks(),
                "modules", List.of(Map.of("key", "evil", "enabled", true)));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, unknown));

        Map<String, Object> duplicate = Map.of(
                "performance", performance,
                "ranks", validRanks(),
                "modules", List.of(
                        Map.of("key", "ranks", "enabled", true),
                        Map.of("key", "ranks", "enabled", false)));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, duplicate));
    }

    @Test
    @DisplayName("缺失模块补到末尾且保留用户顺序")
    void appendsMissingModulesInDefaultOrder() {
        Map<String, Object> body = Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "modules", List.of(
                        Map.of("key", "evaluation", "enabled", false),
                        Map.of("key", "ranks", "enabled", true)));

        Map<String, Object> saved = service.save(7L, 1L, body);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) saved.get("modules");

        assertEquals(8, modules.size());
        assertEquals("evaluation", modules.get(0).get("key"));
        assertEquals(false, modules.get(0).get("enabled"));
        assertEquals("ranks", modules.get(1).get("key"));
        assertEquals("performance", modules.get(2).get("key"));
        assertTrue(modules.stream().skip(2)
                .allMatch(row -> Boolean.TRUE.equals(row.get("enabled"))));
    }

    @Test
    @DisplayName("允许八个模块全部关闭")
    void allowsAllModulesDisabled() {
        List<Map<String, Object>> modules = ClubhouseModuleCatalog.all().stream()
                .map(def -> Map.<String, Object>of("key", def.key(), "enabled", false))
                .toList();
        Map<String, Object> body = Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "modules", modules);

        Map<String, Object> saved = service.save(7L, 1L, body);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) saved.get("modules");

        assertEquals(8, rows.size());
        assertTrue(rows.stream().noneMatch(row -> Boolean.TRUE.equals(row.get("enabled"))));
    }

    @Test
    @DisplayName("modules 非数组时回退默认模块")
    void nonArrayModulesFallsBackToDefaults() {
        Map<String, Object> body = Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "modules", "not-a-list");

        Map<String, Object> saved = service.save(7L, 1L, body);

        assertEquals(8, ((List<?>) saved.get("modules")).size());
    }

    @Test
    @DisplayName("全部关闭的配置写入后读取仍保持关闭")
    void allDisabledConfigRemainsDisabledAfterReload() throws Exception {
        List<Map<String, Object>> modules = ClubhouseModuleCatalog.all().stream()
                .map(def -> Map.<String, Object>of("key", def.key(), "enabled", false))
                .toList();
        Map<String, Object> saved = service.save(7L, 1L, Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "modules", modules));

        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson(new ObjectMapper().writeValueAsString(saved));
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> loaded = service.getOrCreate(7L, 1L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) loaded.get("modules");

        assertEquals(8, rows.size());
        assertTrue(rows.stream().noneMatch(row -> Boolean.TRUE.equals(row.get("enabled"))));
    }

    @Test
    @DisplayName("模块 enabled 只接受布尔语义")
    void rejectsNonBooleanModuleEnabled() {
        Map<String, Object> body = Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "modules", List.of(Map.of("key", "ranks", "enabled", "not-a-boolean")));

        assertThrows(BusinessException.class, () -> service.save(7L, 1L, body));
    }

    @Test
    @DisplayName("合法配置按白名单收敛并通过原子 upsert 保存")
    void savesNormalizedConfig() {
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("metricKey", "batting.h");
        performance.put("viz", "bar");
        Map<String, Object> body = Map.of(
                "layout", "evil-layout",
                "version", 99,
                "performance", performance,
                "ranks", List.of(
                        Map.of("metricKey", "batting.avg"),
                        Map.of("metricKey", "batting.obp"),
                        Map.of("metricKey", "pitching.era"),
                        Map.of("metricKey", "batting.rbi")));

        Map<String, Object> saved = service.save(7L, 1L, body);

        assertEquals(2, saved.get("version"));
        assertEquals("v3-modular", saved.get("layout"));
        assertEquals(8, ((List<?>) saved.get("modules")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> savedPerformance = (Map<String, Object>) saved.get("performance");
        assertEquals("batting.h", savedPerformance.get("metricKey"));
        assertEquals("bar", savedPerformance.get("viz"));
        assertEquals(4, ((List<?>) saved.get("ranks")).size());
        verify(configRepository).upsert(eq(7L), eq(1L), anyString());
    }

    @Test
    @DisplayName("version 1 已存配置保留指标并补默认模块")
    void migratesVersionOneConfig() {
        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson("""
                {"version":1,"layout":"v3-fixed",
                 "performance":{"metricKey":"batting.h","viz":"bar"},
                 "ranks":[{"metricKey":"batting.avg"},{"metricKey":"batting.obp"},
                          {"metricKey":"pitching.era"},{"metricKey":"batting.rbi"}]}
                """);
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals(2, config.get("version"));
        assertEquals("v3-modular", config.get("layout"));
        @SuppressWarnings("unchecked")
        Map<String, Object> performance = (Map<String, Object>) config.get("performance");
        assertEquals("batting.h", performance.get("metricKey"));
        assertEquals(8, ((List<?>) config.get("modules")).size());
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
    }

    @Test
    @DisplayName("损坏的已存 JSON 回退默认配置且不覆盖写库")
    void invalidStoredJsonFallsBack() {
        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson("{broken");
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals("v3-modular", config.get("layout"));
        assertEquals(8, ((List<?>) config.get("modules")).size());
        assertEquals(4, ((List<?>) config.get("ranks")).size());
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
    }

    private List<Map<String, Object>> validRanks() {
        return List.of(
                Map.of("metricKey", "batting.avg"),
                Map.of("metricKey", "batting.h"),
                Map.of("metricKey", "batting.obp"),
                Map.of("metricKey", "batting.rbi"));
    }
}
