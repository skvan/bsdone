/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PortalDevtoolsReport
 *  com.bsball.repository.PortalDevtoolsReportRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.PortalDevtoolsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PortalDevtoolsReportRepository
extends JpaRepository<PortalDevtoolsReport, Long>,
JpaSpecificationExecutor<PortalDevtoolsReport> {
}

