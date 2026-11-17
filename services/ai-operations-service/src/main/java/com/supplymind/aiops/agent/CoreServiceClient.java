package com.supplymind.aiops.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Narrow, typed client to the Core Service's supplier API - this is the "tool" boundary
 * from README §5: agents never query the database directly, only an authenticated
 * internal domain service, with tenant context forwarded explicitly (not trusted from
 * the model).
 */
@Component
public class CoreServiceClient {

    private final RestClient restClient;

    public CoreServiceClient(@Value("${aiops.core-service.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public List<SupplierSummary> listSuppliers(String tenantId) {
        SupplierSummary[] suppliers = restClient.get()
                .uri("/api/v1/suppliers")
                .header("X-Tenant-Id", tenantId)
                .retrieve()
                .body(SupplierSummary[].class);
        return suppliers == null ? List.of() : List.of(suppliers);
    }

    public record SupplierSummary(
            String id,
            String name,
            String category,
            java.math.BigDecimal onTimeRate,
            Integer avgLeadTimeDays) {
    }
}
