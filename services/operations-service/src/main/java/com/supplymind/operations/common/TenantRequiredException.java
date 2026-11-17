package com.supplymind.operations.common;

public class TenantRequiredException extends RuntimeException {

    public TenantRequiredException() {
        super("Tenant context is required for this request");
    }
}
