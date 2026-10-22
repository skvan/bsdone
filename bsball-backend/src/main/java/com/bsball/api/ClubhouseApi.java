package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.exception.UnauthorizedException;
import com.bsball.service.ClubhouseService;
import com.bsball.service.JwtService;
import com.bsball.service.UserClubhouseConfigService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人驾驶舱接口（模块化布局 v3-modular，尺寸固定）。
 * <ul>
 *   <li>GET  /account/clubhouse：聚合数据与 moduleCatalog，可选 teamId/eventId 切换代表球队与赛事</li>
 *   <li>PUT  /account/clubhouse/config：保存模块顺序/开关、表现图与队内排名指标</li>
 *   <li>POST /account/clubhouse/config/reset：恢复默认配置</li>
 * </ul>
 */
@RestController
@RequestMapping(value = {"/account/clubhouse"})
@RequiredArgsConstructor
public class ClubhouseApi {
    private final ClubhouseService clubhouseService;
    private final UserClubhouseConfigService configService;
    private final JwtService jwtService;

    @GetMapping
    public Result<Map<String, Object>> dashboard(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long eventId) {
        return Result.ok(clubhouseService.getDashboard(requireUserId(auth), teamId, eventId));
    }

    @PutMapping(value = {"/config"})
    public Result<Map<String, Object>> saveConfig(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(auth);
        return Result.ok(configService.save(userId, clubhouseService.playerTenantId(userId), body));
    }

    @PostMapping(value = {"/config/reset"})
    public Result<Map<String, Object>> resetConfig(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = requireUserId(auth);
        return Result.ok(configService.reset(userId, clubhouseService.playerTenantId(userId)));
    }

    private Long requireUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = jwtService.parseUserId(token);
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return userId;
    }

}
