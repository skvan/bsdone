/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysLoginLogApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysLoginLog
 *  com.bsball.service.SysLoginLogService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.SysLoginLog;
import com.bsball.service.SysLoginLogService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/login-log"})
public class SysLoginLogApi {
    private final SysLoginLogService sysLoginLogService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysLoginLog>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String status, @RequestParam(required=false) String ip, @RequestParam(required=false) String ipRegion) {
        PageResult data = this.sysLoginLogService.list(page, pageSize, keyword, status, ip, ipRegion);
        return Result.ok((Object)data);
    }

    @Generated
    public SysLoginLogApi(SysLoginLogService sysLoginLogService) {
        this.sysLoginLogService = sysLoginLogService;
    }
}

