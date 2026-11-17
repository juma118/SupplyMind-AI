package com.supplymind.core.product;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            BigDecimal unitCost,
            Integer reorderPoint) {
    }

    public record ProductResponse(
            UUID id,
            String sku,
            String name,
            BigDecimal unitCost,
            Integer reorderPoint,
            Instant createdAt,
            Instant updatedAt) {

        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getUnitCost(),
                    p.getReorderPoint(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
