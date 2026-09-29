/*
 * 账号权限重构（批次 1）：范围关系提供者（ScopeRelationProvider）扩展点。
 *
 * 设计意图：
 *  - 每个实现类对应一种「账号类型 / 关系归属」（如联盟主、球队主、跨区赛事桥接等），
 *    只负责判定自己是否适用于当前账号，并把由关系数据推出的联盟 / 球队 ID 注入上下文；
 *  - 新增账号类型只需新增一个实现类并在容器中注册，范围查询与守卫逻辑零改动（issue #120 的硬要求）。
 */
package com.bsball.service.scope;

public interface ScopeRelationProvider {

    /** 该 Provider 是否适用于当前账号（按关系数据判定）。 */
    boolean supports(ScopeResolutionContext ctx);

    /** 向 ctx 注入 leagueIds / teamIds。 */
    void contribute(ScopeResolutionContext ctx);
}
