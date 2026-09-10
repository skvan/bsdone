/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.TeamLineupTemplate
 *  com.bsball.repository.TeamLineupTemplateRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.TeamLineupTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamLineupTemplateRepository
extends JpaRepository<TeamLineupTemplate, Long>,
JpaSpecificationExecutor<TeamLineupTemplate> {
    public List<TeamLineupTemplate> findByTeamIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long var1);

    public Optional<TeamLineupTemplate> findByIdAndTeamIdAndDeletedAtIsNull(Long var1, Long var2);
}

