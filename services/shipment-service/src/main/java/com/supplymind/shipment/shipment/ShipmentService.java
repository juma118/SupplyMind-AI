package com.supplymind.shipment.shipment;

import com.supplymind.shipment.common.NotFoundException;
import com.supplymind.shipment.common.TenantContext;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ShipmentService {

    @Inject
    TenantContext tenantContext;

    @Inject
    ShipmentEventPublisher eventPublisher;

    public List<Shipment> list() {
        return Shipment.list("tenantId", tenantContext.require());
    }

    public List<Shipment> atRisk() {
        return Shipment.list("tenantId = ?1 and status = ?2", tenantContext.require(), ShipmentStatus.DELAYED);
    }

    public Shipment get(UUID id) {
        Shipment shipment = Shipment.findById(id);
        if (shipment == null || !shipment.tenantId.equals(tenantContext.require())) {
            throw new NotFoundException("Shipment not found: " + id);
        }
        return shipment;
    }

    @Transactional
    public Shipment create(ShipmentDtos.CreateShipmentRequest request) {
        String tenantId = tenantContext.require();
        if (Shipment.count("tenantId = ?1 and shipmentCode = ?2", tenantId, request.shipmentCode()) > 0) {
            throw new IllegalArgumentException("Shipment code already exists for this tenant: " + request.shipmentCode());
        }

        Shipment shipment = new Shipment();
        shipment.tenantId = tenantId;
        shipment.shipmentCode = request.shipmentCode();
        shipment.carrier = request.carrier();
        shipment.origin = request.origin();
        shipment.destinationWarehouse = request.destinationWarehouse();
        shipment.productSku = request.productSku();
        shipment.quantity = request.quantity();
        shipment.avgDailyDemand = request.avgDailyDemand();
        shipment.eta = request.eta();
        shipment.status = ShipmentStatus.BOOKED;
        shipment.persist();

        recordEvent(tenantId, shipment.id, ShipmentEventType.BOOKED, "Shipment booked", null);
        return shipment;
    }

    @Transactional
    public ShipmentEvent addEvent(UUID shipmentId, ShipmentDtos.AddEventRequest request) {
        Shipment shipment = get(shipmentId);

        if (request.eventType() == ShipmentEventType.DELAYED) {
            if (request.delayHours() == null || request.delayHours() <= 0) {
                throw new IllegalArgumentException("delayHours is required and must be positive for a DELAYED event");
            }
            if (shipment.avgDailyDemand == null) {
                // Caught here deliberately, with a clear message, instead of letting
                // ShipmentEventPublisher's Map.of(...) throw a bare NullPointerException -
                // that's exactly what happened before this check existed: a 500 with no
                // explanation, and the downstream investigation silently never triggered.
                throw new IllegalArgumentException(
                        "Shipment " + shipment.shipmentCode + " has no avgDailyDemand set - required to "
                                + "investigate stockout risk for a delay, set it when creating the shipment");
            }
            shipment.status = ShipmentStatus.DELAYED;
            shipment.persist();
        } else if (request.eventType() == ShipmentEventType.ARRIVED) {
            shipment.status = ShipmentStatus.DELIVERED;
            shipment.persist();
        } else if (request.eventType() == ShipmentEventType.DEPARTED) {
            shipment.status = ShipmentStatus.IN_TRANSIT;
            shipment.persist();
        }

        ShipmentEvent event = recordEvent(shipment.tenantId, shipment.id, request.eventType(),
                request.description(), request.delayHours());

        if (request.eventType() == ShipmentEventType.DELAYED) {
            eventPublisher.publishDelayed(shipment, request.delayHours());
        }

        return event;
    }

    @Transactional
    public ShipmentEvent handleCarrierWebhook(ShipmentDtos.WebhookPayload payload) {
        String tenantId = tenantContext.require();
        Shipment shipment = Shipment.<Shipment>find("tenantId = ?1 and shipmentCode = ?2", tenantId, payload.shipmentCode())
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("Shipment not found for code: " + payload.shipmentCode()));

        return addEvent(shipment.id, new ShipmentDtos.AddEventRequest(
                ShipmentEventType.DELAYED, payload.description(), payload.delayHours()));
    }

    public List<ShipmentEvent> timeline(UUID shipmentId) {
        get(shipmentId);
        return ShipmentEvent.list("shipmentId = ?1 order by occurredAt asc", shipmentId);
    }

    /**
     * On-time means "delivered and never had a DELAYED event recorded" - not just
     * "currently DELIVERED", since a shipment that was delayed and later arrived is still
     * late, not on-time. Only concluded (DELIVERED) shipments count toward the rate; ones
     * still in transit haven't had a chance to be late or not yet.
     */
    public ShipmentDtos.OnTimeRateResponse onTimeRate() {
        String tenantId = tenantContext.require();
        long totalDelivered = Shipment.count("tenantId = ?1 and status = ?2", tenantId, ShipmentStatus.DELIVERED);
        long onTimeDelivered = Shipment.count(
                "tenantId = ?1 and status = ?2 and id not in (select e.shipmentId from ShipmentEvent e where e.eventType = ?3)",
                tenantId, ShipmentStatus.DELIVERED, ShipmentEventType.DELAYED);

        Double rate = totalDelivered == 0 ? null : (double) onTimeDelivered / totalDelivered;
        return new ShipmentDtos.OnTimeRateResponse(totalDelivered, onTimeDelivered, rate);
    }

    private ShipmentEvent recordEvent(String tenantId, UUID shipmentId, ShipmentEventType type, String description,
                                       Integer delayHours) {
        ShipmentEvent event = new ShipmentEvent();
        event.tenantId = tenantId;
        event.shipmentId = shipmentId;
        event.eventType = type;
        event.description = description;
        event.delayHours = delayHours;
        event.persist();
        return event;
    }
}
