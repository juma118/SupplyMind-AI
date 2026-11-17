package com.supplymind.shipment.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ShipmentDtos {

    private ShipmentDtos() {
    }

    public record CreateShipmentRequest(
            @NotBlank String shipmentCode,
            String carrier,
            String origin,
            @NotBlank String destinationWarehouse,
            @NotBlank String productSku,
            @NotNull @Positive Integer quantity,
            BigDecimal avgDailyDemand,
            Instant eta) {
    }

    public record AddEventRequest(
            @NotNull ShipmentEventType eventType,
            String description,
            Integer delayHours) {
    }

    public record WebhookPayload(
            @NotBlank String shipmentCode,
            @NotNull Integer delayHours,
            String description) {
    }

    public record ShipmentResponse(
            UUID id,
            String shipmentCode,
            String carrier,
            String origin,
            String destinationWarehouse,
            String productSku,
            int quantity,
            BigDecimal avgDailyDemand,
            String status,
            Instant eta,
            Instant createdAt,
            Instant updatedAt) {

        public static ShipmentResponse from(Shipment s) {
            return new ShipmentResponse(s.id, s.shipmentCode, s.carrier, s.origin, s.destinationWarehouse,
                    s.productSku, s.quantity, s.avgDailyDemand, s.status.name(), s.eta, s.createdAt, s.updatedAt);
        }
    }

    public record EventResponse(UUID id, String eventType, String description, Integer delayHours, Instant occurredAt) {
        public static EventResponse from(ShipmentEvent e) {
            return new EventResponse(e.id, e.eventType.name(), e.description, e.delayHours, e.occurredAt);
        }
    }

    // onTimeRate is null, not 0 or 1, when there's no delivered shipment yet - a rate of
    // zero would misleadingly read as "100% late" rather than "no data".
    public record OnTimeRateResponse(long totalDelivered, long onTimeDelivered, Double onTimeRate) {
    }
}
