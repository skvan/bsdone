/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PlayerClaimInvite
 *  com.bsball.repository.PlayerClaimInviteRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.PlayerClaimInvite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerClaimInviteRepository
extends JpaRepository<PlayerClaimInvite, Long> {
    public Optional<PlayerClaimInvite> findByTokenAndDeletedAtIsNull(String var1);

    /** 球队入队邀请列表（电子秩序册批 4，Issue #310）。 */
    public List<PlayerClaimInvite> findByTeamIdAndDeletedAtIsNullOrderByIdDesc(Long teamId);
}

