/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalDevtoolsReportAdminApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.PortalDevtoolsReport
 *  com.bsball.service.PortalDevtoolsReportService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.PortalDevtoolsReport;
import com.bsball.service.PortalDevtoolsReportService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/portal/devtools-report"})
public class PortalDevtoolsReportAdminApi {
    private final PortalDevtoolsReportService portalDevtoolsReportService;

    @GetMapping(value={"/list"})
    public Result<PageResult<PortalDevtoolsReport>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String excludeKeyword, @RequestParam(required=false) String ip, @RequestParam(required=false) String ipRegion) {
        return Result.ok((Object)this.portalDevtoolsReportService.list(page, pageSize, keyword, excludeKeyword, ip, ipRegion));
    }

    @Generated
    public PortalDevtoolsReportAdminApi(PortalDevtoolsReportService portalDevtoolsReportService) {
        this.portalDevtoolsReportService = portalDevtoolsReportService;
    }
}

