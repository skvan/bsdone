package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.service.ClubhouseService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开个人主页接口：免登录只读，按球员 ID 取数。
 * <p>球员未认领、跨租户、主人未开启分享时统一返回 404；响应按主人启用模块裁剪数据。
 * 分享链接可携带分享时的范围参数，公开页锁定该范围。</p>
 */
@RestController
@RequestMapping(value = {"/player/{playerId}/clubhouse"})
@RequiredArgsConstructor
public class PublicClubhouseApi {
    private final ClubhouseService clubhouseService;

    @GetMapping
    public Result<Map<String, Object>> dashboard(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String gameMode,
            HttpServletResponse response) {
        noStore(response);
        return Result.ok(clubhouseService.getPublicDashboard(playerId, teamId, leagueId, eventId, gameMode));
    }

    @GetMapping(value = {"/milestones"})
    public Result<Map<String, Object>> milestones(
            @PathVariable Long playerId,
            HttpServletResponse response) {
        noStore(response);
        return Result.ok(clubhouseService.getPublicMilestones(playerId));
    }

    @GetMapping(value = {"/sprays"})
    public Result<Map<String, Object>> sprays(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String gameMode,
            HttpServletResponse response) {
        noStore(response);
        return Result.ok(clubhouseService.getPublicSprays(playerId, teamId, leagueId, eventId, gameMode));
    }

    private void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }
}
