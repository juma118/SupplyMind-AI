package com.supplymind.core.warehouse;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class WarehouseDtos {

    private WarehouseDtos() {
    }

    public record CreateWarehouseRequest(
            @NotBlank String code,
            @NotBlank String name,
            String city,
            String country,
            Integer capacityUnits) {
    }

    public record WarehouseResponse(
            UUID id,
            String code,
            String name,
            String city,
            String country,
            Integer capacityUnits,
            Instant createdAt,
            Instant updatedAt) {

        public static WarehouseResponse from(Warehouse w) {
            return new WarehouseResponse(w.getId(), w.getCode(), w.getName(), w.getCity(),
                    w.getCountry(), w.getCapacityUnits(), w.getCreatedAt(), w.getUpdatedAt());
        }
    }
}
