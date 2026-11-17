package com.supplymind.shipment.shipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ShipmentEventPublisher {

    private static final Logger log = Logger.getLogger(ShipmentEventPublisher.class);

    @Inject
    @Channel("shipment-events-out")
    MutinyEmitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    public void publishDelayed(Shipment shipment, int delayHours) {
        var payload = Map.<String, Object>of(
                "productSku", shipment.productSku,
                "delayHours", delayHours,
                "warehouseCode", shipment.destinationWarehouse,
                "avgDailyDemand", shipment.avgDailyDemand);

        var envelope = new ShipmentEventEnvelope(
                UUID.randomUUID().toString(),
                "ShipmentDelayed",
                1,
                shipment.tenantId,
                Instant.now().toString(),
                shipment.shipmentCode,
                payload);

        try {
            String json = objectMapper.writeValueAsString(envelope);
            emitter.sendAndAwait(json);
        } catch (Exception e) {
            log.errorf(e, "Failed to publish ShipmentDelayed event for shipment %s", shipment.shipmentCode);
            throw new RuntimeException("Failed to publish ShipmentDelayed event", e);
        }
    }
}
