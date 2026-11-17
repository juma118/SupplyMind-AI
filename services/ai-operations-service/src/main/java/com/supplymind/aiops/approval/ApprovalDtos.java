package com.supplymind.aiops.approval;

public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    // actorId is deliberately absent - it comes from the verified JWT (see
    // ApprovalController), never trusted from the request body.
    public record DecisionRequest(String comment) {
    }
}
