package com.supplymind.aiops.approval;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_approvals")
public class HumanApproval {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @PrePersist
    void onCreate() {
        decidedAt = Instant.now();
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

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(UUID recommendationId) {
        this.recommendationId = recommendationId;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecision decision) {
        this.decision = decision;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
