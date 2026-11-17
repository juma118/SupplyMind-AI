package com.supplymind.aiops.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByTenantIdOrderByOccurredAtDesc(String tenantId);

    List<AuditLog> findAllByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, String entityId);
}
