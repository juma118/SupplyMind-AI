package com.supplymind.aiops.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<AiRecommendation, UUID> {

    List<AiRecommendation> findAllByInvestigationId(UUID investigationId);

    Optional<AiRecommendation> findByIdAndTenantId(UUID id, String tenantId);

    List<AiRecommendation> findAllByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, RecommendationStatus status);
}
