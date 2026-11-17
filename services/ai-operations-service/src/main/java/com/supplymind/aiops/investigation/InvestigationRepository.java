package com.supplymind.aiops.investigation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationRepository extends JpaRepository<AiInvestigation, UUID> {

    List<AiInvestigation> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<AiInvestigation> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByTenantIdAndSourceReference(String tenantId, String sourceReference);
}
