/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.EffectiveDataScope
 */
package com.bsball.model.dto;

import java.util.Collections;
import java.util.Set;

public final class EffectiveDataScope {
    private final boolean unrestrictedInTenant;
    private final Set<Long> leagueIds;
    private final Set<Long> teamIds;

    private EffectiveDataScope(boolean unrestrictedInTenant, Set<Long> leagueIds, Set<Long> teamIds) {
        this.unrestrictedInTenant = unrestrictedInTenant;
        this.leagueIds = leagueIds;
        this.teamIds = teamIds;
    }

    public static EffectiveDataScope unrestricted() {
        return new EffectiveDataScope(true, Set.of(), Set.of());
    }

    public static EffectiveDataScope restricted(Set<Long> leagueIds, Set<Long> teamIds) {
        return new EffectiveDataScope(false, leagueIds != null ? leagueIds : Set.of(), teamIds != null ? teamIds : Set.of());
    }

    public static EffectiveDataScope empty() {
        return new EffectiveDataScope(false, Set.of(), Set.of());
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
        if (this.unrestrictedInTenant) {
            return true;
        }
        return this.leagueIds.contains(leagueId);
    }

    public boolean canReadTeam(long teamId) {
        if (this.unrestrictedInTenant) {
            return true;
        }
        return this.teamIds.contains(teamId);
    }
}

