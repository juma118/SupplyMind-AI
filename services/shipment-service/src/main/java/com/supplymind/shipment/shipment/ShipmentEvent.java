package com.supplymind.shipment.shipment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(name = "shipment_id", nullable = false)
    public UUID shipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    public ShipmentEventType eventType;

    public String description;

    @Column(name = "delay_hours")
    public Integer delayHours;

    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
