package com.supplymind.shipment.common;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TenantFilter implements ContainerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            tenantContext.setTenantId(tenantId);
        }
    }
}
