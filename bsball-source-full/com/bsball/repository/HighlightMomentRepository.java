/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.HighlightMoment
 *  com.bsball.repository.HighlightMomentRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.HighlightMoment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HighlightMomentRepository
extends JpaRepository<HighlightMoment, Long>,
JpaSpecificationExecutor<HighlightMoment> {
}

