package com.supplymind.aiops.recommendation;

import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.common.TenantRequiredException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationQueryService {

    private final RecommendationRepository repository;

    public RecommendationQueryService(RecommendationRepository repository) {
        this.repository = repository;
    }

    public List<AiRecommendation> listPending() {
        return repository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(requireTenant(), RecommendationStatus.PENDING);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
