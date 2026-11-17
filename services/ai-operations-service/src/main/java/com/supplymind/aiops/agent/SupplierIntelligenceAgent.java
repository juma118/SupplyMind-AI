package com.supplymind.aiops.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks alternate suppliers by on-time reliability. Pulled from the Core Service's
 * supplier API (the real tool-call path); if that service is unreachable, this returns
 * an empty ranked list and an evidence note saying so explicitly - it never invents
 * supplier data to paper over a failed call.
 */
@Component
public class SupplierIntelligenceAgent {

    private static final Logger log = LoggerFactory.getLogger(SupplierIntelligenceAgent.class);

    private final CoreServiceClient coreServiceClient;

    public SupplierIntelligenceAgent(CoreServiceClient coreServiceClient) {
        this.coreServiceClient = coreServiceClient;
    }

    public SupplierAnalysis analyze(String tenantId) {
        try {
            List<CoreServiceClient.SupplierSummary> ranked = coreServiceClient.listSuppliers(tenantId).stream()
                    .filter(s -> s.onTimeRate() != null)
                    .sorted(Comparator.comparing(CoreServiceClient.SupplierSummary::onTimeRate).reversed())
                    .limit(3)
                    .toList();

            String evidence = ranked.isEmpty()
                    ? "No suppliers with recorded on-time performance found for this tenant in Core Service."
                    : "Top alternate suppliers by on-time rate: " + ranked.stream()
                    .map(s -> s.name() + " (" + formatPct(s.onTimeRate()) + "%, " + s.avgLeadTimeDays() + "d lead)")
                    .reduce((a, b) -> a + "; " + b).orElse("");

            return new SupplierAnalysis(ranked, evidence);
        } catch (Exception e) {
            log.warn("Core Service supplier lookup failed: {}", e.getMessage());
            return new SupplierAnalysis(List.of(),
                    "Supplier lookup failed - Core Service unreachable (" + e.getMessage() + "). No alternate suppliers evaluated.");
        }
    }

    private String formatPct(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP).toString();
    }
}
