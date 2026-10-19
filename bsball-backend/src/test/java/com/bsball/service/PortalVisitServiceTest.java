package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.model.entity.PortalVisitHit;
import com.bsball.repository.PortalVisitHitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalVisitService：埋点来源白名单")
class PortalVisitServiceTest {

    @Mock
    PortalVisitHitRepository repository;
    @Mock
    IpLocationCacheService ipLocationCacheService;
    @Mock
    ApiPermissionService apiPermissionService;

    PortalVisitService service;

    @BeforeEach
    void setUp() {
        service = new PortalVisitService(repository, new TenantProperties(), ipLocationCacheService, apiPermissionService);
    }

    private PortalVisitHit record(String source) {
        service.recordAsync("visitor-1", "/team1/players/9", "1.2.3.4", 1L, "UA", source);
        ArgumentCaptor<PortalVisitHit> captor = ArgumentCaptor.forClass(PortalVisitHit.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("合法来源 share/poster 落库")
    void validSources() {
        assertEquals("share", record("share").getSource());
        reset(repository);
        assertEquals("poster", record("poster").getSource());
    }

    @Test
    @DisplayName("非法/超长/空白/缺失来源 → null 且照常保存")
    void invalidSources() {
        assertNull(record("email").getSource());
        reset(repository);
        assertNull(record("a".repeat(33)).getSource());
        reset(repository);
        assertNull(record("  ").getSource());
        reset(repository);
        assertNull(record(null).getSource());
    }
}
