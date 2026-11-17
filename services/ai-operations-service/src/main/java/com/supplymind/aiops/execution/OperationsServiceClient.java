package com.supplymind.aiops.execution;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Narrow, typed client to the Operations Service - the only way this service touches
 * inventory/purchase-order state. Used for two purposes:
 *  1. read-only lookups during investigation (finding a transfer candidate warehouse)
 *  2. the actual execution call made by ApprovalService after a human approves -
 *     never called from agent code directly (README §5 tool-safety rule).
 *
 * Every call carries this service's own client-credentials token (KeycloakServiceTokenProvider),
 * since Operations Service now requires authentication on every request, including the
 * read-only lookup in (1), which runs during Kafka-triggered processing with no incoming
 * user request to borrow a token from.
 */
@Component
public class OperationsServiceClient {

    private final RestClient restClient;
    private final KeycloakServiceTokenProvider tokenProvider;

    public OperationsServiceClient(@Value("${aiops.operations-service.base-url:http://localhost:8082}") String baseUrl,
                                    KeycloakServiceTokenProvider tokenProvider) {
        this.restClient = RestClient.create(baseUrl);
        this.tokenProvider = tokenProvider;
    }

    public List<WarehouseBalance> listWarehouseBalances(String tenantId, String productSku) {
        WarehouseBalance[] balances = restClient.get()
                .uri("/api/v1/inventory/{sku}/warehouses", productSku)
                .header("X-Tenant-Id", tenantId)
                .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
                .retrieve()
                .body(WarehouseBalance[].class);
        return balances == null ? List.of() : List.of(balances);
    }

    public void transfer(String tenantId, String sourceWarehouseCode, String destWarehouseCode, String productSku,
                          int quantity, String referenceType, String referenceId) {
        restClient.post()
                .uri("/api/v1/inventory/transfers")
                .header("X-Tenant-Id", tenantId)
                .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
                .body(Map.of(
                        "sourceWarehouseCode", sourceWarehouseCode,
                        "destWarehouseCode", destWarehouseCode,
                        "productSku", productSku,
                        "quantity", quantity,
                        "referenceType", referenceType,
                        "referenceId", referenceId))
                .retrieve()
                .toBodilessEntity();
    }

    public String createAndSubmitPurchaseOrder(String tenantId, String supplierId, String warehouseCode,
                                                 String productSku, int quantity, BigDecimal unitPrice,
                                                 String sourceType, String sourceId) {
        String token = tokenProvider.getAccessToken();
        Map<String, Object> created = restClient.post()
                .uri("/api/v1/purchase-orders")
                .header("X-Tenant-Id", tenantId)
                .header("Authorization", "Bearer " + token)
                .body(Map.of(
                        "supplierId", supplierId,
                        "warehouseCode", warehouseCode,
                        "items", List.of(Map.of(
                                "productSku", productSku,
                                "quantity", quantity,
                                "unitPrice", unitPrice)),
                        "sourceType", sourceType,
                        "sourceId", sourceId))
                .retrieve()
                .body(Map.class);

        String poId = (String) created.get("id");
        restClient.post()
                .uri("/api/v1/purchase-orders/{id}/submit", poId)
                .header("X-Tenant-Id", tenantId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
        return poId;
    }

    public record WarehouseBalance(String warehouseCode, String productSku, int availableUnits) {
    }
}
