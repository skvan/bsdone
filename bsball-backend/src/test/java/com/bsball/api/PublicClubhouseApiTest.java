package com.bsball.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.Result;
import com.bsball.service.ClubhouseService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class PublicClubhouseApiTest {
    @Mock
    private ClubhouseService clubhouseService;

    @InjectMocks
    private PublicClubhouseApi api;

    @Test
    @DisplayName("公开主页聚合接口透传范围并禁止缓存")
    void dashboardDelegatesAndDisablesCache() {
        Map<String, Object> data = Map.of("player", Map.of("id", 10L));
        when(clubhouseService.getPublicDashboard(10L, 1L, 2L, 3L, "BASEBALL")).thenReturn(data);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<Map<String, Object>> result = api.dashboard(10L, 1L, 2L, 3L, "BASEBALL", response);

        assertEquals(200, result.getCode());
        assertEquals(data, result.getData());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        verify(clubhouseService).getPublicDashboard(10L, 1L, 2L, 3L, "BASEBALL");
    }

    @Test
    @DisplayName("公开里程碑接口透传球员并禁止缓存")
    void milestonesDelegatesAndDisablesCache() {
        Map<String, Object> data = Map.of("available", true);
        when(clubhouseService.getPublicMilestones(10L)).thenReturn(data);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<Map<String, Object>> result = api.milestones(10L, response);

        assertEquals(200, result.getCode());
        assertEquals(data, result.getData());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        verify(clubhouseService).getPublicMilestones(10L);
    }

    @Test
    @DisplayName("公开打击落点接口透传范围并禁止缓存")
    void spraysDelegatesAndDisablesCache() {
        Map<String, Object> data = Map.of("zones", Map.of());
        when(clubhouseService.getPublicSprays(10L, 1L, 2L, 3L, "BASEBALL")).thenReturn(data);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Result<Map<String, Object>> result = api.sprays(10L, 1L, 2L, 3L, "BASEBALL", response);

        assertEquals(200, result.getCode());
        assertEquals(data, result.getData());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        verify(clubhouseService).getPublicSprays(10L, 1L, 2L, 3L, "BASEBALL");
    }
}
