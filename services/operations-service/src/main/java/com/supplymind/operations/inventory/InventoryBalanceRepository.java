package com.supplymind.operations.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {

    List<InventoryBalance> findAllByTenantId(String tenantId);

    List<InventoryBalance> findAllByTenantIdAndProductSku(String tenantId, String productSku);

    Optional<InventoryBalance> findByTenantIdAndWarehouseCodeAndProductSku(String tenantId, String warehouseCode, String productSku);
}
