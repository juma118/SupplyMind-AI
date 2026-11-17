package com.supplymind.aiops.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Temporary stand-in for Keycloak-derived tenant resolution. Once auth is wired up,
 * this must read the tenant id from the validated JWT claim, not this header.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.set(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
