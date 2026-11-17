package com.supplymind.operations.inventory;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryDtos.BalanceResponse> list() {
        return service.list().stream().map(InventoryDtos.BalanceResponse::from).toList();
    }

    @GetMapping("/low-stock")
    public List<InventoryDtos.BalanceResponse> lowStock() {
        return service.lowStock().stream().map(InventoryDtos.BalanceResponse::from).toList();
    }

    @GetMapping("/{productSku}/warehouses")
    public List<InventoryDtos.BalanceResponse> byProduct(@PathVariable String productSku) {
        return service.byProduct(productSku).stream().map(InventoryDtos.BalanceResponse::from).toList();
    }

    // README §5: "require human approval for consequential operations such as ...
    // inventory transfer execution." system-service covers AI Operations Service
    // executing an already-approved recommendation (see PurchaseOrderController).
    @PreAuthorize("hasRole('warehouse-manager') or hasRole('platform-admin') or hasRole('system-service')")
    @PostMapping("/transfers")
    public void transfer(@Valid @RequestBody InventoryDtos.TransferRequest request) {
        service.transfer(request);
    }

    @PreAuthorize("hasRole('warehouse-manager') or hasRole('platform-admin') or hasRole('system-service')")
    @PostMapping("/adjustments")
    public void adjust(@Valid @RequestBody InventoryDtos.AdjustmentRequest request) {
        service.adjust(request);
    }
}
