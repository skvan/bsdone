/*
 * 账号权限重构（批次 3a）：球员自助建档与本人档案编辑（SELF 通道）。
 *
 * 端点（路径逐字，供 T3.8 种子收录）：
 *  - GET  /account/player-profile         取本人档案（无则返回 null）
 *  - POST /account/player-profile         自助建档（body=Player 草稿）
 *  - PUT  /account/player-profile/{playerId}  本人档案编辑（body=Map，仅 SELF 白名单生效）
 *
 * 当前用户取自 JWT（参照 PlayerClaimApi.requireUserId）；未登录抛 UnauthorizedException。
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.entity.Player;
import com.bsball.service.JwtService;
import com.bsball.service.PlayerService;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/account"})
public class PlayerSelfProfileApi {
    private final PlayerService playerService;
    private final JwtService jwtService;

    @GetMapping(value={"/player-profile"})
    public Result<Player> getSelfProfile(@RequestHeader(value="Authorization", required=false) String auth) {
        Long userId = this.requireUserId(auth);
        return Result.ok(this.playerService.getSelfProfile(userId));
    }

    @PostMapping(value={"/player-profile"})
    public Result<Player> createSelfProfile(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Player draft) {
        Long userId = this.requireUserId(auth);
        return Result.ok(this.playerService.createSelfProfile(userId, draft));
    }

    @PutMapping(value={"/player-profile/{playerId}"})
    public Result<Player> updateSelfProfile(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long playerId, @RequestBody Map<String, Object> body) {
        Long userId = this.requireUserId(auth);
        return Result.ok(this.playerService.updateSelfProfile(userId, playerId, body));
    }

    private Long requireUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = this.jwtService.parseUserId(token);
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return userId;
    }

    @Generated
    public PlayerSelfProfileApi(PlayerService playerService, JwtService jwtService) {
        this.playerService = playerService;
        this.jwtService = jwtService;
    }
}
