/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.ChinaRegionItemDto
 *  com.bsball.model.entity.ChinaRegion
 *  com.bsball.repository.ChinaRegionRepository
 *  com.bsball.service.RegionService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.model.dto.ChinaRegionItemDto;
import com.bsball.model.entity.ChinaRegion;
import com.bsball.repository.ChinaRegionRepository;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionService {
    private final ChinaRegionRepository chinaRegionRepository;

    @Transactional(readOnly=true)
    public List<ChinaRegionItemDto> listChinaChildren(String parentAdcode) {
        List rows = parentAdcode == null || parentAdcode.isBlank() ? this.chinaRegionRepository.findByLevelOrderByAdcodeAsc(1) : this.chinaRegionRepository.findByParentAdcodeOrderByAdcodeAsc(parentAdcode.trim());
        return rows.stream().map(RegionService::toDto).toList();
    }

    private static ChinaRegionItemDto toDto(ChinaRegion r) {
        return new ChinaRegionItemDto(r.getAdcode(), r.getName(), (int)r.getLevel());
    }

    @Generated
    public RegionService(ChinaRegionRepository chinaRegionRepository) {
        this.chinaRegionRepository = chinaRegionRepository;
    }
}

