package com.supplymind.operations.planning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public final class PlanningDtos {

    private PlanningDtos() {
    }

    public record DemandProfileRequest(
            @NotBlank String warehouseCode,
            @NotBlank String productSku,
            @NotNull @Positive BigDecimal avgDailyDemand,
            @NotNull @Positive Integer leadTimeDays,
            Integer safetyStockOverride,
            Double demandStdDev,
            Double serviceLevelZ) {
    }

    public record DemandProfileResponse(
            String warehouseCode,
            String productSku,
            BigDecimal avgDailyDemand,
            int leadTimeDays,
            int safetyStock,
            int reorderPoint) {
    }

    public record EoqRequest(
            @NotNull @Positive BigDecimal annualDemand,
            @NotNull @Positive BigDecimal orderCost,
            @NotNull @Positive BigDecimal holdingCostPerUnitPerYear) {
    }

    public record EoqResponse(int economicOrderQuantity) {
    }

    public record StockoutRiskResponse(
            String warehouseCode,
            String productSku,
            int availableUnits,
            BigDecimal avgDailyDemand,
            Double daysOfCover,
            Integer leadTimeDays,
            Integer reorderPoint,
            boolean belowReorderPoint,
            boolean coverShorterThanLeadTime) {
    }
}
