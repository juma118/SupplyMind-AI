package com.supplymind.aiops.events;

import java.util.Map;

/**
 * Standard event envelope from README §9 (shipment.events.v1). aggregateId is the
 * shipment id; payload carries the event-specific fields (e.g. delayHours, warehouseCode).
 */
public record ShipmentEventEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        String tenantId,
        String occurredAt,
        String aggregateId,
        Map<String, Object> payload) {
}
