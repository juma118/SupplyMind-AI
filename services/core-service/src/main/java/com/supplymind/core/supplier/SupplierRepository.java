package com.supplymind.core.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findAllByTenantId(String tenantId);

    Optional<Supplier> findByIdAndTenantId(UUID id, String tenantId);
}
