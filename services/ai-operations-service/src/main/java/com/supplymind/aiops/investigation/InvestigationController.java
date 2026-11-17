package com.supplymind.aiops.investigation;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {

    private final InvestigationWorkflowService workflowService;
    private final InvestigationQueryService queryService;

    public InvestigationController(InvestigationWorkflowService workflowService, InvestigationQueryService queryService) {
        this.workflowService = workflowService;
        this.queryService = queryService;
    }

    @PostMapping("/shipment-delay")
    public ResponseEntity<InvestigationDtos.InvestigationResponse> createShipmentDelayInvestigation(
            @Valid @RequestBody ShipmentDelayContext context) {
        AiInvestigation investigation = workflowService.runShipmentDelayInvestigation(context);
        InvestigationDtos.InvestigationResponse body = InvestigationDtos.InvestigationResponse.from(investigation);
        return ResponseEntity.created(URI.create("/api/v1/investigations/" + investigation.getId()))
                .body(body);
    }

    @GetMapping
    public List<InvestigationDtos.InvestigationResponse> list() {
        return queryService.list().stream().map(InvestigationDtos.InvestigationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public InvestigationDtos.InvestigationResponse get(@PathVariable UUID id) {
        return InvestigationDtos.InvestigationResponse.from(queryService.get(id));
    }

    @GetMapping("/{id}/timeline")
    public InvestigationDtos.InvestigationDetailResponse timeline(@PathVariable UUID id) {
        return queryService.getDetail(id);
    }

    @GetMapping("/{id}/recommendations")
    public List<InvestigationDtos.RecommendationResponse> recommendations(@PathVariable UUID id) {
        return queryService.getRecommendations(id);
    }
}
