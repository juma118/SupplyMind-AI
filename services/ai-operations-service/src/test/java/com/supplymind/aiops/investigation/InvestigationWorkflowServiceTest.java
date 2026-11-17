package com.supplymind.aiops.investigation;

import com.supplymind.aiops.agent.*;
import com.supplymind.aiops.common.AuditService;
import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.evidence.EvidenceRepository;
import com.supplymind.aiops.execution.OperationsServiceClient;
import com.supplymind.aiops.knowledge.KnowledgeDtos;
import com.supplymind.aiops.knowledge.KnowledgeService;
import com.supplymind.aiops.recommendation.AiRecommendation;
import com.supplymind.aiops.recommendation.RecommendationRepository;
import com.supplymind.aiops.recommendation.RecommendedActionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * This class has already had two real bugs in it this session: a recommendation's
 * free-text summary disagreeing with its structured action, and a PO list endpoint's
 * lazy-loading crash in a sibling class caused by the same "build the DTO outside the
 * transaction" pattern. These tests exist specifically to pin the fixed behavior down -
 * summary text must always describe the exact action that will be executed on approval,
 * never a different one the LLM free-associated.
 */
class InvestigationWorkflowServiceTest {

    private InvestigationRepository investigationRepository;
    private AgentRunRepository agentRunRepository;
    private EvidenceRepository evidenceRepository;
    private RecommendationRepository recommendationRepository;
    private AuditService auditService;
    private AiGatewayService aiGatewayService;
    private SupplierIntelligenceAgent supplierIntelligenceAgent;
    private OperationsServiceClient operationsServiceClient;
    private KnowledgeService knowledgeService;
    private InvestigationWorkflowService workflowService;

    private static final String TENANT = "tenant-test";
    private static final ShipmentDelayContext AT_RISK_CONTEXT =
            new ShipmentDelayContext("SHP-1", "WID-1", 72, "WH-A", 2400, 1200.0);

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);

        investigationRepository = mock(InvestigationRepository.class);
        agentRunRepository = mock(AgentRunRepository.class);
        evidenceRepository = mock(EvidenceRepository.class);
        recommendationRepository = mock(RecommendationRepository.class);
        auditService = mock(AuditService.class);
        aiGatewayService = mock(AiGatewayService.class);
        supplierIntelligenceAgent = mock(SupplierIntelligenceAgent.class);
        operationsServiceClient = mock(OperationsServiceClient.class);
        knowledgeService = mock(KnowledgeService.class);
        when(knowledgeService.search(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new KnowledgeDtos.SearchResponse(List.of(), false, "no documents indexed in this test"));

        when(investigationRepository.save(any())).thenAnswer(inv -> {
            AiInvestigation i = inv.getArgument(0);
            if (i.getId() == null) {
                // Simulates what @PrePersist does on a real JPA save - this entity is
                // being built manually here, not through Hibernate, so that hook never runs.
                i.setStatus(InvestigationStatus.RECEIVED);
                i = spyWithId(i);
            }
            return i;
        });
        when(recommendationRepository.save(any())).thenAnswer(inv -> {
            AiRecommendation r = inv.getArgument(0);
            if (r.getId() == null) {
                AiRecommendation spy = spy(r);
                when(spy.getId()).thenReturn(java.util.UUID.randomUUID());
                return spy;
            }
            return r;
        });
        // Both agent calls fail - exercises the deterministic fallback path, which is
        // what actually ran for most of this session's manual testing too.
        when(aiGatewayService.call(anyString(), anyString())).thenReturn(AiCallResult.fallback("no provider configured"));

        workflowService = new InvestigationWorkflowService(
                investigationRepository, agentRunRepository, evidenceRepository, recommendationRepository,
                auditService, aiGatewayService, new InventoryIntelligenceAgent(), supplierIntelligenceAgent,
                operationsServiceClient, knowledgeService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void recommendsTransferAndSummaryMatchesWhenAWarehouseHasEnoughSurplus() {
        when(operationsServiceClient.listWarehouseBalances(TENANT, "WID-1")).thenReturn(List.of(
                new OperationsServiceClient.WarehouseBalance("WH-C", "WID-1", 5000)));
        when(supplierIntelligenceAgent.analyze(TENANT)).thenReturn(new SupplierAnalysis(List.of(), "no suppliers checked"));

        workflowService.runShipmentDelayInvestigation(AT_RISK_CONTEXT);

        AiRecommendation saved = captureRecommendation();
        assertThat(saved.getActionType()).isEqualTo(RecommendedActionType.TRANSFER);
        assertThat(saved.getActionSourceWarehouse()).isEqualTo("WH-C");
        assertThat(saved.getActionQuantity()).isEqualTo(1200);
        // The bug this session: summary text said "purchase order" while the action was
        // actually TRANSFER. Must never regress.
        assertThat(saved.getSummary()).contains("transferring").doesNotContain("purchase order");
    }

    @Test
    void recommendsPurchaseOrderAndSummaryMatchesWhenNoWarehouseHasEnoughSurplus() {
        when(operationsServiceClient.listWarehouseBalances(TENANT, "WID-1")).thenReturn(List.of(
                new OperationsServiceClient.WarehouseBalance("WH-C", "WID-1", 100))); // not enough
        when(supplierIntelligenceAgent.analyze(TENANT)).thenReturn(new SupplierAnalysis(
                List.of(new com.supplymind.aiops.agent.CoreServiceClient.SupplierSummary(
                        "supplier-1", "Nova Components", "Electronics", new BigDecimal("0.98"), 4)),
                "Nova Components ranked top"));

        workflowService.runShipmentDelayInvestigation(AT_RISK_CONTEXT);

        AiRecommendation saved = captureRecommendation();
        assertThat(saved.getActionType()).isEqualTo(RecommendedActionType.PURCHASE_ORDER);
        assertThat(saved.getActionSupplierId()).isEqualTo("supplier-1");
        assertThat(saved.getSummary()).contains("purchase order").doesNotContain("transferring");
    }

    @Test
    void noActionWhenNoStockoutRisk() {
        var noRiskContext = new ShipmentDelayContext("SHP-2", "WID-1", 1, "WH-A", 10_000, 100.0);
        when(supplierIntelligenceAgent.analyze(TENANT)).thenReturn(new SupplierAnalysis(List.of(), "n/a"));

        workflowService.runShipmentDelayInvestigation(noRiskContext);

        AiRecommendation saved = captureRecommendation();
        assertThat(saved.getActionType()).isEqualTo(RecommendedActionType.NONE);
        verify(operationsServiceClient, never()).listWarehouseBalances(any(), any());
    }

    @Test
    void investigationEndsInAwaitingApprovalNeverSkippingToExecuting() {
        when(supplierIntelligenceAgent.analyze(TENANT)).thenReturn(new SupplierAnalysis(List.of(), "n/a"));

        AiInvestigation result = workflowService.runShipmentDelayInvestigation(AT_RISK_CONTEXT);

        assertThat(result.getStatus()).isEqualTo(InvestigationStatus.AWAITING_APPROVAL);
    }

    private AiRecommendation captureRecommendation() {
        var captor = org.mockito.ArgumentCaptor.forClass(AiRecommendation.class);
        verify(recommendationRepository).save(captor.capture());
        return captor.getValue();
    }

    private AiInvestigation spyWithId(AiInvestigation i) {
        AiInvestigation spy = spy(i);
        when(spy.getId()).thenReturn(java.util.UUID.randomUUID());
        return spy;
    }
}
