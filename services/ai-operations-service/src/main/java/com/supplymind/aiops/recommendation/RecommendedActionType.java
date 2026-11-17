package com.supplymind.aiops.recommendation;

/**
 * The structured, deterministic action behind a recommendation's human-readable summary.
 * The LLM writes the summary text (README §5.1); it never decides this field - that's
 * chosen by deterministic Java logic in InvestigationWorkflowService, which is what
 * ApprovalService actually executes against Operations Service on approval.
 */
public enum RecommendedActionType {
    TRANSFER, PURCHASE_ORDER, NONE
}
