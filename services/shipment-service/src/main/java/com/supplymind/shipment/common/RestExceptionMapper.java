package com.supplymind.shipment.common;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

@Provider
public class RestExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger log = Logger.getLogger(RestExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException exception) {
        Response.Status status = switch (exception) {
            case TenantRequiredException e -> Response.Status.BAD_REQUEST;
            case NotFoundException e -> Response.Status.NOT_FOUND;
            case IllegalArgumentException e -> Response.Status.CONFLICT;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };
        if (status == Response.Status.INTERNAL_SERVER_ERROR) {
            // An unmapped exception type reaching here used to vanish with zero log
            // output - this swallowed exactly the bug that broke the DELAYED event path.
            log.error("Unhandled exception mapped to 500", exception);
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.getStatusCode(),
                        "error", status.getReasonPhrase(),
                        "message", exception.getMessage() == null ? "" : exception.getMessage()))
                .build();
    }
}
