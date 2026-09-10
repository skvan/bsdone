/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.TeamManager
 *  com.bsball.repository.TeamManagerRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.TeamManager;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamManagerRepository
extends JpaRepository<TeamManager, Long> {
    public List<TeamManager> findByTeamIdAndStatusAndDeletedAtIsNull(Long var1, String var2);

    public List<TeamManager> findByUserIdAndStatusAndDeletedAtIsNull(Long var1, String var2);

    public Optional<TeamManager> findByTeamIdAndUserIdAndDeletedAtIsNull(Long var1, Long var2);

    public boolean existsByTeamIdAndUserIdAndStatusAndDeletedAtIsNull(Long var1, Long var2, String var3);
}

