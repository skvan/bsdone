package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Event;
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
@DisplayName("EventService：赛制与积分制配置（Issue #665）")
class EventServiceTournamentConfigTest {

    private static final long TENANT_ID = 10L;
    private static final long EVENT_ID = 5L;

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
    @Mock
    private EventSpecialRuleService eventSpecialRuleService;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, tenantQueryPolicyService, gameRepository, leagueSpecialRuleService,
                eventSpecialRuleService);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    @DisplayName("新建赛事：赛制/积分制/小组/签表字段落默认值")
    void create_defaultsTournamentConfig() {
        Event event = new Event();
        event.setName("测试赛事");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event saved = service.create(event);

        assertEquals("SINGLE_ROUND_ROBIN", saved.getFormatType());
        assertEquals(2, saved.getPointsWin());
        assertEquals(1, saved.getPointsDraw());
        assertEquals(0, saved.getPointsLoss());
        assertEquals(2, saved.getGroupCount());
        assertEquals(2, saved.getAdvancePerGroup());
        assertEquals(Boolean.TRUE, saved.getThirdPlace());
        assertEquals(8, saved.getBracketSize());
        assertEquals(TENANT_ID, saved.getTenantId());
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("新建赛事：3-1-0 积分制与自定义小组配置保留")
    void create_customConfigPreserved() {
        Event event = new Event();
        event.setName("测试赛事");
        event.setFormatType("GROUP_KNOCKOUT");
        event.setPointsWin(3);
        event.setPointsDraw(1);
        event.setPointsLoss(0);
        event.setGroupCount(4);
        event.setAdvancePerGroup(3);
        event.setThirdPlace(false);
        event.setBracketSize(16);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event saved = service.create(event);

        assertEquals("GROUP_KNOCKOUT", saved.getFormatType());
        assertEquals(3, saved.getPointsWin());
        assertEquals(4, saved.getGroupCount());
        assertEquals(3, saved.getAdvancePerGroup());
        assertEquals(Boolean.FALSE, saved.getThirdPlace());
        assertEquals(16, saved.getBracketSize());
    }

    @Test
    @DisplayName("新建赛事：非法赛制返回 400 且不落库")
    void create_invalidFormat_throws400() {
        Event event = new Event();
        event.setName("测试赛事");
        event.setFormatType("UNKNOWN");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(event));

        assertEquals(400, error.getCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("编辑赛事：缺省赛制字段沿用存量，避免旧客户端重置")
    void update_missingConfigKeepsExisting() {
        Event existing = new Event();
        existing.setId(EVENT_ID);
        existing.setTenantId(TENANT_ID);
        existing.setFormatType("GROUP_KNOCKOUT");
        existing.setPointsWin(3);
        existing.setPointsDraw(1);
        existing.setPointsLoss(0);
        existing.setGroupCount(4);
        existing.setAdvancePerGroup(2);
        existing.setThirdPlace(true);
        existing.setBracketSize(16);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Event incoming = new Event();
        incoming.setName("改名赛事");

        Event saved = service.update(EVENT_ID, incoming);

        assertEquals("GROUP_KNOCKOUT", saved.getFormatType());
        assertEquals(3, saved.getPointsWin());
        assertEquals(4, saved.getGroupCount());
        assertEquals(2, saved.getAdvancePerGroup());
        assertEquals(Boolean.TRUE, saved.getThirdPlace());
        assertEquals(16, saved.getBracketSize());
    }
}
