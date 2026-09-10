/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.HistoryRecord
 *  com.bsball.repository.HistoryRecordRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HistoryRecordRepository
extends JpaRepository<HistoryRecord, Long>,
JpaSpecificationExecutor<HistoryRecord> {
}

