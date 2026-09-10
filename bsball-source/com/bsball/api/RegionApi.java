/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.RegionApi
 *  com.bsball.common.Result
 *  com.bsball.model.dto.ChinaRegionItemDto
 *  com.bsball.service.RegionService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.ChinaRegionItemDto;
import com.bsball.service.RegionService;
import java.util.List;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/region/china"})
public class RegionApi {
    private final RegionService regionService;

    @GetMapping(value={"/children"})
    public Result<List<ChinaRegionItemDto>> children(@RequestParam(required=false) String parentAdcode) {
        return Result.ok((Object)this.regionService.listChinaChildren(parentAdcode));
    }

    @Generated
    public RegionApi(RegionService regionService) {
        this.regionService = regionService;
    }
}

