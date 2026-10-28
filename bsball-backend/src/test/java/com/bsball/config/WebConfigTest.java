package com.bsball.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig：CORS 允许方法需覆盖后端已提供的 HTTP 方法")
class WebConfigTest {

    @Mock
    private CorsRegistry registry;

    @Mock
    private CorsRegistration registration;

    @Test
    @DisplayName("允许方法包含 PATCH（/auth/change-password 为 @PatchMapping）")
    void corsAllowsPatch() {
        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.allowCredentials(true)).thenReturn(registration);

        new WebConfig(Path.of(".")).addCorsMappings(registry);

        ArgumentCaptor<String[]> methods = ArgumentCaptor.forClass(String[].class);
        verify(registration).allowedMethods(methods.capture());
        List<String> allowed = Arrays.asList(methods.getValue());
        assertTrue(allowed.contains("PATCH"), "CORS 允许方法缺少 PATCH，实际：" + allowed);
    }
}
