package com.supplymind.operations.inventory;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only ledger row - per README §7 data-integrity principles, movements are never
 * updated or deleted; balances are derived by applying movements transactionally.
 */
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseCode;

    @Column(name = "product_id", nullable = false)
    private String productSku;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        occurredAt = Instant.now();
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

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
