/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.druid.support.jakarta.StatViewServlet
 *  com.alibaba.druid.support.jakarta.WebStatFilter
 *  com.bsball.config.DruidMonitorConfig
 *  jakarta.servlet.Filter
 *  jakarta.servlet.Servlet
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.boot.web.servlet.FilterRegistrationBean
 *  org.springframework.boot.web.servlet.ServletRegistrationBean
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.bsball.config;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name={"spring.datasource.druid.stat-view-servlet.enabled"}, havingValue="true")
public class DruidMonitorConfig {
    @Value(value="${spring.datasource.druid.stat-view-servlet.login-username:admin}")
    private String loginUsername;
    @Value(value="${spring.datasource.druid.stat-view-servlet.login-password:admin}")
    private String loginPassword;
    @Value(value="${spring.datasource.druid.stat-view-servlet.allow:}")
    private String allow;
    @Value(value="${spring.datasource.druid.stat-view-servlet.reset-enable:false}")
    private boolean resetEnable;

    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean bean = new ServletRegistrationBean((Servlet)new StatViewServlet(), new String[]{"/druid/*"});
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("loginUsername", this.loginUsername);
        params.put("loginPassword", this.loginPassword);
        if (this.allow != null && !this.allow.isBlank()) {
            params.put("allow", this.allow);
        }
        params.put("resetEnable", String.valueOf(this.resetEnable));
        bean.setInitParameters(params);
        return bean;
    }

    @Bean
    @ConditionalOnProperty(name={"spring.datasource.druid.web-stat-filter.enabled"}, havingValue="true")
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter(@Value(value="${spring.datasource.druid.web-stat-filter.exclusions:*.js,*.css,*.gif,*.png,*.jpg,*.ico,/druid/*}") String exclusions) {
        FilterRegistrationBean bean = new FilterRegistrationBean((Filter)new WebStatFilter(), new ServletRegistrationBean[0]);
        bean.addUrlPatterns(new String[]{"/*"});
        bean.addInitParameter("exclusions", exclusions);
        return bean;
    }
}

