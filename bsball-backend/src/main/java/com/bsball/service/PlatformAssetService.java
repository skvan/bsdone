/*
 * 账号权限重构（批次 3b，Task 3.14 / spec §6.10）：平台资产归还汇总（最小实现）。
 *
 * 数据主权：历史数据属平台资产；租户管理员及以下的「删除」= 软删 + platform_owned 置位（归还系统租户/平台回收）。
 * 本服务仅提供「已归还」资产计数汇总，供系统超管掌握平台回收规模；完整浏览/处置视图排后（待纠错立项）。
 *
 * 可见性：仅系统超管（非超管 403）。platform_owned 不参与任何现有查询过滤，仅汇总/标记用途。
 */
package com.bsball.service;

import com.bsball.exception.BusinessException;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.TeamRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class PlatformAssetService {

    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final EventRepository eventRepository;
    private final GameRepository gameRepository;
    private final ApiPermissionService apiPermissionService;

    /**
     * 平台资产归还汇总（spec §6.10）：各实体 {@code platform_owned=true} 且已软删的计数。
     * <p>仅系统超管可访问；非超管一律 403。
     */
    public Map<String, Object> summary(Long operatorUserId) {
        if (!this.apiPermissionService.isSuperAdmin(operatorUserId)) {
            throw new BusinessException(403, "仅系统超管可查看平台资产汇总");
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("league", this.leagueRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull());
        out.put("team", this.teamRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull());
        out.put("player", this.playerRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull());
        out.put("event", this.eventRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull());
        out.put("game", this.gameRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull());
        return out;
    }

    @Generated
    public PlatformAssetService(LeagueRepository leagueRepository, TeamRepository teamRepository,
            PlayerRepository playerRepository, EventRepository eventRepository, GameRepository gameRepository,
            ApiPermissionService apiPermissionService) {
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.apiPermissionService = apiPermissionService;
    }
}
