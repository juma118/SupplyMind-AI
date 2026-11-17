package com.supplymind.aiops.investigation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mirrors the workflow state machine in README §5:
 * RECEIVED -> PLANNING -> INVESTIGATING -> EVALUATING -> RECOMMENDATION_READY ->
 * AWAITING_APPROVAL -> APPROVED/REJECTED/REVISION_REQUIRED -> EXECUTING -> COMPLETED/FAILED -> CLOSED
 */
public enum InvestigationStatus {
    RECEIVED,
    PLANNING,
    INVESTIGATING,
    EVALUATING,
    RECOMMENDATION_READY,
    AWAITING_APPROVAL,
    APPROVED,
    REJECTED,
    REVISION_REQUIRED,
    EXECUTING,
    COMPLETED,
    FAILED,
    CLOSED;

    private static final Map<InvestigationStatus, Set<InvestigationStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(RECEIVED, EnumSet.of(PLANNING, FAILED)),
            Map.entry(PLANNING, EnumSet.of(INVESTIGATING, FAILED)),
            Map.entry(INVESTIGATING, EnumSet.of(EVALUATING, FAILED)),
            Map.entry(EVALUATING, EnumSet.of(RECOMMENDATION_READY, FAILED)),
            Map.entry(RECOMMENDATION_READY, EnumSet.of(AWAITING_APPROVAL, FAILED)),
            Map.entry(AWAITING_APPROVAL, EnumSet.of(APPROVED, REJECTED, REVISION_REQUIRED)),
            Map.entry(REVISION_REQUIRED, EnumSet.of(PLANNING, CLOSED)),
            Map.entry(APPROVED, EnumSet.of(EXECUTING)),
            Map.entry(REJECTED, EnumSet.of(CLOSED)),
            Map.entry(EXECUTING, EnumSet.of(COMPLETED, FAILED)),
            Map.entry(COMPLETED, EnumSet.of(CLOSED)),
            Map.entry(FAILED, EnumSet.of(CLOSED)),
            Map.entry(CLOSED, EnumSet.noneOf(InvestigationStatus.class))
    );

    public boolean canTransitionTo(InvestigationStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
