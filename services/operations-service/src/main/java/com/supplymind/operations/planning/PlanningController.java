package com.supplymind.operations.planning;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    private final PlanningService service;

    public PlanningController(PlanningService service) {
        this.service = service;
    }

    @PostMapping("/demand-profile")
    public PlanningDtos.DemandProfileResponse setDemandProfile(@Valid @RequestBody PlanningDtos.DemandProfileRequest request) {
        return service.setDemandProfile(request);
    }

    @PostMapping("/eoq")
    public PlanningDtos.EoqResponse economicOrderQuantity(@Valid @RequestBody PlanningDtos.EoqRequest request) {
        return service.economicOrderQuantity(request);
    }

    @GetMapping("/stockout-risk")
    public List<PlanningDtos.StockoutRiskResponse> stockoutRisk() {
        return service.stockoutRisk();
    }
}
