/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PortalFeedback
 *  com.bsball.repository.PortalFeedbackRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.PortalFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PortalFeedbackRepository
extends JpaRepository<PortalFeedback, Long>,
JpaSpecificationExecutor<PortalFeedback> {
}

