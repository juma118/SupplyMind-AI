package com.supplymind.shipment.shipment;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/webhooks/carriers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarrierWebhookResource {

    @Inject
    ShipmentService service;

    @POST
    @Path("/{carrier}")
    public ShipmentDtos.EventResponse handle(@PathParam("carrier") String carrier,
                                              @Valid ShipmentDtos.WebhookPayload payload) {
        // carrier is accepted but unused in this thin slice - a real build would apply
        // per-carrier payload mapping here before calling the shared delay-event path.
        return ShipmentDtos.EventResponse.from(service.handleCarrierWebhook(payload));
    }
}
