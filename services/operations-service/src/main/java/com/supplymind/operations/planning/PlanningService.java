package com.supplymind.operations.planning;

import com.supplymind.operations.common.NotFoundException;
import com.supplymind.operations.common.TenantContext;
import com.supplymind.operations.common.TenantRequiredException;
import com.supplymind.operations.inventory.InventoryBalance;
import com.supplymind.operations.inventory.InventoryBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * README §6: demand forecasting and reorder-point intelligence, built on the deterministic
 * formulas in ReorderCalculationService. Persisting the demand profile onto
 * InventoryBalance is what finally makes reorder_point a real, non-null value - before
 * this, nothing in the codebase ever wrote to that column, so the "low stock" view was
 * silently empty regardless of actual stock levels.
 */
@Service
public class PlanningService {

    private final InventoryBalanceRepository balanceRepository;
    private final ReorderCalculationService calculationService;

    public PlanningService(InventoryBalanceRepository balanceRepository, ReorderCalculationService calculationService) {
        this.balanceRepository = balanceRepository;
        this.calculationService = calculationService;
    }

    @Transactional
    public PlanningDtos.DemandProfileResponse setDemandProfile(PlanningDtos.DemandProfileRequest request) {
        String tenantId = requireTenant();
        InventoryBalance balance = balanceRepository
                .findByTenantIdAndWarehouseCodeAndProductSku(tenantId, request.warehouseCode(), request.productSku())
                .orElseThrow(() -> new NotFoundException(
                        "No inventory balance for " + request.productSku() + " at " + request.warehouseCode()
                                + " - transfer, adjust, or receive stock there first"));

        var result = calculationService.calculateReorderPoint(request.avgDailyDemand(), request.leadTimeDays(),
                request.safetyStockOverride(), request.demandStdDev(), request.serviceLevelZ());

        balance.setAvgDailyDemand(request.avgDailyDemand());
        balance.setLeadTimeDays(request.leadTimeDays());
        balance.setSafetyStock(result.safetyStock());
        balance.setReorderPoint(result.reorderPoint());
        balanceRepository.save(balance);

        return new PlanningDtos.DemandProfileResponse(request.warehouseCode(), request.productSku(),
                request.avgDailyDemand(), request.leadTimeDays(), result.safetyStock(), result.reorderPoint());
    }

    public PlanningDtos.EoqResponse economicOrderQuantity(PlanningDtos.EoqRequest request) {
        int eoq = calculationService.economicOrderQuantity(
                request.annualDemand(), request.orderCost(), request.holdingCostPerUnitPerYear());
        return new PlanningDtos.EoqResponse(eoq);
    }

    public List<PlanningDtos.StockoutRiskResponse> stockoutRisk() {
        String tenantId = requireTenant();
        return balanceRepository.findAllByTenantId(tenantId).stream()
                .filter(b -> b.getAvgDailyDemand() != null && b.getAvgDailyDemand().signum() > 0)
                .map(this::toStockoutRisk)
                .toList();
    }

    private PlanningDtos.StockoutRiskResponse toStockoutRisk(InventoryBalance b) {
        double daysOfCover = b.getAvailableUnits() / b.getAvgDailyDemand().doubleValue();
        boolean belowReorderPoint = b.getReorderPoint() != null && b.getAvailableUnits() < b.getReorderPoint();
        boolean coverShorterThanLeadTime = b.getLeadTimeDays() != null && daysOfCover < b.getLeadTimeDays();

        return new PlanningDtos.StockoutRiskResponse(b.getWarehouseCode(), b.getProductSku(), b.getAvailableUnits(),
                b.getAvgDailyDemand(), daysOfCover, b.getLeadTimeDays(), b.getReorderPoint(),
                belowReorderPoint, coverShorterThanLeadTime);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
