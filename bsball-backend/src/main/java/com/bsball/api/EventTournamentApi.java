package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.TournamentViewDTO;
import com.bsball.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户赛事积分榜与淘汰赛对阵（Issue #665）。
 * <p>公开只读接口；赛事可见性由 {@code EventService#get} 统一处理。
 */
@RestController
@RequestMapping(value = "/event")
@RequiredArgsConstructor
public class EventTournamentApi {
    private final TournamentService tournamentService;

    /**
     * 获取赛事积分榜与对阵视图。
     *
     * @param eventId 赛事 ID
     * @return 积分榜、赛程对阵与提示信息
     */
    @GetMapping(value = "/{eventId}/tournament")
    public Result<TournamentViewDTO> get(@PathVariable Long eventId) {
        TournamentViewDTO view = this.tournamentService.getView(eventId);
        if (view == null) {
            return Result.fail(404, "赛事不存在或已删除");
        }
        return Result.ok(view);
    }
}
