package com.supplymind.aiops.recommendation;

import com.supplymind.aiops.investigation.InvestigationDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationQueryController {

    private final RecommendationQueryService queryService;

    public RecommendationQueryController(RecommendationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public List<InvestigationDtos.RecommendationResponse> list(
            @RequestParam(name = "status", defaultValue = "PENDING") String status) {
        // Only PENDING is wired up today - this is the Approvals queue (PRODUCT_SPEC §4.8),
        // which only ever needs pending items; extend if other statuses need listing later.
        return queryService.listPending().stream().map(InvestigationDtos.RecommendationResponse::from).toList();
    }
}
