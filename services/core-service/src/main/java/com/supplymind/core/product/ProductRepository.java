package com.supplymind.core.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    java.util.List<Product> findAllByTenantId(String tenantId);

    Optional<Product> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByTenantIdAndSku(String tenantId, String sku);
}
