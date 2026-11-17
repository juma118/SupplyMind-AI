package com.supplymind.aiops.investigation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Input payload for the primary demo scenario (README §2): a delayed shipment that may
 * cause a stockout at the destination warehouse. In production this is populated from a
 * ShipmentDelayed Kafka event; here it is accepted directly over REST for the thin slice.
 */
public record ShipmentDelayContext(
        @NotBlank String shipmentId,
        @NotBlank String productSku,
        @NotNull @Positive Integer delayHours,
        @NotBlank String warehouseCode,
        @NotNull @Positive Integer currentStockUnits,
        @NotNull @Positive Double avgDailyDemand) {
}
