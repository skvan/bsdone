/*
 * 赛事结束校验（Issue #318 决策 7）：复用「编辑赛事→状态」入口，置为 ended 前须该赛事全部比赛已终态。
 *
 * 语义：非 ended → ended 且存在未删未终态（非 final 且非 cancelled，含 NULL）比赛 → 400
 * 「存在未结束的比赛，无法结束赛事」且无写（口径与球队解散守卫 countPendingGamesByTeamId 一致）；
 * 全部终态 → 放行；已有 ended 再保存 / 反向（ended→其它）不拦（保持可修正）。
 * 另：requireRoster（报名制标记）创建时定型（决策 11），编辑一律沿用存量值（防请求体缺省 TRUE 静默翻转）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；EventService 手工 new（@Generated 构造器）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.League;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("赛事结束校验：全部比赛 final 才可置 ended（Issue #318 决策 7）")
class EventEndValidationRuleTest {

    private static final long TENANT_ID = 10L;
    private static final long EVENT_ID = 5L;
    private static final long LEAGUE_ID = 1L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private LeagueSpecialRuleService leagueSpecialRuleService;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, tenantQueryPolicyService, gameRepository, leagueSpecialRuleService,
                org.mockito.Mockito.mock(EventSpecialRuleService.class));
    }

    @Test
    @DisplayName("结束拦截：存在未 final 比赛 → 400「存在未结束的比赛，无法结束赛事」且无写")
    void update_toEnded_hasNotFinalGames_400_noWrite() {
        stubExisting("ongoing");
        when(gameRepository.countUnfinishedGamesByEvent(EVENT_ID)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(EVENT_ID, payload("ended")));

        assertEquals(400, ex.getCode());
        assertEquals("存在未结束的比赛，无法结束赛事", ex.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("结束放行：全部比赛 final → 落库 ended")
    void update_toEnded_allFinal_saves() {
        stubExisting("ongoing");
        when(gameRepository.countUnfinishedGamesByEvent(EVENT_ID)).thenReturn(0L);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event saved = service.update(EVENT_ID, payload("ended"));

        assertEquals("ended", saved.getStatus());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("不拦：已有 ended 再保存（幂等）/ 反向 ended→ongoing 可修正 → 不查比赛计数")
    void update_alreadyEndedOrReverse_skips() {
        stubExisting("ended");
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(EVENT_ID, payload("ended"));
        service.update(EVENT_ID, payload("ongoing"));

        verify(gameRepository, never()).countUnfinishedGamesByEvent(anyLong());
    }

    @Test
    @DisplayName("报名制标记：编辑沿用存量 require_roster（请求体缺省 TRUE 不得把存量 false 赛事翻转）")
    void update_requireRosterInheritedFromExisting() {
        stubExisting("ongoing", Boolean.FALSE);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        // payload 未设置 → 实体默认 TRUE，编辑后必须仍为存量 FALSE
        Event saved = service.update(EVENT_ID, payload("ongoing"));

        assertEquals(Boolean.FALSE, saved.getRequireRoster());
    }

    private void stubExisting(String status) {
        stubExisting(status, null);
    }

    private void stubExisting(String status, Boolean requireRoster) {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = new Event();
        existing.setId(EVENT_ID);
        existing.setTenantId(TENANT_ID);
        existing.setStatus(status);
        existing.setLeagueId(LEAGUE_ID);
        if (requireRoster != null) {
            existing.setRequireRoster(requireRoster);
        }
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));
    }

    private static Event payload(String status) {
        Event e = new Event();
        e.setStatus(status);
        return e;
    }

    private static League league(long id, long tenantId) {
        League l = new League();
        l.setId(id);
        l.setTenantId(tenantId);
        l.setName("联盟" + id);
        return l;
    }
}
