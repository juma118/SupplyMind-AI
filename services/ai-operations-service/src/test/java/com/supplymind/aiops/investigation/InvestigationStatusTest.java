package com.supplymind.aiops.investigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the single source of truth for the workflow in README §5 - a bug here means an
 * investigation could silently skip human approval or get stuck forever, so every legal
 * and every illegal transition is worth asserting explicitly rather than sampling a few.
 */
class InvestigationStatusTest {

    @Test
    void happyPathApprovalFlowIsAllowedStepByStep() {
        assertThat(InvestigationStatus.RECEIVED.canTransitionTo(InvestigationStatus.PLANNING)).isTrue();
        assertThat(InvestigationStatus.PLANNING.canTransitionTo(InvestigationStatus.INVESTIGATING)).isTrue();
        assertThat(InvestigationStatus.INVESTIGATING.canTransitionTo(InvestigationStatus.EVALUATING)).isTrue();
        assertThat(InvestigationStatus.EVALUATING.canTransitionTo(InvestigationStatus.RECOMMENDATION_READY)).isTrue();
        assertThat(InvestigationStatus.RECOMMENDATION_READY.canTransitionTo(InvestigationStatus.AWAITING_APPROVAL)).isTrue();
        assertThat(InvestigationStatus.AWAITING_APPROVAL.canTransitionTo(InvestigationStatus.APPROVED)).isTrue();
        assertThat(InvestigationStatus.APPROVED.canTransitionTo(InvestigationStatus.EXECUTING)).isTrue();
        assertThat(InvestigationStatus.EXECUTING.canTransitionTo(InvestigationStatus.COMPLETED)).isTrue();
        assertThat(InvestigationStatus.COMPLETED.canTransitionTo(InvestigationStatus.CLOSED)).isTrue();
    }

    @Test
    void cannotSkipApprovalAndJumpStraightFromRecommendationReadyToExecuting() {
        assertThat(InvestigationStatus.RECOMMENDATION_READY.canTransitionTo(InvestigationStatus.EXECUTING)).isFalse();
    }

    @Test
    void cannotSkipAwaitingApprovalAndJumpStraightFromReadyToApproved() {
        assertThat(InvestigationStatus.RECOMMENDATION_READY.canTransitionTo(InvestigationStatus.APPROVED)).isFalse();
    }

    @Test
    void rejectedGoesStraightToClosedNeverToExecuting() {
        assertThat(InvestigationStatus.REJECTED.canTransitionTo(InvestigationStatus.CLOSED)).isTrue();
        assertThat(InvestigationStatus.REJECTED.canTransitionTo(InvestigationStatus.EXECUTING)).isFalse();
    }

    @Test
    void revisionRequiredReturnsToPlanningRatherThanReRunningTheWholePipeline() {
        assertThat(InvestigationStatus.REVISION_REQUIRED.canTransitionTo(InvestigationStatus.PLANNING)).isTrue();
        assertThat(InvestigationStatus.REVISION_REQUIRED.canTransitionTo(InvestigationStatus.AWAITING_APPROVAL)).isFalse();
    }

    @Test
    void closedIsTerminalAndAllowsNoFurtherTransitions() {
        for (InvestigationStatus target : InvestigationStatus.values()) {
            assertThat(InvestigationStatus.CLOSED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void completedCanOnlyMoveToClosed() {
        for (InvestigationStatus target : InvestigationStatus.values()) {
            boolean allowed = InvestigationStatus.COMPLETED.canTransitionTo(target);
            if (target == InvestigationStatus.CLOSED) {
                assertThat(allowed).isTrue();
            } else {
                assertThat(allowed).as("COMPLETED -> %s should be disallowed", target).isFalse();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(InvestigationStatus.class)
    void noStateCanTransitionToItself(InvestigationStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }
}
