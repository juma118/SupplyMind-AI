package com.supplymind.aiops.common;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, append-only audit record. Never updated after insert.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        occurredAt = Instant.now();
    }

    public static AuditLog of(String tenantId, String actor, String action, String entityType,
                               String entityId, String details) {
        AuditLog log = new AuditLog();
        log.tenantId = tenantId;
        log.actor = actor;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.details = details;
        return log;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
