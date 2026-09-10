/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.ResolvedTenantHolder
 *  com.bsball.core.TenantResolveFilter
 *  com.bsball.service.TenantResolutionService
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  lombok.Generated
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 *  org.springframework.web.filter.OncePerRequestFilter
 */
package com.bsball.core;

import com.bsball.core.ResolvedTenantHolder;
import com.bsball.service.TenantResolutionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.Generated;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(value=-2147483633)
public class TenantResolveFilter
extends OncePerRequestFilter {
    private final TenantResolutionService tenantResolutionService;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Long tid = this.tenantResolutionService.resolve(request);
            ResolvedTenantHolder.set((Long)tid);
            filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
        }
        finally {
            ResolvedTenantHolder.clear();
        }
    }

    @Generated
    public TenantResolveFilter(TenantResolutionService tenantResolutionService) {
        this.tenantResolutionService = tenantResolutionService;
    }
}

