package com.supplymind.operations.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * README §6's formulas: ROP = (Average Daily Demand x Lead Time) + Safety Stock, and
 * EOQ = sqrt(2 x annual demand x order cost / holding cost). This is deterministic math
 * that must never be delegated to an LLM (README §5) - these tests are what actually
 * enforces correctness, since nothing else in the pipeline would catch a sign error or
 * rounding mistake here.
 */
class ReorderCalculationServiceTest {

    private final ReorderCalculationService service = new ReorderCalculationService();

    @Test
    void reorderPointIsLeadTimeDemandPlusZeroSafetyStockWhenVariabilityIsUnknown() {
        var result = service.calculateReorderPoint(new BigDecimal("1200.0"), 4, null, null, null);

        // 1200 units/day * 4 days = 4800 units of lead-time demand, no safety stock known.
        assertThat(result.reorderPoint()).isEqualTo(4800);
        assertThat(result.safetyStock()).isEqualTo(0);
    }

    @Test
    void explicitSafetyStockOverrideIsAddedOnTopOfLeadTimeDemand() {
        var result = service.calculateReorderPoint(new BigDecimal("1200.0"), 4, 500, null, null);

        assertThat(result.reorderPoint()).isEqualTo(4800 + 500);
        assertThat(result.safetyStock()).isEqualTo(500);
    }

    @Test
    void safetyStockFromStdDevAndServiceLevelUsesTheStandardFormula() {
        // z=1.65 (~95% service level), stdDev=100 units/day, leadTime=4 days.
        // safety stock = 1.65 * 100 * sqrt(4) = 1.65 * 100 * 2 = 330, rounded up.
        var result = service.calculateReorderPoint(new BigDecimal("1200.0"), 4, null, 100.0, 1.65);

        assertThat(result.safetyStock()).isEqualTo(330);
        assertThat(result.reorderPoint()).isEqualTo(4800 + 330);
    }

    @Test
    void fractionalLeadTimeDemandRoundsUpNeverDown() {
        // 100.1 units/day * 3 days = 300.3 -> must round up to 301, not truncate to 300,
        // since under-rounding a reorder point risks a real stockout.
        var result = service.calculateReorderPoint(new BigDecimal("100.1"), 3, null, null, null);

        assertThat(result.reorderPoint()).isEqualTo(301);
    }

    @Test
    void economicOrderQuantityMatchesTheStandardFormula() {
        // EOQ = sqrt(2 * 10000 * 50 / 2) = sqrt(500000) ~= 707.1 -> rounds up to 708.
        int eoq = service.economicOrderQuantity(new BigDecimal("10000"), new BigDecimal("50"), new BigDecimal("2"));

        assertThat(eoq).isEqualTo(708);
    }

    @Test
    void economicOrderQuantityRejectsNonPositiveHoldingCost() {
        assertThatThrownBy(() -> service.economicOrderQuantity(new BigDecimal("10000"), new BigDecimal("50"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
