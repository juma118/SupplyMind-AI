package com.supplymind.operations.inventory;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * warehouseCode/productSku are the Core Service's natural keys (e.g. "WH-A", "WID-2200"),
 * not foreign keys - these are separate deployable services (README §4) with no shared
 * database, so referential integrity across them is enforced in application code, not SQL.
 */
@Entity
@Table(name = "inventory_balances")
public class InventoryBalance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseCode;

    @Column(name = "product_id", nullable = false)
    private String productSku;

    @Column(name = "available_units", nullable = false)
    private int availableUnits;

    @Column(name = "reserved_units", nullable = false)
    private int reservedUnits;

    @Column(name = "incoming_units", nullable = false)
    private int incomingUnits;

    @Column(name = "damaged_units", nullable = false)
    private int damagedUnits;

    @Column(name = "reorder_point")
    private Integer reorderPoint;

    @Column(name = "avg_daily_demand")
    private java.math.BigDecimal avgDailyDemand;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "safety_stock")
    private Integer safetyStock;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
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

    public int getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(int availableUnits) {
        this.availableUnits = availableUnits;
    }

    public int getReservedUnits() {
        return reservedUnits;
    }

    public void setReservedUnits(int reservedUnits) {
        this.reservedUnits = reservedUnits;
    }

    public int getIncomingUnits() {
        return incomingUnits;
    }

    public void setIncomingUnits(int incomingUnits) {
        this.incomingUnits = incomingUnits;
    }

    public int getDamagedUnits() {
        return damagedUnits;
    }

    public void setDamagedUnits(int damagedUnits) {
        this.damagedUnits = damagedUnits;
    }

    public Integer getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public java.math.BigDecimal getAvgDailyDemand() {
        return avgDailyDemand;
    }

    public void setAvgDailyDemand(java.math.BigDecimal avgDailyDemand) {
        this.avgDailyDemand = avgDailyDemand;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public Integer getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(Integer safetyStock) {
        this.safetyStock = safetyStock;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
