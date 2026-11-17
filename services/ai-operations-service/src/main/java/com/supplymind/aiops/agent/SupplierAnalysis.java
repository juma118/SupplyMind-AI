package com.supplymind.aiops.agent;

import java.util.List;

public record SupplierAnalysis(List<CoreServiceClient.SupplierSummary> alternateSuppliers, String evidenceDescription) {
}
