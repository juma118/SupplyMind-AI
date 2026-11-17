package com.supplymind.operations.procurement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    // Fetch-joined for the same reason as findByIdAndTenantId below: items is LAZY and
    // open-in-view is disabled, so a list endpoint mapping straight to DTOs needs items
    // already initialized by the query itself.
    @Query("select distinct po from PurchaseOrder po left join fetch po.items where po.tenantId = :tenantId order by po.createdAt desc")
    List<PurchaseOrder> findAllByTenantId(String tenantId);

    // items is LAZY and open-in-view is disabled, so every load that might cross into a
    // DTO mapping outside the transaction must fetch-join items here, not rely on access
    // inside the service happening to touch the collection first (see submit() bug).
    @Query("select po from PurchaseOrder po left join fetch po.items where po.id = :id and po.tenantId = :tenantId")
    Optional<PurchaseOrder> findByIdAndTenantId(UUID id, String tenantId);
}
