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
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerClaimInviteRepository
extends JpaRepository<PlayerClaimInvite, Long> {
    public Optional<PlayerClaimInvite> findByTokenAndDeletedAtIsNull(String var1);
}

