/*
 * 建盟链路（批次①）：门户自助建联盟端点。
 *
 * 类级映射固定在 /portal（与 PortalFeedbackApi 等门户控制器同约定）：
 * 真实路径 = /portal/league-create，与以下三处逐字一致：
 *  - 前端 business.js: leagueApi.portalCreate → /api/portal/league-create；
 *  - 种子 InitDataRunner.ensurePortalLeagueCreateApiIfNeeded → /portal/league-create；
 *  - member 白名单谓词 isApiAllowedForMember → p.startsWith("/portal/")。
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.service.LeagueService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/portal"})
public class PortalLeagueCreateApi {
    private final LeagueService leagueService;

    /**
     * 门户自助建联盟：仅登录用户可调（401 兜底）。
     * 语义由 createForCurrentUser 分派：门户角色→按租户开关落申请/直建自授权；管理员→直建。
     */
    @PostMapping(value={"/league-create"})
    public Result<Map<String, Object>> portalCreate(@RequestBody @Valid League body) {
        if (CurrentUserHolder.get() == null) {
            throw new BusinessException(401, "\u8bf7\u5148\u767b\u5f55");
        }
        return Result.ok(this.leagueService.createForCurrentUser(body));
    }

    @Generated
    public PortalLeagueCreateApi(LeagueService leagueService) {
        this.leagueService = leagueService;
    }
}
