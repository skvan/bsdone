/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalIpLocationAdminApi
 *  com.bsball.common.Result
 *  com.bsball.model.dto.IpLocationBatchDto
 *  com.bsball.model.dto.IpLocationBatchRequest
 *  com.bsball.model.dto.IpLocationDto
 *  com.bsball.service.LbsIpLocationService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.IpLocationBatchDto;
import com.bsball.model.dto.IpLocationBatchRequest;
import com.bsball.model.dto.IpLocationDto;
import com.bsball.service.LbsIpLocationService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/portal"})
public class PortalIpLocationAdminApi {
    private final LbsIpLocationService lbsIpLocationService;

    @GetMapping(value={"/ip-location"})
    public Result<IpLocationDto> ipLocation(@RequestParam(value="ip") String ip) {
        return Result.ok((Object)this.lbsIpLocationService.locate(ip));
    }

    @PostMapping(value={"/ip-location/batch"})
    public Result<IpLocationBatchDto> ipLocationBatch(@RequestBody(required=false) IpLocationBatchRequest body) {
        return Result.ok((Object)this.lbsIpLocationService.locateBatch(body != null ? body.ips() : null));
    }

    @Generated
    public PortalIpLocationAdminApi(LbsIpLocationService lbsIpLocationService) {
        this.lbsIpLocationService = lbsIpLocationService;
    }
}

