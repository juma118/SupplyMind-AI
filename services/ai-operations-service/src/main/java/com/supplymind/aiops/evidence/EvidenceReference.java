package com.supplymind.aiops.evidence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_references")
public class EvidenceReference {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_id")
    private String sourceId;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getInvestigationId() {
        return investigationId;
    }

    public void setInvestigationId(UUID investigationId) {
        this.investigationId = investigationId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
