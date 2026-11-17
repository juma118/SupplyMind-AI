package com.supplymind.shipment.shipment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(name = "shipment_code", nullable = false)
    public String shipmentCode;

    public String carrier;

    public String origin;

    @Column(name = "destination_warehouse", nullable = false)
    public String destinationWarehouse;

    @Column(name = "product_sku", nullable = false)
    public String productSku;

    @Column(nullable = false)
    public int quantity;

    @Column(name = "avg_daily_demand")
    public BigDecimal avgDailyDemand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ShipmentStatus status;

    public Instant eta;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ShipmentStatus.BOOKED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
