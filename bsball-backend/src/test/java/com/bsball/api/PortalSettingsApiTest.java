package com.bsball.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * PortalSettingsApi 页脚版本占位符替换单元测试（Issue #123）。
 * 覆盖：单/多占位符替换、无占位符原样、版本空值降级、HTML 转义、null 输入。
 */
class PortalSettingsApiTest {

    @Test
    void replacesSinglePlaceholder() {
        assertEquals("© 2026 BSD · v1.1.1", PortalSettingsApi.applyVersionPlaceholder("© 2026 BSD · v{{version}}", "1.1.1"));
    }

    @Test
    void replacesMultiplePlaceholders() {
        assertEquals("v2.0.0 / v2.0.0", PortalSettingsApi.applyVersionPlaceholder("v{{version}} / v{{version}}", "2.0.0"));
    }

    @Test
    void keepsTextWithoutPlaceholder() {
        assertEquals("© 2026 BSD", PortalSettingsApi.applyVersionPlaceholder("© 2026 BSD", "1.1.1"));
    }

    @Test
    void keepsTextWhenVersionBlank() {
        assertEquals("v{{version}}", PortalSettingsApi.applyVersionPlaceholder("v{{version}}", "   "));
    }

    @Test
    void keepsTextWhenVersionNull() {
        assertEquals("v{{version}}", PortalSettingsApi.applyVersionPlaceholder("v{{version}}", null));
    }

    @Test
    void trimsVersion() {
        assertEquals("v1.1.1", PortalSettingsApi.applyVersionPlaceholder("v{{version}}", "  1.1.1  "));
    }

    @Test
    void escapesHtmlInVersion() {
        assertEquals("v&lt;script&gt;&amp;&quot;", PortalSettingsApi.applyVersionPlaceholder("v{{version}}", "<script>&\""));
    }

    @Test
    void handlesNullText() {
        assertEquals("", PortalSettingsApi.applyVersionPlaceholder(null, "1.1.1"));
    }

    @Test
    void handlesEmptyText() {
        assertEquals("", PortalSettingsApi.applyVersionPlaceholder("", "1.1.1"));
    }

    @Test
    void keepsTextWhenVersionIsUnfilteredPlaceholder() {
        assertEquals("v{{version}}", PortalSettingsApi.applyVersionPlaceholder("v{{version}}", "@app.release.version@"));
    }

    @Test
    void ignoresOtherBraces() {
        assertEquals("{version} 1.1.1", PortalSettingsApi.applyVersionPlaceholder("{version} {{version}}", "1.1.1"));
    }
}
