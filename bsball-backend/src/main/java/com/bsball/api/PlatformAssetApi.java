/*
 * 账号权限重构（批次 3b，Task 3.14 / spec §6.10）：平台资产归还汇总端点。
 *
 * GET /sys/platform-asset/summary（仅系统超管）——返回各实体 platform_owned=true 且已软删的计数。
 * 完整浏览/处置视图排后（登记待纠错立项）。超管在 ApiPermissionFilter 层放行；非超管由过滤器与本服务双重拒绝。
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.service.PlatformAssetService;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/platform-asset"})
public class PlatformAssetApi {

    private final PlatformAssetService platformAssetService;

    @GetMapping(value={"/summary"})
    public Result<Map<String, Object>> summary() {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "请先登录");
        }
        return Result.ok(this.platformAssetService.summary(uid));
    }

    @Generated
    public PlatformAssetApi(PlatformAssetService platformAssetService) {
        this.platformAssetService = platformAssetService;
    }
}
