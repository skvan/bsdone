package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @DisplayName("无配置时返回 v4 默认画布且不在读接口落库")
    void returnsDefaultsWithoutWriting() {
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.empty());

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals(4, config.get("version"));
        assertEquals("v4-board", config.get("layout"));
        assertNotNull(config.get("performance"));
        assertEquals(4, ((List<?>) config.get("ranks")).size());
        assertEquals(9, ((List<?>) config.get("modules")).size());
        Map<String, Object> ranks = moduleRow(config, "ranks");
        assertEquals(0, ranks.get("x"));
        assertEquals(0, ranks.get("y"));
        assertEquals(1216, ranks.get("width"));
        assertEquals(220, ranks.get("height"));
        Map<String, Object> performance = moduleRow(config, "performance");
        assertEquals(1468, performance.get("y"));
        assertEquals(800, performance.get("width"));
        assertEquals(500, performance.get("height"));
        Map<String, Object> teams = moduleRow(config, "teams");
        assertEquals(816, teams.get("x"));
        assertEquals(1468, teams.get("y"));
        assertEquals(400, teams.get("width"));
        assertEquals(300, teams.get("height"));
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) config.get("milestones");
        assertEquals(ClubhouseMilestoneCatalog.defaultKeys(), milestones.get("metricKeys"));
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

        assertEquals(9, modules.size());
        assertEquals("evaluation", modules.get(0).get("key"));
        assertEquals(false, modules.get(0).get("enabled"));
        assertEquals("ranks", modules.get(1).get("key"));
        assertEquals("milestones", modules.get(2).get("key"));
        assertEquals("performance", modules.get(3).get("key"));
        assertTrue(modules.stream().skip(2)
                .allMatch(row -> Boolean.TRUE.equals(row.get("enabled"))));
    }

    @Test
    @DisplayName("允许九个模块全部关闭")
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

        assertEquals(9, rows.size());
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

        assertEquals("v4-board", saved.get("layout"));
        assertEquals(9, ((List<?>) saved.get("modules")).size());
    }

    @Test
    @DisplayName("全部关闭的配置写入后读取仍保持关闭")
    void allDisabledConfigRemainsDisabledAfterReload() throws Exception {
        List<Map<String, Object>> modules = ClubhouseModuleCatalog.all().stream()
                .map(def -> module(def.key(), false, 0, 0, def.defaultWidth(), def.defaultHeight()))
                .toList();
        Map<String, Object> saved = service.save(7L, 1L, boardBody(modules));

        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson(new ObjectMapper().writeValueAsString(saved));
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> loaded = service.getOrCreate(7L, 1L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) loaded.get("modules");

        assertEquals(9, rows.size());
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
    @DisplayName("里程碑默认 6 项且可保存 1–6 项")
    void savesMilestoneMetricKeys() {
        Map<String, Object> saved = service.save(7L, 1L, Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "milestones", Map.of("metricKeys", List.of(
                        "batting.h", "pitching.ip", "pitching.np"))));

        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) saved.get("milestones");
        assertEquals(List.of("batting.h", "pitching.ip", "pitching.np"),
                milestones.get("metricKeys"));
    }

    @Test
    @DisplayName("里程碑指标拒绝 7 项、未知、重复和非数组")
    void rejectsInvalidMilestoneMetricKeys() {
        Map<String, Object> base = Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks());

        Map<String, Object> tooMany = new LinkedHashMap<>(base);
        tooMany.put("milestones", Map.of("metricKeys", List.of(
                "batting.h", "batting.hr", "batting.rbi", "batting.r",
                "batting.sb", "batting.gp", "pitching.ip")));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, tooMany));

        Map<String, Object> unknown = new LinkedHashMap<>(base);
        unknown.put("milestones", Map.of("metricKeys", List.of("batting.unknown")));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, unknown));

        Map<String, Object> duplicate = new LinkedHashMap<>(base);
        duplicate.put("milestones", Map.of("metricKeys", List.of("batting.h", "batting.h")));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, duplicate));

        Map<String, Object> notArray = new LinkedHashMap<>(base);
        notArray.put("milestones", Map.of("metricKeys", "batting.h"));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L, notArray));
    }

    @Test
    @DisplayName("里程碑指标缺失或为空时回退默认 6 项")
    void missingMilestoneKeysFallsBackToDefaults() {
        Map<String, Object> saved = service.save(7L, 1L, Map.of(
                "performance", Map.of("metricKey", "batting.avg", "viz", "line"),
                "ranks", validRanks(),
                "milestones", Map.of("metricKeys", List.of())));

        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) saved.get("milestones");
        assertEquals(ClubhouseMilestoneCatalog.defaultKeys(), milestones.get("metricKeys"));
        assertEquals(6, ((List<?>) milestones.get("metricKeys")).size());
    }

    @Test
    @DisplayName("v4 保存模块几何并在回读时保留")
    void savesAndReloadsBoardGeometry() throws Exception {
        Map<String, Object> saved = service.save(7L, 1L, boardBody(List.of(
                module("performance", true, 0, 16, 800, 460),
                module("teams", true, 816, 16, 400, 300))));

        Map<String, Object> performance = moduleRow(saved, "performance");
        assertEquals(0, performance.get("x"));
        assertEquals(16, performance.get("y"));
        assertEquals(800, performance.get("width"));
        assertEquals(460, performance.get("height"));

        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson(new ObjectMapper().writeValueAsString(saved));
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> loaded = service.getOrCreate(7L, 1L);
        assertEquals(4, loaded.get("version"));
        assertEquals("v4-board", loaded.get("layout"));
        Map<String, Object> loadedPerformance = moduleRow(loaded, "performance");
        assertEquals(0, loadedPerformance.get("x"));
        assertEquals(16, loadedPerformance.get("y"));
        assertEquals(800, loadedPerformance.get("width"));
        assertEquals(460, loadedPerformance.get("height"));
    }

    @Test
    @DisplayName("v4 模块几何缺省时补目录默认值")
    void missingModuleGeometryFallsBackToDefaults() {
        Map<String, Object> saved = service.save(7L, 1L,
                boardBody(List.of(Map.of("key", "performance", "enabled", true))));

        Map<String, Object> performance = moduleRow(saved, "performance");
        assertEquals(0, performance.get("x"));
        assertEquals(0, performance.get("y"));
        assertEquals(800, performance.get("width"));
        assertEquals(500, performance.get("height"));
        assertEquals(9, ((List<?>) saved.get("modules")).size());
    }

    @Test
    @DisplayName("v4 拒绝越界、负坐标、非整数、过小过大尺寸与缺字段")
    void rejectsInvalidBoardGeometry() {
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, -1, 0, 800, 460)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, 0, 12001, 800, 460)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, 0, 0, 359, 460)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, 0, 0, 1217, 460)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, 0, 0, 800, 319)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, 0, 0, 800, 1601)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(module("performance", true, 600, 0, 800, 460)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(rawModule("performance", 0, 0, 800.5, 460)))));
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(rawModule("performance", 0, 0, 800, "460")))));

        LinkedHashMap<String, Object> partial = new LinkedHashMap<>();
        partial.put("key", "performance");
        partial.put("enabled", true);
        partial.put("x", 0);
        partial.put("width", 800);
        partial.put("height", 460);
        assertThrows(BusinessException.class, () -> service.save(7L, 1L,
                boardBody(List.of(partial))));
    }

    @Test
    @DisplayName("启用模块重叠时后出现模块下移，输出无重叠")
    void resolvesOverlappingEnabledModules() {
        Map<String, Object> saved = service.save(7L, 1L, boardBody(List.of(
                module("performance", true, 0, 0, 800, 460),
                module("teams", true, 0, 0, 400, 300))));

        Map<String, Object> teams = moduleRow(saved, "teams");
        assertEquals(0, teams.get("x"));
        assertEquals(476, teams.get("y"));
        assertNoOverlap(saved);
    }

    @Test
    @DisplayName("禁用模块保留几何且不参与重叠检测")
    void ignoresOverlapWithDisabledModules() {
        Map<String, Object> saved = service.save(7L, 1L, boardBody(List.of(
                module("teams", false, 0, 0, 400, 300),
                module("performance", true, 0, 0, 800, 460))));

        Map<String, Object> teams = moduleRow(saved, "teams");
        assertEquals(false, teams.get("enabled"));
        assertEquals(0, teams.get("x"));
        assertEquals(0, teams.get("y"));
        assertEquals(400, teams.get("width"));
        assertEquals(300, teams.get("height"));
        Map<String, Object> performance = moduleRow(saved, "performance");
        assertEquals(0, performance.get("x"));
        assertEquals(0, performance.get("y"));
    }

    @Test
    @DisplayName("v4 缺失模块追加到所有模块底部且不重叠")
    void appendsMissingBoardModulesBelowExisting() {
        Map<String, Object> saved = service.save(7L, 1L, boardBody(List.of(
                module("ranks", true, 0, 0, 1216, 220))));

        assertEquals(9, ((List<?>) saved.get("modules")).size());
        assertEquals(0, moduleRow(saved, "ranks").get("y"));
        assertEquals(236, moduleRow(saved, "evaluation").get("y"));
        assertEquals(952, moduleRow(saved, "milestones").get("y"));
        assertEquals(1468, moduleRow(saved, "performance").get("y"));
        assertEquals(1984, moduleRow(saved, "teams").get("y"));
        assertNoOverlap(saved);
    }

    @Test
    @DisplayName("v3 配置读取迁移为 v4 自由画布且不写库")
    void migratesVersionThreeToBoard() {
        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson("""
                {"version":3,"layout":"v3-modular",
                 "modules":[{"key":"performance","enabled":true,"width":8,"height":22},
                            {"key":"teams","enabled":true,"width":4,"height":0}],
                 "performance":{"metricKey":"batting.avg","viz":"line"},
                 "ranks":[{"metricKey":"batting.avg"},{"metricKey":"batting.h"},
                          {"metricKey":"batting.obp"},{"metricKey":"batting.rbi"}]}
                """);
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals(4, config.get("version"));
        assertEquals("v4-board", config.get("layout"));
        Map<String, Object> performance = moduleRow(config, "performance");
        assertEquals(805, performance.get("width"));
        assertEquals(320, performance.get("height"));
        assertEquals(0, performance.get("x"));
        assertEquals(0, performance.get("y"));
        Map<String, Object> teams = moduleRow(config, "teams");
        assertEquals(395, teams.get("width"));
        assertEquals(300, teams.get("height"));
        assertEquals(821, teams.get("x"));
        assertEquals(0, teams.get("y"));
        assertEquals(9, ((List<?>) config.get("modules")).size());
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
    }

    @Test
    @DisplayName("version 2 已存配置读取时补齐 v4 默认几何且不写库")
    void migratesVersionTwoModuleSizes() {
        UserClubhouseConfig entity = new UserClubhouseConfig();
        entity.setConfigJson("""
                {"version":2,"layout":"v3-modular",
                 "modules":[{"key":"performance","enabled":true},{"key":"teams","enabled":false}],
                 "performance":{"metricKey":"batting.avg","viz":"line"},
                 "ranks":[{"metricKey":"batting.avg"},{"metricKey":"batting.h"},
                          {"metricKey":"batting.obp"},{"metricKey":"batting.rbi"}]}
                """);
        when(configRepository.findFirstByUserIdAndTenantIdAndDeletedAtIsNull(7L, 1L))
                .thenReturn(Optional.of(entity));

        Map<String, Object> config = service.getOrCreate(7L, 1L);

        assertEquals(4, config.get("version"));
        Map<String, Object> performance = moduleRow(config, "performance");
        assertEquals(800, performance.get("width"));
        assertEquals(500, performance.get("height"));
        assertEquals(0, performance.get("y"));
        Map<String, Object> teams = moduleRow(config, "teams");
        assertEquals(400, teams.get("width"));
        assertEquals(300, teams.get("height"));
        assertEquals(816, teams.get("x"));
        assertEquals(false, teams.get("enabled"));
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
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

        assertEquals(4, saved.get("version"));
        assertEquals("v4-board", saved.get("layout"));
        assertEquals(9, ((List<?>) saved.get("modules")).size());
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

        assertEquals(4, config.get("version"));
        assertEquals("v4-board", config.get("layout"));
        @SuppressWarnings("unchecked")
        Map<String, Object> performance = (Map<String, Object>) config.get("performance");
        assertEquals("batting.h", performance.get("metricKey"));
        assertEquals(9, ((List<?>) config.get("modules")).size());
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

        assertEquals("v4-board", config.get("layout"));
        assertEquals(9, ((List<?>) config.get("modules")).size());
        assertEquals(4, ((List<?>) config.get("ranks")).size());
        verify(configRepository, never()).save(any(UserClubhouseConfig.class));
    }

    private Map<String, Object> boardBody(List<Map<String, Object>> modules) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", 4);
        body.put("layout", "v4-board");
        body.put("performance", Map.of("metricKey", "batting.avg", "viz", "line"));
        body.put("ranks", validRanks());
        body.put("modules", modules);
        return body;
    }

    private Map<String, Object> module(String key, boolean enabled,
            int x, int y, int width, int height) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("enabled", enabled);
        row.put("x", x);
        row.put("y", y);
        row.put("width", width);
        row.put("height", height);
        return row;
    }

    private Map<String, Object> rawModule(String key, Object x, Object y, Object width, Object height) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("enabled", true);
        row.put("x", x);
        row.put("y", y);
        row.put("width", width);
        row.put("height", height);
        return row;
    }

    private void assertNoOverlap(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) config.get("modules");
        List<ClubhouseBoardLayout.ModulePlacement> enabled = modules.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("enabled")))
                .map(row -> new ClubhouseBoardLayout.ModulePlacement(
                        (String) row.get("key"), true,
                        (Integer) row.get("x"), (Integer) row.get("y"),
                        (Integer) row.get("width"), (Integer) row.get("height")))
                .toList();
        for (int i = 0; i < enabled.size(); i++) {
            for (int j = i + 1; j < enabled.size(); j++) {
                assertFalse(ClubhouseBoardLayout.overlaps(enabled.get(i), enabled.get(j)));
            }
        }
    }

    private Map<String, Object> moduleRow(Map<String, Object> config, String key) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) config.get("modules");
        return modules.stream()
                .filter(row -> key.equals(row.get("key")))
                .findFirst()
                .orElseThrow();
    }

    private List<Map<String, Object>> validRanks() {
        return List.of(
                Map.of("metricKey", "batting.avg"),
                Map.of("metricKey", "batting.h"),
                Map.of("metricKey", "batting.obp"),
                Map.of("metricKey", "batting.rbi"));
    }
}
