/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysOperationLogApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysOperationLog
 *  com.bsball.service.SysOperationLogService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.SysOperationLog;
import com.bsball.service.SysOperationLogService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/operation-log"})
public class SysOperationLogApi {
    private final SysOperationLogService sysOperationLogService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysOperationLog>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String module, @RequestParam(required=false) String action, @RequestParam(required=false) String ip, @RequestParam(required=false) String ipRegion) {
        PageResult data = this.sysOperationLogService.list(page, pageSize, keyword, module, action, ip, ipRegion);
        return Result.ok((Object)data);
    }

    @Generated
    public SysOperationLogApi(SysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }
}

