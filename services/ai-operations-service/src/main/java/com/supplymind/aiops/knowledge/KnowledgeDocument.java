package com.supplymind.aiops.knowledge;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private KnowledgeSourceType sourceType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private boolean embedded;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public KnowledgeSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(KnowledgeSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
