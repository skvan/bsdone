/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.DruidEntryController
 *  com.bsball.core.DruidEntryTokenCache
 *  jakarta.servlet.http.HttpServletResponse
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestParam
 */
package com.bsball.api;

import com.bsball.core.DruidEntryTokenCache;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*
 * Exception performing whole class analysis ignored.
 */
@Controller
@ConditionalOnProperty(name={"spring.datasource.druid.stat-view-servlet.enabled"}, havingValue="true")
public class DruidEntryController {
    private final DruidEntryTokenCache tokenCache;
    @Value(value="${server.servlet.context-path:}")
    private String contextPath;
    @Value(value="${spring.datasource.druid.stat-view-servlet.login-username:admin}")
    private String loginUsername;
    @Value(value="${spring.datasource.druid.stat-view-servlet.login-password:admin}")
    private String loginPassword;

    @GetMapping(value={"/druid-entry"}, produces={"text/html"})
    public void druidEntry(@RequestParam(required=false) String token, HttpServletResponse response) throws IOException {
        String base = this.contextPath == null || this.contextPath.isEmpty() || "/".equals(this.contextPath) ? "" : this.contextPath;
        String loginUrl = base + "/druid/login.html";
        if (token == null || token.isBlank() || !this.tokenCache.consume(token)) {
            response.sendRedirect(loginUrl);
            return;
        }
        String submitUrl = base + "/druid/submitLogin";
        String indexUrl = base + "/druid/index.html";
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>\u8df3\u8f6c\u4e2d...</title></head><body>\n<p>\u6b63\u5728\u767b\u5f55\u6570\u636e\u76d1\u63a7...</p>\n<script>\n(function(){\n  var u = %s, p = %s, submitUrl = %s, indexUrl = %s, loginUrl = %s;\n  var body = 'loginUsername=' + encodeURIComponent(u) + '&loginPassword=' + encodeURIComponent(p);\n  fetch(submitUrl, {\n    method: 'POST',\n    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n    body: body,\n    credentials: 'same-origin'\n  }).then(function(r) { return r.text(); }).then(function(t) {\n    if (t && String(t).trim().toLowerCase().indexOf('success') >= 0) location.href = indexUrl;\n    else document.body.innerHTML = '<p>\u767b\u5f55\u5931\u8d25\uff0c\u8bf7<a href=\"' + loginUrl + '\">\u624b\u52a8\u767b\u5f55</a>\u3002\u54cd\u5e94: ' + (t || '(\u7a7a)') + '</p>';\n  }).catch(function(e) {\n    document.body.innerHTML = '<p>\u767b\u5f55\u5931\u8d25\uff0c\u8bf7<a href=\"' + loginUrl + '\">\u624b\u52a8\u767b\u5f55</a>\u3002' + (e && e.message ? e.message : '') + '</p>';\n  });\n})();\n</script>\n</body></html>\n".formatted(new Object[]{DruidEntryController.jsonQuote((String)this.loginUsername), DruidEntryController.jsonQuote((String)this.loginPassword), DruidEntryController.jsonQuote((String)submitUrl), DruidEntryController.jsonQuote((String)indexUrl), DruidEntryController.jsonQuote((String)loginUrl)});
        response.setContentType("text/html;charset=" + StandardCharsets.UTF_8.name());
        response.getWriter().write(html);
    }

    private static String escapeHtmlAttr(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String jsonQuote(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    @Generated
    public DruidEntryController(DruidEntryTokenCache tokenCache) {
        this.tokenCache = tokenCache;
    }
}

