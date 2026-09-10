/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalPublicConfigKeys
 *  com.bsball.api.PortalSettingsApi
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.service.SysConfigService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.api.PortalPublicConfigKeys;
import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.service.SysConfigService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/portal/settings"})
public class PortalSettingsApi {
    private final SysConfigService sysConfigService;

    @GetMapping
    public Result<Map<String, Object>> get() {
        String s;
        Object v;
        Object cpObj;
        long tid = this.sysConfigService.effectiveTenantId(CurrentUserHolder.getTenantId());
        Map cfg = this.sysConfigService.getConfig(tid);
        LinkedHashMap out = new LinkedHashMap();
        for (String key : PortalPublicConfigKeys.ALL) {
            if (!cfg.containsKey(key)) continue;
            out.put(key, cfg.get(key));
        }
        Object ftObj = cfg.get("footerTextPortal");
        String mergedFooter = PortalSettingsApi.mergeFooterPortalHtml((String)(ftObj != null ? String.valueOf(ftObj) : ""), (String)((cpObj = cfg.get("copyrightPortal")) != null ? String.valueOf(cpObj) : null));
        if (!mergedFooter.isEmpty()) {
            out.put("footerTextPortal", mergedFooter);
        }
        boolean showPv = true;
        Object v2 = v = out.containsKey("publicViewCount") ? out.get("publicViewCount") : cfg.get("publicViewCount");
        if (v instanceof Boolean) {
            Boolean b = (Boolean)v;
            showPv = b;
        } else if (v != null && ("false".equalsIgnoreCase(s = v.toString().trim()) || "0".equals(s))) {
            showPv = false;
        }
        out.put("publicViewCount", showPv);
        boolean portalDevtoolsGuard = PortalSettingsApi.readBool((Map)cfg, out, (String)"portalDevtoolsGuard", (boolean)true);
        out.put("portalDevtoolsGuard", portalDevtoolsGuard);
        boolean subFallback = portalDevtoolsGuard;
        out.put("portalDevtoolsGuardOverlay", PortalSettingsApi.readBool((Map)cfg, out, (String)"portalDevtoolsGuardOverlay", (boolean)subFallback));
        out.put("portalDevtoolsGuardDebuggerTrap", PortalSettingsApi.readBool((Map)cfg, out, (String)"portalDevtoolsGuardDebuggerTrap", (boolean)subFallback));
        out.put("portalDevtoolsGuardCopyrightNotice", PortalSettingsApi.readBool((Map)cfg, out, (String)"portalDevtoolsGuardCopyrightNotice", (boolean)subFallback));
        return Result.ok(out);
    }

    private static boolean readBool(Map<String, Object> cfg, Map<String, Object> out, String key, boolean defaultVal) {
        Object v;
        Object object = v = out.containsKey(key) ? out.get(key) : cfg.get(key);
        if (v instanceof Boolean) {
            Boolean b = (Boolean)v;
            return b;
        }
        if (v != null) {
            String s = v.toString().trim();
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return false;
            }
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return true;
            }
        }
        return defaultVal;
    }

    private static String mergeFooterPortalHtml(String footerText, String copyrightPortal) {
        String cp;
        String ft = footerText != null ? footerText.trim() : "";
        String string = cp = copyrightPortal != null ? copyrightPortal.trim() : "";
        if (cp.isEmpty()) {
            return ft;
        }
        if (ft.contains(cp)) {
            return ft;
        }
        if (ft.isEmpty()) {
            return "<p>" + PortalSettingsApi.escapeHtml((String)cp) + "</p>";
        }
        return ft + "\n<p>" + PortalSettingsApi.escapeHtml((String)cp) + "</p>";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @Generated
    public PortalSettingsApi(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }
}

