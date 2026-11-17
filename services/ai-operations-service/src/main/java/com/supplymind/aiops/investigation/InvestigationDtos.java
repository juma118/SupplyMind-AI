package com.supplymind.aiops.investigation;

import com.supplymind.aiops.agent.AiAgentRun;
import com.supplymind.aiops.evidence.EvidenceReference;
import com.supplymind.aiops.recommendation.AiRecommendation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InvestigationDtos {

    private InvestigationDtos() {
    }

    public record InvestigationResponse(
            UUID id,
            IncidentType incidentType,
            InvestigationStatus status,
            Severity severity,
            String sourceReference,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt) {

        public static InvestigationResponse from(AiInvestigation i) {
            return new InvestigationResponse(i.getId(), i.getIncidentType(), i.getStatus(), i.getSeverity(),
                    i.getSourceReference(), i.getCreatedAt(), i.getUpdatedAt(), i.getClosedAt());
        }
    }

    public record AgentRunResponse(
            UUID id,
            String agentName,
            String status,
            String mode,
            String provider,
            String model,
            String output,
            String errorMessage,
            Instant startedAt,
            Instant finishedAt) {

        public static AgentRunResponse from(AiAgentRun r) {
            return new AgentRunResponse(r.getId(), r.getAgentName(),
                    r.getStatus() == null ? null : r.getStatus().name(),
                    r.getMode() == null ? null : r.getMode().name(),
                    r.getProvider(), r.getModel(), r.getOutput(), r.getErrorMessage(),
                    r.getStartedAt(), r.getFinishedAt());
        }
    }

    public record EvidenceResponse(
            UUID id,
            String sourceType,
            String sourceId,
            String description,
            Instant createdAt) {

        public static EvidenceResponse from(EvidenceReference e) {
            return new EvidenceResponse(e.getId(), e.getSourceType(), e.getSourceId(), e.getDescription(), e.getCreatedAt());
        }
    }

    public record RecommendationResponse(
            UUID id,
            UUID investigationId,
            String summary,
            BigDecimal estimatedCost,
            String status,
            String actionType,
            String actionProductSku,
            Integer actionQuantity,
            String actionSourceWarehouse,
            String actionDestWarehouse,
            String actionSupplierId,
            Instant createdAt) {

        public static RecommendationResponse from(AiRecommendation r) {
            return new RecommendationResponse(r.getId(), r.getInvestigationId(), r.getSummary(), r.getEstimatedCost(),
                    r.getStatus() == null ? null : r.getStatus().name(),
                    r.getActionType() == null ? null : r.getActionType().name(),
                    r.getActionProductSku(), r.getActionQuantity(), r.getActionSourceWarehouse(),
                    r.getActionDestWarehouse(), r.getActionSupplierId(), r.getCreatedAt());
        }
    }

    public record InvestigationDetailResponse(
            InvestigationResponse investigation,
            List<AgentRunResponse> agentRuns,
            List<EvidenceResponse> evidence,
            List<RecommendationResponse> recommendations) {
    }
}
