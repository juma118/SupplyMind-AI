package com.supplymind.shipment.shipment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MutinyEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * This payload is a cross-service contract, not just this service's own concern:
 * ai-operations-service's ShipmentEventListener.toContext() reads these exact field names
 * out of the "payload" map (productSku, delayHours, warehouseCode, avgDailyDemand) and the
 * envelope's top-level fields (eventType, tenantId, aggregateId). A field rename on either
 * side without the other breaks event consumption with no compile-time warning, since both
 * services deserialize into their own independent copy of this envelope shape.
 */
class ShipmentEventPublisherTest {

    private MutinyEmitter<String> emitter;
    private ShipmentEventPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        emitter = mock(MutinyEmitter.class);
        publisher = new ShipmentEventPublisher();
        publisher.emitter = emitter;
        publisher.objectMapper = new ObjectMapper();
    }

    @Test
    void publishesAnEnvelopeWithTheExactFieldNamesAiOperationsServiceExpects() throws Exception {
        Shipment shipment = new Shipment();
        shipment.tenantId = "tenant-demo";
        shipment.shipmentCode = "SHP-1";
        shipment.productSku = "WID-2200";
        shipment.destinationWarehouse = "WH-A";
        shipment.avgDailyDemand = new BigDecimal("1200.0");

        publisher.publishDelayed(shipment, 72);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emitter).sendAndAwait(captor.capture());

        JsonNode json = new ObjectMapper().readTree(captor.getValue());
        assertThat(json.get("eventType").asText()).isEqualTo("ShipmentDelayed");
        assertThat(json.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(json.get("tenantId").asText()).isEqualTo("tenant-demo");
        assertThat(json.get("aggregateId").asText()).isEqualTo("SHP-1");
        assertThat(UUID.fromString(json.get("eventId").asText())).isNotNull();

        JsonNode payload = json.get("payload");
        assertThat(payload.get("productSku").asText()).isEqualTo("WID-2200");
        assertThat(payload.get("delayHours").asInt()).isEqualTo(72);
        assertThat(payload.get("warehouseCode").asText()).isEqualTo("WH-A");
        assertThat(payload.get("avgDailyDemand").asDouble()).isEqualTo(1200.0);
    }

    @Test
    void wrapsAndRethrowsWhenTheEmitterFails() {
        Shipment shipment = new Shipment();
        shipment.tenantId = "tenant-demo";
        shipment.shipmentCode = "SHP-1";
        shipment.productSku = "WID-2200";
        shipment.destinationWarehouse = "WH-A";
        shipment.avgDailyDemand = new BigDecimal("1200.0");
        doThrow(new RuntimeException("kafka unreachable")).when(emitter).sendAndAwait(anyString());

        assertThatThrownBy(() -> publisher.publishDelayed(shipment, 72))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish ShipmentDelayed event");
    }
}
