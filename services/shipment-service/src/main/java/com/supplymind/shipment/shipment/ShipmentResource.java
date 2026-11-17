package com.supplymind.shipment.shipment;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

// @Authenticated (not a global HTTP policy) so CarrierWebhookResource stays reachable by
// external carrier systems with no human JWT to present - that endpoint deliberately has
// no annotation at all, which is a real security gap (README §15's hardening checklist
// calls for proper webhook auth, e.g. a signature, which isn't built here) but a correct
// stand-in distinction from "every other shipment endpoint needs a real user".
@Authenticated
@Path("/api/v1/shipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    @Inject
    ShipmentService service;

    @GET
    public List<ShipmentDtos.ShipmentResponse> list() {
        return service.list().stream().map(ShipmentDtos.ShipmentResponse::from).toList();
    }

    @GET
    @Path("/at-risk")
    public List<ShipmentDtos.ShipmentResponse> atRisk() {
        return service.atRisk().stream().map(ShipmentDtos.ShipmentResponse::from).toList();
    }

    @GET
    @Path("/analytics/on-time-rate")
    public ShipmentDtos.OnTimeRateResponse onTimeRate() {
        return service.onTimeRate();
    }

    @GET
    @Path("/{id}")
    public ShipmentDtos.ShipmentResponse get(@PathParam("id") UUID id) {
        return ShipmentDtos.ShipmentResponse.from(service.get(id));
    }

    @GET
    @Path("/{id}/timeline")
    public List<ShipmentDtos.EventResponse> timeline(@PathParam("id") UUID id) {
        return service.timeline(id).stream().map(ShipmentDtos.EventResponse::from).toList();
    }

    // PRODUCT_SPEC.md §3: Logistics Coordinator owns Shipments.
    @RolesAllowed({"logistics-coordinator", "platform-admin"})
    @POST
    public Response create(@Valid ShipmentDtos.CreateShipmentRequest request) {
        Shipment created = service.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ShipmentDtos.ShipmentResponse.from(created))
                .build();
    }

    @RolesAllowed({"logistics-coordinator", "platform-admin"})
    @POST
    @Path("/{id}/events")
    public ShipmentDtos.EventResponse addEvent(@PathParam("id") UUID id, @Valid ShipmentDtos.AddEventRequest request) {
        return ShipmentDtos.EventResponse.from(service.addEvent(id, request));
    }
}
