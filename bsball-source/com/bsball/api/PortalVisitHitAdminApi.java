/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalVisitHitAdminApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.PortalVisitHit
 *  com.bsball.service.PortalVisitService
 *  lombok.Generated
 *  org.springframework.format.annotation.DateTimeFormat
 *  org.springframework.format.annotation.DateTimeFormat$ISO
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.PortalVisitHit;
import com.bsball.service.PortalVisitService;
import java.time.LocalDate;
import lombok.Generated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/portal/visit-hit"})
public class PortalVisitHitAdminApi {
    private final PortalVisitService portalVisitService;

    @GetMapping(value={"/list"})
    public Result<PageResult<PortalVisitHit>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String excludeKeyword, @RequestParam(required=false) String ip, @RequestParam(required=false) String ipRegion, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hitDateFrom, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hitDateTo) {
        return Result.ok((Object)this.portalVisitService.listHits(page, pageSize, keyword, excludeKeyword, ip, ipRegion, hitDateFrom, hitDateTo));
    }

    @Generated
    public PortalVisitHitAdminApi(PortalVisitService portalVisitService) {
        this.portalVisitService = portalVisitService;
    }
}

