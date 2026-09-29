/*
 * 账号权限重构（批次 1）：范围解析上下文（ScopeResolutionContext）。
 *
 * 语义与分工：
 *  - 本类是一次「范围解析过程」的可变工作台：承载主体标识（userId / tenantId）与逐步汇聚的
 *    联盟 / 球队 ID 集合，由 ScopeRelationProvider 各实现按关系数据向其注入（contribute）；
 *  - 集合「按设计可写」：addLeague / addTeam 供 Provider 注入与兼容分支展开使用，
 *    故本类不提供不可变包装，也不要在此添加此类改动；
 *  - 注入契约：注入一律走 addLeague / addTeam（内置 null 守卫）；请勿直接对 getLeagueIds() /
 *    getTeamIds() 的返回集合做 add / addAll 注入，以免绕过守卫导致下游 EffectiveScope 的
 *    Set.copyOf 因 null 元素抛 NPE；
 *  - 最终不可变性由消费端 EffectiveScope 保证：其构造器对集合做 Set.copyOf 防御性拷贝，
 *    对外以不可变视图暴露。可变工作台 → 不可变结果，职责分明。
 *
 * 说明：addLeague / addTeam 对入参为 null 时静默忽略（不注入、不抛异常）。
 */
package com.bsball.service.scope;

import java.util.HashSet;
import java.util.Set;

public final class ScopeResolutionContext {

    /** 账号用户 ID；null 表示访客 / 未登录等无主体场景。 */
    private final Long userId;

    /** 租户 ID，范围解析始终限定在单一租户内。 */
    private final long tenantId;

    /** 我可管理的联盟 ID 集合（可写工作台，由 Provider 注入）。 */
    private final Set<Long> leagueIds = new HashSet<>();

    /** 我可管理的球队 ID 集合（可写工作台，由 Provider 注入）。 */
    private final Set<Long> teamIds = new HashSet<>();

    public ScopeResolutionContext(Long userId, long tenantId) {
        this.userId = userId;
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public long getTenantId() {
        return tenantId;
    }

    /** 返回可写的联盟 ID 工作集合，供 Provider 注入。 */
    public Set<Long> getLeagueIds() {
        return leagueIds;
    }

    /** 返回可写的球队 ID 工作集合，供 Provider 注入。 */
    public Set<Long> getTeamIds() {
        return teamIds;
    }

    /** 注入一个联盟 ID；id 为 null 时静默忽略。 */
    public void addLeague(Long id) {
        if (id != null) {
            leagueIds.add(id);
        }
    }

    /** 注入一个球队 ID；id 为 null 时静默忽略。 */
    public void addTeam(Long id) {
        if (id != null) {
            teamIds.add(id);
        }
    }
}
