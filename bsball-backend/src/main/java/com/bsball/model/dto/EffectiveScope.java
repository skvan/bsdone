/*
 * 账号权限重构（批次 1）：有效范围（EffectiveScope）读写双语义值对象。
 *
 * 语义铁律：
 *  - 读（canRead*）  = 访客只读放行 ∪ 租户内不受限 ∪ 集合命中；
 *  - 写/管理（canManage*） 只认“租户内不受限 ∪ 集合命中”，绝不读 guestLikeRead 标志；
 *  - isManageEmpty 用于“管理域为空 ⇒ 空页 / 403”判定。
 *
 * 不可变值对象：构造后字段不再变化，集合以不可变视图对外暴露。
 */
package com.bsball.model.dto;

import java.util.Collections;
import java.util.Set;

public final class EffectiveScope {

    /** 门户只读放行（仅影响读语义，绝不授予管理权）。 */
    private final boolean guestLikeRead;

    /** 超管 / 租户管理员：租户内不受限。 */
    private final boolean unrestrictedInTenant;

    /** 我可管理的联盟 ID 集合。 */
    private final Set<Long> leagueIds;

    /** 我可管理的球队 ID 集合。 */
    private final Set<Long> teamIds;

    private EffectiveScope(boolean guestLikeRead, boolean unrestrictedInTenant, Set<Long> leagueIds, Set<Long> teamIds) {
        this.guestLikeRead = guestLikeRead;
        this.unrestrictedInTenant = unrestrictedInTenant;
        this.leagueIds = leagueIds == null ? Set.of() : leagueIds;
        this.teamIds = teamIds == null ? Set.of() : teamIds;
    }

    /** 租户内不受限（超管 / 租户管理员）：读写全放行。 */
    public static EffectiveScope unrestricted() {
        return new EffectiveScope(false, true, Set.of(), Set.of());
    }

    /** 受限范围：仅放行给定 ID；guestLikeRead 只影响读语义。 */
    public static EffectiveScope restricted(boolean guestLikeRead, Set<Long> leagueIds, Set<Long> teamIds) {
        return new EffectiveScope(guestLikeRead, false, leagueIds, teamIds);
    }

    /** 空范围：什么都不放行（管理域为空）。 */
    public static EffectiveScope empty() {
        return new EffectiveScope(false, false, Set.of(), Set.of());
    }

    public boolean isGuestLikeRead() {
        return this.guestLikeRead;
    }

    public boolean isUnrestrictedInTenant() {
        return this.unrestrictedInTenant;
    }

    public Set<Long> getLeagueIds() {
        return Collections.unmodifiableSet(this.leagueIds);
    }

    public Set<Long> getTeamIds() {
        return Collections.unmodifiableSet(this.teamIds);
    }

    public boolean canReadLeague(long leagueId) {
        return this.guestLikeRead || this.unrestrictedInTenant || this.leagueIds.contains(leagueId);
    }

    public boolean canReadTeam(long teamId) {
        return this.guestLikeRead || this.unrestrictedInTenant || this.teamIds.contains(teamId);
    }

    public boolean canManageLeague(long leagueId) {
        return this.unrestrictedInTenant || this.leagueIds.contains(leagueId);
    }

    public boolean canManageTeam(long teamId) {
        return this.unrestrictedInTenant || this.teamIds.contains(teamId);
    }

    /** 管理域是否为空：非不受限且两个集合均为空。 */
    public boolean isManageEmpty() {
        return !this.unrestrictedInTenant && this.leagueIds.isEmpty() && this.teamIds.isEmpty();
    }
}
