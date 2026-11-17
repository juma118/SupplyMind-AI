package com.supplymind.shipment.common;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped (not ThreadLocal) because RESTEasy Reactive can hop threads within a
 * single request - a ThreadLocal set by a filter on one thread would not be visible to
 * resource code running on another. Populated by TenantFilter.
 */
@RequestScoped
public class TenantContext {

    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String require() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
