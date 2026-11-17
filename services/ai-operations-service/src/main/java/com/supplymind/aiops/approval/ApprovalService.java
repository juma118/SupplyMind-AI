package com.supplymind.aiops.approval;

import com.supplymind.aiops.common.AuditService;
import com.supplymind.aiops.common.InvalidStateTransitionException;
import com.supplymind.aiops.common.NotFoundException;
import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.common.TenantRequiredException;
import com.supplymind.aiops.execution.OperationsServiceClient;
import com.supplymind.aiops.investigation.AiInvestigation;
import com.supplymind.aiops.investigation.InvestigationRepository;
import com.supplymind.aiops.investigation.InvestigationStatus;
import com.supplymind.aiops.recommendation.AiRecommendation;
import com.supplymind.aiops.recommendation.RecommendationRepository;
import com.supplymind.aiops.recommendation.RecommendationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The only path by which an AI recommendation leads to any state change beyond itself.
 * README §5 tool-safety rule: "AI recommendations must not directly mutate inventory or
 * purchase orders" and "require human approval for consequential operations" - this
 * service is the human gate. EXECUTING now calls Operations Service's protected
 * transfer/PO APIs via executeAction(), driven by the recommendation's deterministic
 * action fields - never its free-text summary.
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final RecommendationRepository recommendationRepository;
    private final InvestigationRepository investigationRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;
    private final OperationsServiceClient operationsServiceClient;

    public ApprovalService(RecommendationRepository recommendationRepository,
                            InvestigationRepository investigationRepository,
                            ApprovalRepository approvalRepository,
                            AuditService auditService,
                            OperationsServiceClient operationsServiceClient) {
        this.recommendationRepository = recommendationRepository;
        this.investigationRepository = investigationRepository;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
        this.operationsServiceClient = operationsServiceClient;
    }

    @Transactional
    public AiRecommendation approve(UUID recommendationId, String actorId, String comment) {
        String tenantId = requireTenant();
        AiRecommendation recommendation = getPendingRecommendation(recommendationId, tenantId);
        AiInvestigation investigation = getInvestigation(recommendation.getInvestigationId(), tenantId);

        requireInvestigationStatus(investigation, InvestigationStatus.AWAITING_APPROVAL);

        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendationRepository.save(recommendation);
        saveApproval(recommendationId, tenantId, actorId, ApprovalDecision.APPROVED, comment);

        moveInvestigation(investigation, InvestigationStatus.APPROVED);
        moveInvestigation(investigation, InvestigationStatus.EXECUTING);

        try {
            executeAction(tenantId, recommendation);
            moveInvestigation(investigation, InvestigationStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Execution failed for recommendation {}: {}", recommendationId, e.getMessage(), e);
            auditService.record(tenantId, "system", "EXECUTE_FAILED", "AiRecommendation", recommendationId.toString(), e.getMessage());
            moveInvestigation(investigation, InvestigationStatus.FAILED);
            moveInvestigation(investigation, InvestigationStatus.CLOSED);
            auditService.record(tenantId, actorId, "APPROVE", "AiRecommendation", recommendationId.toString(), comment);
            return recommendation;
        }
        moveInvestigation(investigation, InvestigationStatus.CLOSED);

        auditService.record(tenantId, actorId, "APPROVE", "AiRecommendation", recommendationId.toString(), comment);
        return recommendation;
    }

    private void executeAction(String tenantId, AiRecommendation recommendation) {
        switch (recommendation.getActionType()) {
            case TRANSFER -> {
                operationsServiceClient.transfer(tenantId, recommendation.getActionSourceWarehouse(),
                        recommendation.getActionDestWarehouse(), recommendation.getActionProductSku(),
                        recommendation.getActionQuantity(), "AI_RECOMMENDATION", recommendation.getId().toString());
                auditService.record(tenantId, "system", "EXECUTE_TRANSFER", "AiRecommendation",
                        recommendation.getId().toString(),
                        recommendation.getActionQuantity() + " x " + recommendation.getActionProductSku()
                                + " from " + recommendation.getActionSourceWarehouse() + " to " + recommendation.getActionDestWarehouse());
            }
            case PURCHASE_ORDER -> {
                String poId = operationsServiceClient.createAndSubmitPurchaseOrder(tenantId,
                        recommendation.getActionSupplierId(), recommendation.getActionDestWarehouse(),
                        recommendation.getActionProductSku(), recommendation.getActionQuantity(),
                        recommendation.getActionUnitPrice(), "AI_RECOMMENDATION", recommendation.getId().toString());
                auditService.record(tenantId, "system", "EXECUTE_PURCHASE_ORDER", "AiRecommendation",
                        recommendation.getId().toString(), "Created and submitted purchase order " + poId);
            }
            case NONE -> auditService.record(tenantId, "system", "EXECUTE_NONE", "AiRecommendation",
                    recommendation.getId().toString(), "No automatic action for this recommendation");
        }
    }

    @Transactional
    public AiRecommendation reject(UUID recommendationId, String actorId, String comment) {
        String tenantId = requireTenant();
        AiRecommendation recommendation = getPendingRecommendation(recommendationId, tenantId);
        AiInvestigation investigation = getInvestigation(recommendation.getInvestigationId(), tenantId);

        requireInvestigationStatus(investigation, InvestigationStatus.AWAITING_APPROVAL);

        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendationRepository.save(recommendation);
        saveApproval(recommendationId, tenantId, actorId, ApprovalDecision.REJECTED, comment);

        moveInvestigation(investigation, InvestigationStatus.REJECTED);
        moveInvestigation(investigation, InvestigationStatus.CLOSED);

        auditService.record(tenantId, actorId, "REJECT", "AiRecommendation", recommendationId.toString(), comment);
        return recommendation;
    }

    private AiRecommendation getPendingRecommendation(UUID recommendationId, String tenantId) {
        AiRecommendation recommendation = recommendationRepository.findByIdAndTenantId(recommendationId, tenantId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Recommendation " + recommendationId + " is not pending (status=" + recommendation.getStatus() + ")");
        }
        return recommendation;
    }

    private AiInvestigation getInvestigation(UUID investigationId, String tenantId) {
        return investigationRepository.findByIdAndTenantId(investigationId, tenantId)
                .orElseThrow(() -> new NotFoundException("Investigation not found: " + investigationId));
    }

    private void requireInvestigationStatus(AiInvestigation investigation, InvestigationStatus expected) {
        if (investigation.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "Investigation " + investigation.getId() + " is not " + expected + " (status=" + investigation.getStatus() + ")");
        }
    }

    private void moveInvestigation(AiInvestigation investigation, InvestigationStatus target) {
        if (!investigation.getStatus().canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition investigation from " + investigation.getStatus() + " to " + target);
        }
        investigation.setStatus(target);
        if (target == InvestigationStatus.CLOSED) {
            investigation.setClosedAt(Instant.now());
        }
        investigationRepository.save(investigation);
    }

    private void saveApproval(UUID recommendationId, String tenantId, String actorId, ApprovalDecision decision, String comment) {
        HumanApproval approval = new HumanApproval();
        approval.setTenantId(tenantId);
        approval.setRecommendationId(recommendationId);
        approval.setActorId(actorId);
        approval.setDecision(decision);
        approval.setComment(comment);
        approvalRepository.save(approval);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
