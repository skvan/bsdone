/*
 * 赛事级特殊规则开关（Issue #275，2026-10-24 口径调整）：EventSpecialRuleService 单元测试。
 *
 * 覆盖：默认值兜底（无记录 / 部分键 / null eventId 早退零查库）、写入并回读生效值
 *      （upsert 新增 + 更新既有行、审计字段）、非法键名 / 空映射 / null 值拒绝（零写入）。
 * 写权限由 EventService.create / update（business:event:edit）入口负责；本服务不重复做租户守卫。
 * 纯单元测试：不启动 Spring 容器、不连接数据库。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.EventSpecialRule;
import com.bsball.repository.EventSpecialRuleRepository;
import java.util.ArrayList;
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
@DisplayName("EventSpecialRuleService：赛事特殊规则读取 / 写入（#275）")
class EventSpecialRuleServiceTest {

    private static final long EVENT_ID = 5L;
    private static final long TENANT_ID = 10L;
    private static final long USER_ID = 9L;

    @Mock
    private EventSpecialRuleRepository eventSpecialRuleRepository;

    private EventSpecialRuleService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        CurrentUserHolder.set(USER_ID, TENANT_ID);
        service = new EventSpecialRuleService(eventSpecialRuleRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("无记录时返回默认值：extraBatter=false、forceHalf=true")
    void getEffective_noRows_returnsDefaults() {
        when(eventSpecialRuleRepository.findByEventId(EVENT_ID)).thenReturn(List.of());

        Map<String, Boolean> effective = service.getEffective(EVENT_ID);

        assertEquals(2, effective.size());
        assertEquals(Boolean.FALSE, effective.get("extraBatter"));
        assertEquals(Boolean.TRUE, effective.get("forceHalf"));
    }

    @Test
    @DisplayName("eventId 为空：直接返回默认值，零仓储交互")
    void getEffective_nullEventId_returnsDefaultsWithoutQuery() {
        Map<String, Boolean> effective = service.getEffective(null);

        assertEquals(Boolean.FALSE, effective.get("extraBatter"));
        assertEquals(Boolean.TRUE, effective.get("forceHalf"));
        verifyNoInteractions(eventSpecialRuleRepository);
    }

    @Test
    @DisplayName("仅有 extra_batter 行时 force_half 回默认开启；未知键忽略")
    void getEffective_partialRows_fallbackToDefaults() {
        when(eventSpecialRuleRepository.findByEventId(EVENT_ID)).thenReturn(List.of(
                rule(EventSpecialRuleService.RULE_EXTRA_BATTER, true),
                rule("future_unknown_key", true)));

        Map<String, Boolean> effective = service.getEffective(EVENT_ID);

        assertEquals(2, effective.size());
        assertEquals(Boolean.TRUE, effective.get("extraBatter"));
        assertEquals(Boolean.TRUE, effective.get("forceHalf"));
    }

    @Test
    @DisplayName("hasRules：有行 true / 无行 false / eventId 为空 false")
    void hasRules_readsExistence() {
        when(eventSpecialRuleRepository.existsByEventId(EVENT_ID)).thenReturn(true, false);

        assertTrue(service.hasRules(EVENT_ID));
        assertFalse(service.hasRules(EVENT_ID));
        assertFalse(service.hasRules(null));
    }

    @Test
    @DisplayName("updateRules 写入并回读生效值（新增 + 更新既有行，保留 createdBy）")
    void updateRules_upsertAndReturnEffective() {
        EventSpecialRule existing = rule(EventSpecialRuleService.RULE_FORCE_HALF, true);
        existing.setId(7L);
        existing.setCreatedBy(88L);
        when(eventSpecialRuleRepository.findByEventIdAndRuleKey(EVENT_ID, EventSpecialRuleService.RULE_EXTRA_BATTER))
                .thenReturn(Optional.empty());
        when(eventSpecialRuleRepository.findByEventIdAndRuleKey(EVENT_ID, EventSpecialRuleService.RULE_FORCE_HALF))
                .thenReturn(Optional.of(existing));

        List<EventSpecialRule> savedRows = new ArrayList<>();
        when(eventSpecialRuleRepository.save(any(EventSpecialRule.class))).thenAnswer(inv -> {
            EventSpecialRule row = inv.getArgument(0);
            savedRows.add(row);
            return row;
        });
        when(eventSpecialRuleRepository.findByEventId(EVENT_ID)).thenReturn(savedRows);

        Map<String, Boolean> rules = new LinkedHashMap<>();
        rules.put("extraBatter", true);
        rules.put("forceHalf", false);

        Map<String, Boolean> effective = service.updateRules(EVENT_ID, TENANT_ID, rules);

        assertEquals(Boolean.TRUE, effective.get("extraBatter"));
        assertEquals(Boolean.FALSE, effective.get("forceHalf"));
        assertEquals(2, savedRows.size());
        EventSpecialRule newRow = savedRows.get(0);
        assertEquals(TENANT_ID, newRow.getTenantId());
        assertEquals(EVENT_ID, newRow.getEventId());
        assertEquals(EventSpecialRuleService.RULE_EXTRA_BATTER, newRow.getRuleKey());
        assertEquals(Boolean.TRUE, newRow.getEnabled());
        assertEquals(USER_ID, newRow.getCreatedBy());
        assertEquals(88L, existing.getCreatedBy());
        assertEquals(USER_ID, existing.getUpdatedBy());
        assertEquals(Boolean.FALSE, existing.getEnabled());
    }

    @Test
    @DisplayName("未知规则键：400 且零写入")
    void updateRules_unknownKey_rejectedWithoutWrites() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateRules(EVENT_ID, TENANT_ID, Map.of("unknownKey", true)));

        assertEquals(400, ex.getCode());
        verifyNoInteractions(eventSpecialRuleRepository);
    }

    @Test
    @DisplayName("空映射：400 且零写入")
    void updateRules_empty_rejectedWithoutWrites() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateRules(EVENT_ID, TENANT_ID, Map.of()));

        assertEquals(400, ex.getCode());
        verifyNoInteractions(eventSpecialRuleRepository);
    }

    @Test
    @DisplayName("null 值：400 且零写入")
    void updateRules_nullValue_rejectedWithoutWrites() {
        Map<String, Boolean> rules = new LinkedHashMap<>();
        rules.put("extraBatter", null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateRules(EVENT_ID, TENANT_ID, rules));

        assertEquals(400, ex.getCode());
        verifyNoInteractions(eventSpecialRuleRepository);
    }

    private EventSpecialRule rule(String ruleKey, boolean enabled) {
        EventSpecialRule row = new EventSpecialRule();
        row.setEventId(EVENT_ID);
        row.setTenantId(TENANT_ID);
        row.setRuleKey(ruleKey);
        row.setEnabled(enabled);
        return row;
    }
}
