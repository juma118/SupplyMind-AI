package com.supplymind.aiops.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.execution.OperationsServiceClient;
import com.supplymind.aiops.investigation.InvestigationRepository;
import com.supplymind.aiops.investigation.InvestigationWorkflowService;
import com.supplymind.aiops.investigation.ShipmentDelayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes shipment.events.v1 (README §9) and turns a ShipmentDelayed event into an
 * investigation - this is the real event-driven entry point; the REST endpoint on
 * InvestigationController is the manual/demo equivalent of the same call.
 *
 * Idempotency: duplicate delivery of the same shipment delay is tolerated by skipping
 * creation if an investigation already exists for that shipment id (README §9: "design
 * consumers to tolerate duplicate delivery"). This is a simplification - a production
 * version would key on eventId via an inbox table, not aggregateId, since a shipment can
 * legitimately be delayed more than once.
 */
@Component
public class ShipmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventListener.class);

    private final ObjectMapper objectMapper;
    private final InvestigationWorkflowService workflowService;
    private final InvestigationRepository investigationRepository;
    private final OperationsServiceClient operationsServiceClient;

    public ShipmentEventListener(ObjectMapper objectMapper, InvestigationWorkflowService workflowService,
                                  InvestigationRepository investigationRepository,
                                  OperationsServiceClient operationsServiceClient) {
        this.objectMapper = objectMapper;
        this.workflowService = workflowService;
        this.investigationRepository = investigationRepository;
        this.operationsServiceClient = operationsServiceClient;
    }

    @KafkaListener(topics = "shipment.events.v1", groupId = "ai-operations-service")
    public void onShipmentEvent(String message) {
        ShipmentEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, ShipmentEventEnvelope.class);
        } catch (Exception e) {
            log.error("Could not parse shipment event envelope, skipping: {}", e.getMessage());
            return;
        }

        if (!"ShipmentDelayed".equals(envelope.eventType())) {
            log.debug("Ignoring shipment event type {}", envelope.eventType());
            return;
        }

        try {
            TenantContext.set(envelope.tenantId());

            if (investigationRepository.existsByTenantIdAndSourceReference(envelope.tenantId(), envelope.aggregateId())) {
                log.info("Investigation already exists for shipment {}, skipping duplicate event {}",
                        envelope.aggregateId(), envelope.eventId());
                return;
            }

            ShipmentDelayContext context = toContext(envelope);
            workflowService.runShipmentDelayInvestigation(context);
            log.info("Investigation created from event {} for shipment {}", envelope.eventId(), envelope.aggregateId());
        } catch (Exception e) {
            log.error("Failed to process ShipmentDelayed event {}: {}", envelope.eventId(), e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * currentStockUnits is deliberately NOT trusted from the event payload - a real
     * shipment-tracking producer (Shipment Service) has no business knowing warehouse
     * inventory levels, so this always asks Operations Service for the live balance.
     * A payload-supplied value is only honored as a manual-test convenience (e.g. events
     * produced directly via rpk during development); avgDailyDemand still comes from the
     * payload since no demand-forecasting module exists yet (README §6).
     */
    private ShipmentDelayContext toContext(ShipmentEventEnvelope envelope) {
        var payload = envelope.payload();
        String productSku = (String) payload.get("productSku");
        String warehouseCode = (String) payload.get("warehouseCode");

        int currentStockUnits;
        if (payload.get("currentStockUnits") != null) {
            currentStockUnits = ((Number) payload.get("currentStockUnits")).intValue();
        } else {
            currentStockUnits = operationsServiceClient.listWarehouseBalances(envelope.tenantId(), productSku).stream()
                    .filter(b -> b.warehouseCode().equals(warehouseCode))
                    .findFirst()
                    .map(OperationsServiceClient.WarehouseBalance::availableUnits)
                    .orElse(0);
        }

        return new ShipmentDelayContext(
                envelope.aggregateId(),
                productSku,
                ((Number) payload.get("delayHours")).intValue(),
                warehouseCode,
                currentStockUnits,
                ((Number) payload.get("avgDailyDemand")).doubleValue());
    }
}
