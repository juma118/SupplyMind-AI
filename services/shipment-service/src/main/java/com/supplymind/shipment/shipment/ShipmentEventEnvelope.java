package com.supplymind.shipment.shipment;

import java.util.Map;

/**
 * Must stay structurally compatible with ai-operations-service's ShipmentEventEnvelope
 * (README §9 standard event envelope) since that's the real consumer on shipment.events.v1.
 * These two records are duplicated across services deliberately - no shared library - each
 * service owns its own copy of the contract it depends on, per the microservice boundary.
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
