package com.supplymind.aiops.investigation;

import com.supplymind.aiops.agent.AgentRunRepository;
import com.supplymind.aiops.common.NotFoundException;
import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.common.TenantRequiredException;
import com.supplymind.aiops.evidence.EvidenceRepository;
import com.supplymind.aiops.recommendation.RecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InvestigationQueryService {

    private final InvestigationRepository investigationRepository;
    private final AgentRunRepository agentRunRepository;
    private final EvidenceRepository evidenceRepository;
    private final RecommendationRepository recommendationRepository;

    public InvestigationQueryService(InvestigationRepository investigationRepository,
                                      AgentRunRepository agentRunRepository,
                                      EvidenceRepository evidenceRepository,
                                      RecommendationRepository recommendationRepository) {
        this.investigationRepository = investigationRepository;
        this.agentRunRepository = agentRunRepository;
        this.evidenceRepository = evidenceRepository;
        this.recommendationRepository = recommendationRepository;
    }

    public List<AiInvestigation> list() {
        return investigationRepository.findAllByTenantIdOrderByCreatedAtDesc(requireTenant());
    }

    public AiInvestigation get(UUID id) {
        return investigationRepository.findByIdAndTenantId(id, requireTenant())
                .orElseThrow(() -> new NotFoundException("Investigation not found: " + id));
    }

    public InvestigationDtos.InvestigationDetailResponse getDetail(UUID id) {
        AiInvestigation investigation = get(id);
        var agentRuns = agentRunRepository.findAllByInvestigationIdOrderByStartedAtAsc(id).stream()
                .map(InvestigationDtos.AgentRunResponse::from).toList();
        var evidence = evidenceRepository.findAllByInvestigationId(id).stream()
                .map(InvestigationDtos.EvidenceResponse::from).toList();
        var recommendations = recommendationRepository.findAllByInvestigationId(id).stream()
                .map(InvestigationDtos.RecommendationResponse::from).toList();

        return new InvestigationDtos.InvestigationDetailResponse(
                InvestigationDtos.InvestigationResponse.from(investigation), agentRuns, evidence, recommendations);
    }

    public List<InvestigationDtos.RecommendationResponse> getRecommendations(UUID id) {
        get(id);
        return recommendationRepository.findAllByInvestigationId(id).stream()
                .map(InvestigationDtos.RecommendationResponse::from).toList();
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
