package com.supplymind.core.common;

/**
 * Holds the current request's tenant id, set by {@link TenantFilter}.
 * Will be replaced by a Keycloak JWT claim once identity is wired up (see README §2:
 * tenant identity must come from authenticated context, not a browser-supplied value).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
