package com.supplymind.operations.planning;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * README §6's formulas, kept as plain deterministic Java - never delegated to the LLM
 * (README §5's tool-safety rule applies to this math exactly as much as it does to
 * inventory/cost calculations elsewhere in the codebase).
 *
 * Safety stock here is intentionally simple: either an explicit override, or
 * z * demandStdDev * sqrt(leadTimeDays) when daily-demand variability is known, or zero
 * otherwise. README §6 says to "begin with a transparent statistical baseline" - a safety
 * stock of zero when variability is unknown is that baseline; it is not hidden as a
 * sophisticated estimate it isn't.
 */
@Service
public class ReorderCalculationService {

    public ReorderCalculationResult calculateReorderPoint(BigDecimal avgDailyDemand, int leadTimeDays,
                                                            Integer safetyStockOverride, Double demandStdDev,
                                                            Double serviceLevelZ) {
        int safetyStock = resolveSafetyStock(leadTimeDays, safetyStockOverride, demandStdDev, serviceLevelZ);

        BigDecimal leadTimeDemand = avgDailyDemand.multiply(BigDecimal.valueOf(leadTimeDays));
        int reorderPoint = leadTimeDemand.setScale(0, RoundingMode.CEILING).intValue() + safetyStock;

        return new ReorderCalculationResult(reorderPoint, safetyStock, leadTimeDemand);
    }

    private int resolveSafetyStock(int leadTimeDays, Integer safetyStockOverride, Double demandStdDev, Double serviceLevelZ) {
        if (safetyStockOverride != null) {
            return safetyStockOverride;
        }
        if (demandStdDev != null && serviceLevelZ != null) {
            double safetyStock = serviceLevelZ * demandStdDev * Math.sqrt(leadTimeDays);
            return (int) Math.ceil(safetyStock);
        }
        return 0;
    }

    /**
     * EOQ = sqrt(2 x annual demand x order cost / holding cost per unit per year).
     * Takes explicit cost inputs rather than assuming defaults this codebase has no real
     * data for (order cost, holding cost) - README §13 bars inventing performance/accuracy
     * figures, and an invented default cost is the same kind of fabrication.
     */
    public int economicOrderQuantity(BigDecimal annualDemand, BigDecimal orderCost, BigDecimal holdingCostPerUnitPerYear) {
        if (holdingCostPerUnitPerYear.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("holdingCostPerUnitPerYear must be positive");
        }
        double eoq = Math.sqrt(
                2 * annualDemand.doubleValue() * orderCost.doubleValue() / holdingCostPerUnitPerYear.doubleValue());
        return (int) Math.ceil(eoq);
    }

    public record ReorderCalculationResult(int reorderPoint, int safetyStock, BigDecimal leadTimeDemand) {
    }
}
