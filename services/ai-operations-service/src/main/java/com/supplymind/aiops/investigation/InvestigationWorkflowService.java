package com.supplymind.aiops.investigation;

import com.supplymind.aiops.agent.*;
import com.supplymind.aiops.common.AuditService;
import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.common.TenantRequiredException;
import com.supplymind.aiops.evidence.EvidenceReference;
import com.supplymind.aiops.evidence.EvidenceRepository;
import com.supplymind.aiops.execution.OperationsServiceClient;
import com.supplymind.aiops.knowledge.KnowledgeDtos;
import com.supplymind.aiops.knowledge.KnowledgeService;
import com.supplymind.aiops.recommendation.AiRecommendation;
import com.supplymind.aiops.recommendation.RecommendationRepository;
import com.supplymind.aiops.recommendation.RecommendedActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Runs the full investigation pipeline synchronously for the thin vertical slice
 * (README §2 primary demo scenario). A real deployment would consume this from a Kafka
 * ShipmentDelayed event and run it asynchronously (README §8: 202 Accepted + SSE); this
 * version is invoked directly over REST and blocks until the recommendation is ready,
 * which is enough to prove the agent/tool/evidence/recommendation chain end-to-end.
 */
@Service
public class InvestigationWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationWorkflowService.class);

    private final InvestigationRepository investigationRepository;
    private final AgentRunRepository agentRunRepository;
    private final EvidenceRepository evidenceRepository;
    private final RecommendationRepository recommendationRepository;
    private final AuditService auditService;
    private final AiGatewayService aiGatewayService;
    private final InventoryIntelligenceAgent inventoryIntelligenceAgent;
    private final SupplierIntelligenceAgent supplierIntelligenceAgent;
    private final OperationsServiceClient operationsServiceClient;
    private final KnowledgeService knowledgeService;

    public InvestigationWorkflowService(InvestigationRepository investigationRepository,
                                         AgentRunRepository agentRunRepository,
                                         EvidenceRepository evidenceRepository,
                                         RecommendationRepository recommendationRepository,
                                         AuditService auditService,
                                         AiGatewayService aiGatewayService,
                                         InventoryIntelligenceAgent inventoryIntelligenceAgent,
                                         SupplierIntelligenceAgent supplierIntelligenceAgent,
                                         OperationsServiceClient operationsServiceClient,
                                         KnowledgeService knowledgeService) {
        this.investigationRepository = investigationRepository;
        this.agentRunRepository = agentRunRepository;
        this.evidenceRepository = evidenceRepository;
        this.recommendationRepository = recommendationRepository;
        this.auditService = auditService;
        this.aiGatewayService = aiGatewayService;
        this.inventoryIntelligenceAgent = inventoryIntelligenceAgent;
        this.supplierIntelligenceAgent = supplierIntelligenceAgent;
        this.operationsServiceClient = operationsServiceClient;
        this.knowledgeService = knowledgeService;
    }

    @Transactional
    public AiInvestigation runShipmentDelayInvestigation(ShipmentDelayContext context) {
        String tenantId = requireTenant();

        AiInvestigation investigation = new AiInvestigation();
        investigation.setTenantId(tenantId);
        investigation.setIncidentType(IncidentType.SHIPMENT_DELAY);
        investigation.setSourceReference(context.shipmentId());
        investigation = investigationRepository.save(investigation);
        auditService.record(tenantId, "system", "INGEST", "AiInvestigation", investigation.getId().toString(),
                "Shipment delay reported: " + context.shipmentId());

        transition(investigation, InvestigationStatus.PLANNING);
        InventoryAnalysis inventoryAnalysis = inventoryIntelligenceAgent.analyze(context);
        Severity severity = inventoryAnalysis.stockoutRisk() ? Severity.HIGH : Severity.MEDIUM;
        investigation.setSeverity(severity);

        AiCallResult planResult = runCoordinatorPlanning(investigation, context, inventoryAnalysis);

        transition(investigation, InvestigationStatus.INVESTIGATING);
        recordAgentRun(investigation, "Shipment Intelligence Agent", AiMode.FALLBACK, "none", "deterministic",
                AgentRunStatus.DONE, "Shipment " + context.shipmentId() + " delayed " + context.delayHours()
                        + "h en route to warehouse " + context.warehouseCode() + ".", null);
        saveEvidence(investigation, "shipment_event", context.shipmentId(),
                "Delay of " + context.delayHours() + "h reported for shipment " + context.shipmentId());

        recordAgentRun(investigation, "Inventory Intelligence Agent", AiMode.FALLBACK, "none", "deterministic",
                AgentRunStatus.DONE, inventoryAnalysis.evidenceDescription(), null);
        saveEvidence(investigation, "inventory_ledger", context.warehouseCode(), inventoryAnalysis.evidenceDescription());

        SupplierAnalysis supplierAnalysis = supplierIntelligenceAgent.analyze(tenantId);
        recordAgentRun(investigation, "Supplier Intelligence Agent", AiMode.FALLBACK, "none", "deterministic",
                AgentRunStatus.DONE, supplierAnalysis.evidenceDescription(), null);
        saveEvidence(investigation, "supplier_catalog", context.productSku(), supplierAnalysis.evidenceDescription());

        KnowledgeDtos.SearchResponse policyFindings = runPolicyRetrieval(investigation, inventoryAnalysis);

        transition(investigation, InvestigationStatus.EVALUATING);
        AiRecommendation recommendation = runRecommendationEvaluator(investigation, context, inventoryAnalysis, supplierAnalysis, planResult, policyFindings);

        transition(investigation, InvestigationStatus.RECOMMENDATION_READY);
        transition(investigation, InvestigationStatus.AWAITING_APPROVAL);

        auditService.record(tenantId, "system", "RECOMMEND", "AiRecommendation", recommendation.getId().toString(),
                recommendation.getSummary());

        return investigation;
    }

    private AiCallResult runCoordinatorPlanning(AiInvestigation investigation, ShipmentDelayContext context,
                                                 InventoryAnalysis inventoryAnalysis) {
        String system = "You are the Operations Coordinator agent for a supply chain platform. "
                + "Classify incident severity and list 2-3 concise investigation steps. Keep the answer under 80 words.";
        String user = String.format(
                "Shipment %s delayed %dh to warehouse %s carrying %s. %s",
                context.shipmentId(), context.delayHours(), context.warehouseCode(), context.productSku(),
                inventoryAnalysis.evidenceDescription());

        AiCallResult result = aiGatewayService.call(system, user);
        String output = result.isSuccess() ? result.text()
                : "Fallback plan (no model available): investigate shipment delay cause, "
                + "check stockout exposure, evaluate alternate suppliers and transfer options.";

        recordAgentRun(investigation, "Operations Coordinator", result.mode(), result.provider(), result.model(),
                result.isSuccess() ? AgentRunStatus.DONE : AgentRunStatus.FAILED, output, result.error());
        return result;
    }

    /**
     * README §5 RAG: "Copilot answers and recommendations should cite operational records
     * or document references." Retrieval failures (no embedding provider reachable, or no
     * matching document) are not fatal - they just mean no citation is available this time,
     * same non-blocking-degradation shape as every other AI step in this pipeline.
     */
    private KnowledgeDtos.SearchResponse runPolicyRetrieval(AiInvestigation investigation, InventoryAnalysis inventoryAnalysis) {
        if (!inventoryAnalysis.stockoutRisk()) {
            return new KnowledgeDtos.SearchResponse(List.of(), true, "No stockout risk - policy lookup skipped.");
        }

        KnowledgeDtos.SearchResponse response = knowledgeService.search("emergency procurement stockout policy", 2);
        String mode = response.semanticSearchAvailable() ? "pgvector" : "keyword-fallback";

        if (response.results().isEmpty()) {
            recordAgentRun(investigation, "Policy Retrieval Agent", AiMode.FALLBACK, mode, "n/a",
                    AgentRunStatus.DONE, "No matching policy document found (" + response.note() + ")", null);
        } else {
            String summary = response.results().stream()
                    .map(r -> r.title() + " (" + r.sourceType() + ")")
                    .reduce((a, b) -> a + "; " + b).orElse("");
            recordAgentRun(investigation, "Policy Retrieval Agent",
                    response.semanticSearchAvailable() ? AiMode.ONLINE : AiMode.FALLBACK, mode, "n/a",
                    AgentRunStatus.DONE, "Found: " + summary, null);
            for (var result : response.results()) {
                saveEvidence(investigation, "policy_document", result.documentId(),
                        result.title() + " (" + result.sourceType() + "): " + result.snippet());
            }
        }
        return response;
    }

    private AiRecommendation runRecommendationEvaluator(AiInvestigation investigation, ShipmentDelayContext context,
                                                          InventoryAnalysis inventoryAnalysis,
                                                          SupplierAnalysis supplierAnalysis,
                                                          AiCallResult planResult,
                                                          KnowledgeDtos.SearchResponse policyFindings) {
        // The action is decided deterministically first, so both the fallback summary and
        // the prompt given to a real model are grounded in the same action the system will
        // actually execute on approval - the LLM narrates a decision, it does not make one.
        AiRecommendation recommendation = new AiRecommendation();
        recommendation.setTenantId(investigation.getTenantId());
        recommendation.setInvestigationId(investigation.getId());
        recommendation.setEstimatedCost(estimateCost(inventoryAnalysis));
        determineAction(recommendation, investigation.getTenantId(), context, inventoryAnalysis, supplierAnalysis);

        String system = "You are the Recommendation Evaluator agent. Given the evidence and the action already "
                + "selected by deterministic logic, write one concise recovery recommendation (max 60 words) "
                + "for a human Supply Chain Manager to approve or reject, describing that exact action. "
                + "If a relevant policy document is provided, reference it by title. "
                + "Do not propose a different action. Do not state a confidence percentage.";
        String user = String.format(
                "Inventory finding: %s%nSupplier finding: %s%nCoordinator plan: %s%nSelected action: %s%nRelevant policy: %s",
                inventoryAnalysis.evidenceDescription(), supplierAnalysis.evidenceDescription(),
                planResult.isSuccess() ? planResult.text() : "(coordinator plan unavailable)",
                describeAction(recommendation), describePolicyFindings(policyFindings));

        AiCallResult result = aiGatewayService.call(system, user);
        String summary = result.isSuccess() ? result.text() : buildDeterministicRecommendation(recommendation);

        recordAgentRun(investigation, "Recommendation Evaluator", result.mode(), result.provider(), result.model(),
                result.isSuccess() ? AgentRunStatus.DONE : AgentRunStatus.FAILED, summary, result.error());

        recommendation.setSummary(summary);
        return recommendationRepository.save(recommendation);
    }

    private String describePolicyFindings(KnowledgeDtos.SearchResponse policyFindings) {
        if (policyFindings.results().isEmpty()) {
            return "(none found)";
        }
        return policyFindings.results().stream()
                .map(r -> "\"" + r.title() + "\" - " + r.snippet())
                .reduce((a, b) -> a + " / " + b).orElse("(none found)");
    }

    private String describeAction(AiRecommendation r) {
        return switch (r.getActionType()) {
            case TRANSFER -> "Transfer " + r.getActionQuantity() + " units of " + r.getActionProductSku()
                    + " from " + r.getActionSourceWarehouse() + " to " + r.getActionDestWarehouse() + ".";
            case PURCHASE_ORDER -> "Create and submit a purchase order for " + r.getActionQuantity() + " units of "
                    + r.getActionProductSku() + " to warehouse " + r.getActionDestWarehouse()
                    + " with supplier " + r.getActionSupplierId() + ".";
            case NONE -> "No automatic action; no stockout risk or no viable transfer/supplier found.";
        };
    }

    /**
     * Deterministic action selection - this is what ApprovalService actually executes on
     * approval, never the LLM's free-text summary (README §5 tool-safety rule). Prefers a
     * transfer from a warehouse with full coverage of the shortfall; falls back to a
     * purchase order with the top-ranked alternate supplier; otherwise no automatic action.
     */
    private void determineAction(AiRecommendation recommendation, String tenantId, ShipmentDelayContext context,
                                  InventoryAnalysis inventoryAnalysis, SupplierAnalysis supplierAnalysis) {
        if (!inventoryAnalysis.stockoutRisk()) {
            recommendation.setActionType(RecommendedActionType.NONE);
            return;
        }
        int shortfall = (int) Math.ceil(inventoryAnalysis.shortfallUnits());

        try {
            var candidate = operationsServiceClient.listWarehouseBalances(tenantId, context.productSku()).stream()
                    .filter(b -> !b.warehouseCode().equals(context.warehouseCode()))
                    .filter(b -> b.availableUnits() >= shortfall)
                    .max((a, b) -> Integer.compare(a.availableUnits(), b.availableUnits()));
            if (candidate.isPresent()) {
                recommendation.setActionType(RecommendedActionType.TRANSFER);
                recommendation.setActionProductSku(context.productSku());
                recommendation.setActionQuantity(shortfall);
                recommendation.setActionSourceWarehouse(candidate.get().warehouseCode());
                recommendation.setActionDestWarehouse(context.warehouseCode());
                return;
            }
        } catch (Exception e) {
            log.warn("Operations Service unreachable while looking for transfer candidates: {}", e.getMessage());
        }

        if (!supplierAnalysis.alternateSuppliers().isEmpty()) {
            var top = supplierAnalysis.alternateSuppliers().get(0);
            recommendation.setActionType(RecommendedActionType.PURCHASE_ORDER);
            recommendation.setActionProductSku(context.productSku());
            recommendation.setActionQuantity(shortfall);
            recommendation.setActionDestWarehouse(context.warehouseCode());
            recommendation.setActionSupplierId(top.id());
            recommendation.setActionUnitPrice(BigDecimal.valueOf(8.5));
            return;
        }

        recommendation.setActionType(RecommendedActionType.NONE);
    }

    private String buildDeterministicRecommendation(AiRecommendation recommendation) {
        return switch (recommendation.getActionType()) {
            case NONE -> "No stockout risk detected, or no viable transfer/supplier found; no automatic action proposed.";
            case TRANSFER -> "Stockout risk detected for " + recommendation.getActionProductSku()
                    + " at " + recommendation.getActionDestWarehouse() + " (shortfall ~" + recommendation.getActionQuantity()
                    + " units). Recommend transferring " + recommendation.getActionQuantity() + " units from "
                    + recommendation.getActionSourceWarehouse() + ", which has sufficient available stock.";
            case PURCHASE_ORDER -> "Stockout risk detected for " + recommendation.getActionProductSku()
                    + " at " + recommendation.getActionDestWarehouse() + " (shortfall ~" + recommendation.getActionQuantity()
                    + " units). No warehouse has enough surplus stock to transfer; recommend an emergency purchase order"
                    + " for " + recommendation.getActionQuantity() + " units with the top-ranked alternate supplier.";
        };
    }

    private BigDecimal estimateCost(InventoryAnalysis inventoryAnalysis) {
        if (!inventoryAnalysis.stockoutRisk()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(inventoryAnalysis.shortfallUnits() * 8.5).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void transition(AiInvestigation investigation, InvestigationStatus target) {
        if (!investigation.getStatus().canTransitionTo(target)) {
            throw new com.supplymind.aiops.common.InvalidStateTransitionException(
                    "Cannot transition investigation from " + investigation.getStatus() + " to " + target);
        }
        investigation.setStatus(target);
        if (target == InvestigationStatus.CLOSED) {
            investigation.setClosedAt(Instant.now());
        }
        investigationRepository.save(investigation);
    }

    private void recordAgentRun(AiInvestigation investigation, String agentName, AiMode mode, String provider,
                                 String model, AgentRunStatus status, String output, String error) {
        AiAgentRun run = new AiAgentRun();
        run.setTenantId(investigation.getTenantId());
        run.setInvestigationId(investigation.getId());
        run.setAgentName(agentName);
        run.setMode(mode);
        run.setProvider(provider);
        run.setModel(model);
        run.setStatus(status);
        run.setOutput(output);
        run.setErrorMessage(error);
        run.setFinishedAt(Instant.now());
        agentRunRepository.save(run);
    }

    private void saveEvidence(AiInvestigation investigation, String sourceType, String sourceId, String description) {
        EvidenceReference evidence = new EvidenceReference();
        evidence.setTenantId(investigation.getTenantId());
        evidence.setInvestigationId(investigation.getId());
        evidence.setSourceType(sourceType);
        evidence.setSourceId(sourceId);
        evidence.setDescription(description);
        evidenceRepository.save(evidence);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
