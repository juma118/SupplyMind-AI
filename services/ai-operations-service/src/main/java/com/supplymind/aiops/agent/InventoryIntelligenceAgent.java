package com.supplymind.aiops.agent;

import com.supplymind.aiops.investigation.ShipmentDelayContext;
import org.springframework.stereotype.Component;

/**
 * Deterministic stock/cover calculation - per README §5 tool-safety rules, this math is
 * plain Java, never delegated to the model. The agent "label" reflects its role in the
 * workflow (README §5 Agents list); the computation itself is not an LLM call.
 */
@Component
public class InventoryIntelligenceAgent {

    public InventoryAnalysis analyze(ShipmentDelayContext context) {
        double coverHours = (context.currentStockUnits() / context.avgDailyDemand()) * 24.0;
        boolean atRisk = context.delayHours() >= coverHours;
        double shortfallUnits = atRisk
                ? ((context.delayHours() - coverHours) / 24.0) * context.avgDailyDemand()
                : 0.0;

        String evidence = String.format(
                "Warehouse %s holds %d units of %s (~%.1fh of cover at %.1f units/day average demand). "
                        + "Shipment delay of %dh %s projected cover.",
                context.warehouseCode(), context.currentStockUnits(), context.productSku(),
                coverHours, context.avgDailyDemand(), context.delayHours(),
                atRisk ? "EXCEEDS" : "does not exceed");

        return new InventoryAnalysis(coverHours, atRisk, shortfallUnits, evidence);
    }
}
