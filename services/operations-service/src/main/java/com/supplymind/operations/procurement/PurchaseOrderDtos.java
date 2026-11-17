package com.supplymind.operations.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PurchaseOrderDtos {

    private PurchaseOrderDtos() {
    }

    public record CreateItemRequest(
            @NotBlank String productSku,
            @NotNull @Positive Integer quantity,
            @NotNull BigDecimal unitPrice) {
    }

    public record CreatePurchaseOrderRequest(
            @NotBlank String supplierId,
            @NotBlank String warehouseCode,
            @NotEmpty @Valid List<CreateItemRequest> items,
            String sourceType,
            String sourceId) {
    }

    public record ItemResponse(UUID id, String productSku, int quantity, BigDecimal unitPrice) {
        public static ItemResponse from(PurchaseOrderItem item) {
            return new ItemResponse(item.getId(), item.getProductSku(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public record PurchaseOrderResponse(
            UUID id,
            String supplierId,
            String warehouseCode,
            String status,
            BigDecimal totalValue,
            List<ItemResponse> items,
            String sourceType,
            String sourceId,
            Instant createdAt,
            Instant updatedAt) {

        public static PurchaseOrderResponse from(PurchaseOrder po) {
            return new PurchaseOrderResponse(po.getId(), po.getSupplierId(), po.getWarehouseCode(),
                    po.getStatus().name(), po.getTotalValue(),
                    po.getItems().stream().map(ItemResponse::from).toList(),
                    po.getSourceType(), po.getSourceId(), po.getCreatedAt(), po.getUpdatedAt());
        }
    }
}
