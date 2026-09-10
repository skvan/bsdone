/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PlayerClaim
 *  com.bsball.repository.PlayerClaimRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.PlayerClaim;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlayerClaimRepository
extends JpaRepository<PlayerClaim, Long>,
JpaSpecificationExecutor<PlayerClaim> {
    public Optional<PlayerClaim> findByIdAndDeletedAtIsNull(Long var1);

    public List<PlayerClaim> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long var1);

    public boolean existsByUserIdAndPlayerIdAndStatusAndDeletedAtIsNull(Long var1, Long var2, String var3);

    public boolean existsByPlayerIdAndStatusAndDeletedAtIsNull(Long var1, String var2);

    public boolean existsByUserIdAndStatusAndDeletedAtIsNull(Long var1, String var2);

    public Optional<PlayerClaim> findTopByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(Long var1, String var2);
}

