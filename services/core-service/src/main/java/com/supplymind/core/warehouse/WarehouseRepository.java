package com.supplymind.core.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    List<Warehouse> findAllByTenantId(String tenantId);

    Optional<Warehouse> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByTenantIdAndCode(String tenantId, String code);
}
