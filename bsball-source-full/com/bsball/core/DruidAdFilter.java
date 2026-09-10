/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.DruidAdFilter
 *  com.bsball.core.DruidAdFilter$BufferingResponseWrapper
 *  jakarta.servlet.Filter
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.bsball.core.DruidAdFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value=-2147483638)
@ConditionalOnProperty(name={"spring.datasource.druid.stat-view-servlet.enabled"}, havingValue="true")
public class DruidAdFilter
implements Filter {
    private static final String DRUID_COMMON_JS = "druid/js/common.js";
    private static final String BUILD_FOOTER = "this.buildFooter();";
    private static final String BUILD_FOOTER_COMMENTED = "//this.buildFooter();";

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        String uri = req.getRequestURI();
        if (uri == null || !uri.contains(DRUID_COMMON_JS)) {
            chain.doFilter(request, response);
            return;
        }
        BufferingResponseWrapper wrapper = new BufferingResponseWrapper(res);
        chain.doFilter(request, (ServletResponse)wrapper);
        String content = wrapper.getContent();
        if (content != null && content.contains(BUILD_FOOTER)) {
            content = content.replace(BUILD_FOOTER, BUILD_FOOTER_COMMENTED);
        }
        if (content != null) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            res.setContentLength(bytes.length);
            res.getOutputStream().write(bytes);
        }
    }
}

