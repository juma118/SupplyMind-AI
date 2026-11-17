package com.supplymind.aiops.agent;

import com.supplymind.aiops.investigation.ShipmentDelayContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * README §5 requires this math to be deterministic Java, never an LLM call - these
 * numbers are exactly what the fallback recommendation text and the actual transfer/PO
 * quantities are built from (see InvestigationWorkflowService), so a rounding or
 * off-by-one here would silently produce a wrong recommendation, not an obvious crash.
 */
class InventoryIntelligenceAgentTest {

    private final InventoryIntelligenceAgent agent = new InventoryIntelligenceAgent();

    @Test
    void noStockoutRiskWhenCoverExceedsDelay() {
        // 2400 units / 1200 per day * 24h = 48h of cover, which exceeds a 24h delay.
        var context = new ShipmentDelayContext("SHP-1", "WID-1", 24, "WH-A", 2400, 1200.0);

        InventoryAnalysis result = agent.analyze(context);

        assertThat(result.stockoutRisk()).isFalse();
        assertThat(result.coverHours()).isCloseTo(48.0, within(0.01));
        assertThat(result.shortfallUnits()).isEqualTo(0.0);
    }

    @Test
    void stockoutRiskAndShortfallWhenDelayExceedsCover() {
        // Same scenario as the README §2 primary demo scenario: 48h of cover, 72h delay.
        var context = new ShipmentDelayContext("SHP-1", "WID-1", 72, "WH-A", 2400, 1200.0);

        InventoryAnalysis result = agent.analyze(context);

        assertThat(result.stockoutRisk()).isTrue();
        assertThat(result.coverHours()).isCloseTo(48.0, within(0.01));
        // (72h - 48h) / 24h * 1200 units/day = 1200 units short.
        assertThat(result.shortfallUnits()).isCloseTo(1200.0, within(0.01));
    }

    @Test
    void exactBoundaryWhereDelayEqualsCoverCountsAsAtRisk() {
        // delayHours >= coverHours is the boundary condition in the implementation -
        // exactly-enough cover is treated as risky, not safe, which is the conservative
        // (correct) choice for a stockout warning.
        var context = new ShipmentDelayContext("SHP-1", "WID-1", 48, "WH-A", 2400, 1200.0);

        InventoryAnalysis result = agent.analyze(context);

        assertThat(result.stockoutRisk()).isTrue();
        assertThat(result.shortfallUnits()).isCloseTo(0.0, within(0.01));
    }
}
