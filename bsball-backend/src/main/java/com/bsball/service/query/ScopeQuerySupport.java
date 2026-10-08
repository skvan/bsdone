/*
 * 账号权限重构（批次 2，Task 2.2）：查询层统一收口组件（ScopeQuerySupport）。
 *
 * 职责：把「管理视图读」统一收窄到调用方所属域，供 T2.4 的十项服务接线复用，
 * 避免各处重复拼装可见 ID 集合导致的语义漂移。
 *
 * 核心语义（务必精确）：
 *  - 非管理上下文（ScopeContextHolder.isManage() == false）或 租户内不受限身份 → 返回 null（= 不限制，宽读）；
 *  - 管理上下文 + 受限身份 → 返回窄域 ID 列表；
 *  - 空集 → 原样返回空集（调用方据此渲染空页），绝不把空集「优化」成 null。
 *
 * 与 EffectiveScope（批次 1）配合：本类只读其 leagueIds / teamIds 与 unrestrictedInTenant 语义，
 * 不涉及 guestLikeRead（门户只读放行由读路径另行处理）。
 *
 * 消费范式（三项约定，务必遵守）：
 *  - null ⇒ 跳过过滤谓词（不限制/宽读，勿当空集处理）；
 *  - 空集 ⇒ 直接短路返回空页（勿再查库）；
 *  - 非空 ⇒ 作为 IN 集合使用。
 * 返回值顺序不保证：仅供 IN 过滤 / contains 判定使用，勿依赖顺序。
 */
package com.bsball.service.query;

import com.bsball.core.ScopeContextHolder;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.repository.EventRepository;
import com.bsball.repository.TeamRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ScopeQuerySupport {

    private final TeamRepository teamRepository;
    private final EventRepository eventRepository;

    public ScopeQuerySupport(TeamRepository teamRepository, EventRepository eventRepository) {
        this.teamRepository = teamRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * 可见联盟 ID 列表。
     *
     * @return null = 不限制（宽读）；空集 = 空页；非空 = 窄域
     */
    public List<Long> visibleLeagueIds(EffectiveScope s) {
        if (!ScopeContextHolder.isManage() || s.isUnrestrictedInTenant()) {
            return null;
        }
        return List.copyOf(s.getLeagueIds());
    }

    /**
     * 可见球队 ID 列表：显式授予的 teamIds ∪ 由可见联盟派生的球队（同租户、未删除）。
     *
     * @return null = 不限制（宽读）；空集 = 空页；非空 = 窄域
     */
    public List<Long> visibleTeamIds(EffectiveScope s, long tenantId) {
        if (!ScopeContextHolder.isManage() || s.isUnrestrictedInTenant()) {
            return null;
        }
        Set<Long> ids = new HashSet<>(s.getTeamIds());
        if (!s.getLeagueIds().isEmpty()) {
            ids.addAll(this.teamRepository.findIdsByLeagueIdInAndTenantId(s.getLeagueIds(), tenantId));
        }
        return List.copyOf(ids);
    }

    /**
     * 可见赛事 ID 列表：完全由可见联盟派生（同租户、未删除）；无可见联盟即空集。
     *
     * @return null = 不限制（宽读）；空集 = 空页；非空 = 窄域
     */
    public List<Long> visibleEventIds(EffectiveScope s, long tenantId) {
        if (!ScopeContextHolder.isManage() || s.isUnrestrictedInTenant()) {
            return null;
        }
        if (s.getLeagueIds().isEmpty()) {
            return List.of();
        }
        return List.copyOf(this.eventRepository.findIdsByLeagueIdInAndTenantId(s.getLeagueIds(), tenantId));
    }
}
