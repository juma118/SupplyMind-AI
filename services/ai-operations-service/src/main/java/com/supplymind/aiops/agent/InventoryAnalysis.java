package com.supplymind.aiops.agent;

public record InventoryAnalysis(
        double coverHours,
        boolean stockoutRisk,
        double shortfallUnits,
        String evidenceDescription) {
}
