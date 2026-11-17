package com.supplymind.operations.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record BalanceResponse(
            UUID id,
            String warehouseCode,
            String productSku,
            int availableUnits,
            int reservedUnits,
            int incomingUnits,
            int damagedUnits,
            Integer reorderPoint,
            BigDecimal avgDailyDemand,
            Integer leadTimeDays,
            Integer safetyStock,
            Instant updatedAt) {

        public static BalanceResponse from(InventoryBalance b) {
            return new BalanceResponse(b.getId(), b.getWarehouseCode(), b.getProductSku(), b.getAvailableUnits(),
                    b.getReservedUnits(), b.getIncomingUnits(), b.getDamagedUnits(), b.getReorderPoint(),
                    b.getAvgDailyDemand(), b.getLeadTimeDays(), b.getSafetyStock(), b.getUpdatedAt());
        }
    }

    public record TransferRequest(
            @NotBlank String sourceWarehouseCode,
            @NotBlank String destWarehouseCode,
            @NotBlank String productSku,
            @NotNull @Positive Integer quantity,
            String referenceType,
            String referenceId) {
    }

    public record AdjustmentRequest(
            @NotBlank String warehouseCode,
            @NotBlank String productSku,
            @NotNull Integer quantityDelta,
            @NotBlank String reason) {
    }
}
