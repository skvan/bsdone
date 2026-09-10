/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysIpLocationCacheAdminApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.model.dto.IpLocationRefreshResultDto
 *  com.bsball.model.dto.LbsProviderOptionDto
 *  com.bsball.model.enums.LbsProvider
 *  com.bsball.model.vo.IpLocationCacheRowVo
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.LbsIpLocationService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.config.OpenPlatformProperties;
import com.bsball.model.dto.IpLocationRefreshResultDto;
import com.bsball.model.dto.LbsProviderOptionDto;
import com.bsball.model.enums.LbsProvider;
import com.bsball.model.vo.IpLocationCacheRowVo;
import com.bsball.service.IpLocationCacheService;
import com.bsball.service.LbsIpLocationService;
import java.util.List;
import java.util.Optional;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/ip-location-cache"})
public class SysIpLocationCacheAdminApi {
    private final OpenPlatformProperties openPlatformProperties;
    private final IpLocationCacheService ipLocationCacheService;
    private final LbsIpLocationService lbsIpLocationService;

    @GetMapping(value={"/page"})
    public Result<PageResult<IpLocationCacheRowVo>> page(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String sortField, @RequestParam(required=false) String sortOrder) {
        return Result.ok((Object)this.ipLocationCacheService.page(page, pageSize, keyword, sortField, sortOrder));
    }

    @GetMapping(value={"/lbs-providers"})
    public Result<List<LbsProviderOptionDto>> lbsProviders() {
        return Result.ok((Object)this.lbsIpLocationService.listLbsProviderOptions());
    }

    @PostMapping(value={"/refresh"})
    public Result<IpLocationRefreshResultDto> refresh(@RequestParam(value="ip") String ip, @RequestParam(value="provider", required=false) String provider) {
        String n;
        if (!this.openPlatformProperties.anyProviderConfigured()) {
            return Result.fail((int)400, (String)"\u672a\u914d\u7f6e IP \u5b9a\u4f4d\u5bc6\u94a5\uff08\u9ad8\u5fb7/\u767e\u5ea6/\u817e\u8baf\u7b49\u81f3\u5c11\u586b\u5176\u4e00\uff09");
        }
        if (ip == null || ip.isBlank()) {
            return Result.fail((int)400, (String)"IP \u4e0d\u80fd\u4e3a\u7a7a");
        }
        LbsProvider only = null;
        if (provider != null && !provider.isBlank()) {
            try {
                only = LbsProvider.valueOf((String)provider.trim().toUpperCase());
            }
            catch (IllegalArgumentException e) {
                return Result.fail((int)400, (String)"\u672a\u77e5\u7684\u6570\u636e\u6e90\uff0c\u53ef\u9009\uff1aAMAP\u3001TENCENT\u3001BAIDU\u3001INTERFACE_BOX");
            }
            if (!this.lbsIpLocationService.isProviderConfigured(only)) {
                return Result.fail((int)400, (String)"\u8be5\u6570\u636e\u6e90\u672a\u914d\u7f6e\u5bc6\u94a5\u6216\u672a\u542f\u7528\uff0c\u8bf7\u5148\u5728\u7cfb\u7edf\u914d\u7f6e\u4e2d\u586b\u5199\u5bf9\u5e94 LBS");
            }
        }
        if (!LbsIpLocationService.isPublicRoutableIp((String)(n = ip.trim()))) {
            return Result.fail((int)400, (String)"\u5185\u7f51\u6216\u4fdd\u7559\u5730\u5740\u65e0\u6cd5\u901a\u8fc7\u516c\u7f51 LBS \u91cd\u65b0\u89e3\u6790");
        }
        Optional region = this.lbsIpLocationService.forceFetchFromProvidersAndPersist(n, only);
        if (region.isEmpty()) {
            String hint = only != null ? "\u6307\u5b9a\u6570\u636e\u6e90\u672a\u8fd4\u56de\u6709\u6548\u5f52\u5c5e\u5730\uff08\u53ef\u68c0\u67e5\u914d\u989d\u3001IP \u662f\u5426\u652f\u6301\u6216\u7a0d\u540e\u91cd\u8bd5\uff09" : "\u5f53\u524d\u5df2\u914d\u7f6e\u7684\u6570\u636e\u6e90\u5747\u672a\u8fd4\u56de\u6709\u6548\u5f52\u5c5e\u5730";
            return Result.fail((int)502, (String)hint);
        }
        return Result.ok((Object)new IpLocationRefreshResultDto((String)region.get()));
    }

    @Generated
    public SysIpLocationCacheAdminApi(OpenPlatformProperties openPlatformProperties, IpLocationCacheService ipLocationCacheService, LbsIpLocationService lbsIpLocationService) {
        this.openPlatformProperties = openPlatformProperties;
        this.ipLocationCacheService = ipLocationCacheService;
        this.lbsIpLocationService = lbsIpLocationService;
    }
}

