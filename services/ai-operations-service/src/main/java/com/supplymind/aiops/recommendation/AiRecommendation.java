package com.supplymind.aiops.recommendation;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
public class AiRecommendation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(columnDefinition = "text", nullable = false)
    private String summary;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private RecommendedActionType actionType = RecommendedActionType.NONE;

    @Column(name = "action_product_sku")
    private String actionProductSku;

    @Column(name = "action_quantity")
    private Integer actionQuantity;

    @Column(name = "action_source_warehouse")
    private String actionSourceWarehouse;

    @Column(name = "action_dest_warehouse")
    private String actionDestWarehouse;

    @Column(name = "action_supplier_id")
    private String actionSupplierId;

    @Column(name = "action_unit_price")
    private BigDecimal actionUnitPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = RecommendationStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public RecommendedActionType getActionType() {
        return actionType;
    }

    public void setActionType(RecommendedActionType actionType) {
        this.actionType = actionType;
    }

    public String getActionProductSku() {
        return actionProductSku;
    }

    public void setActionProductSku(String actionProductSku) {
        this.actionProductSku = actionProductSku;
    }

    public Integer getActionQuantity() {
        return actionQuantity;
    }

    public void setActionQuantity(Integer actionQuantity) {
        this.actionQuantity = actionQuantity;
    }

    public String getActionSourceWarehouse() {
        return actionSourceWarehouse;
    }

    public void setActionSourceWarehouse(String actionSourceWarehouse) {
        this.actionSourceWarehouse = actionSourceWarehouse;
    }

    public String getActionDestWarehouse() {
        return actionDestWarehouse;
    }

    public void setActionDestWarehouse(String actionDestWarehouse) {
        this.actionDestWarehouse = actionDestWarehouse;
    }

    public String getActionSupplierId() {
        return actionSupplierId;
    }

    public void setActionSupplierId(String actionSupplierId) {
        this.actionSupplierId = actionSupplierId;
    }

    public BigDecimal getActionUnitPrice() {
        return actionUnitPrice;
    }

    public void setActionUnitPrice(BigDecimal actionUnitPrice) {
        this.actionUnitPrice = actionUnitPrice;
    }
}
