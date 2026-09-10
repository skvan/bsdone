/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysMediaGalleryItem
 *  com.bsball.repository.SysMediaGalleryItemRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.SysMediaGalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysMediaGalleryItemRepository
extends JpaRepository<SysMediaGalleryItem, Long>,
JpaSpecificationExecutor<SysMediaGalleryItem> {
}

