/*
 * 参考样例：DTO 值对象的纯单元测试写法。
 * 仅校验不可变值对象的读写双语义，不启动 Spring 容器、不访问数据库/网络。
 */
package com.bsball.model.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EffectiveScope：有效范围的读写双语义值对象")
class EffectiveScopeTest {

    @Test
    @DisplayName("访客只读：读放行，但绝不获得管理权，且管理域为空")
    void guestLikeRead_allowsRead_butNeverManage() {
        EffectiveScope s = EffectiveScope.restricted(true, Set.of(), Set.of());
        assertTrue(s.canReadLeague(1L));
        assertTrue(s.canReadTeam(1L));
        assertFalse(s.canManageLeague(1L));
        assertFalse(s.canManageTeam(1L));
        assertTrue(s.isManageEmpty());
    }

    @Test
    @DisplayName("受限范围：只放行给定 ID")
    void restricted_containsOnlyGivenIds() {
        EffectiveScope s = EffectiveScope.restricted(false, Set.of(10L), Set.of(100L));
        assertTrue(s.canManageLeague(10L));
        assertFalse(s.canManageLeague(11L));
        assertTrue(s.canManageTeam(100L));
        assertFalse(s.canReadTeam(101L));
        assertFalse(s.isManageEmpty());
    }

    @Test
    @DisplayName("不受限：读写全部放行，管理域非空")
    void unrestricted_allowsEverything() {
        EffectiveScope s = EffectiveScope.unrestricted();
        assertTrue(s.canManageLeague(9L));
        assertTrue(s.canManageTeam(9L));
        assertTrue(s.canReadLeague(9L));
        assertTrue(s.canReadTeam(9L));
        assertFalse(s.isManageEmpty());
    }

    @Test
    @DisplayName("外部集合后续变更不影响已构造实例（防御性拷贝）")
    void externalMutation_doesNotAffectConstructedScope() {
        java.util.Set<Long> ids = new java.util.HashSet<>(java.util.Set.of(10L));
        EffectiveScope s = EffectiveScope.restricted(false, ids, java.util.Set.of());
        ids.add(11L);
        assertFalse(s.canManageLeague(11L));
        assertTrue(s.canManageLeague(10L));
    }

    @Test
    @DisplayName("empty 工厂：管理域为空且不放行")
    void empty_isEmptyAndDenyManage() {
        EffectiveScope s = EffectiveScope.empty();
        assertTrue(s.isManageEmpty());
        assertFalse(s.canManageLeague(1L));
        assertFalse(s.canManageTeam(1L));
        assertFalse(s.isUnrestrictedInTenant());
    }
}
