package com.supplymind.aiops.approval;

import com.supplymind.aiops.investigation.InvestigationDtos;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // README §5/§2: approving a recommendation is the actual human gate on a consequential
    // action, and the audit trail's actor must be as trustworthy as tenant - so actorId
    // comes from the verified JWT's preferred_username, never the request body (a client
    // could otherwise claim to be anyone in its own audit record).
    @PreAuthorize("hasRole('supply-chain-manager') or hasRole('platform-admin')")
    @PostMapping("/{id}/approve")
    public InvestigationDtos.RecommendationResponse approve(@PathVariable UUID id,
                                                              @AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody ApprovalDtos.DecisionRequest request) {
        return InvestigationDtos.RecommendationResponse.from(
                approvalService.approve(id, actorFrom(jwt), request.comment()));
    }

    @PreAuthorize("hasRole('supply-chain-manager') or hasRole('platform-admin')")
    @PostMapping("/{id}/reject")
    public InvestigationDtos.RecommendationResponse reject(@PathVariable UUID id,
                                                             @AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody ApprovalDtos.DecisionRequest request) {
        return InvestigationDtos.RecommendationResponse.from(
                approvalService.reject(id, actorFrom(jwt), request.comment()));
    }

    private String actorFrom(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }
}
