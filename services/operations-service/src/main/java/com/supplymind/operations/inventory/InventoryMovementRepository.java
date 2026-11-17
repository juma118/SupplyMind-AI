package com.supplymind.operations.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findAllByTenantIdAndWarehouseCodeAndProductSkuOrderByOccurredAtAsc(
            String tenantId, String warehouseCode, String productSku);
}
