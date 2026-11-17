package com.supplymind.aiops.common;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String tenantId, String actor, String action, String entityType, String entityId, String details) {
        repository.save(AuditLog.of(tenantId, actor, action, entityType, entityId, details));
    }
}
