package com.supplymind.aiops.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<HumanApproval, UUID> {

    List<HumanApproval> findAllByRecommendationId(UUID recommendationId);
}
