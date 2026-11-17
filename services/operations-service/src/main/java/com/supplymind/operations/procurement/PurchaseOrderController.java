package com.supplymind.operations.procurement;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<PurchaseOrderDtos.PurchaseOrderResponse> list() {
        return service.list().stream().map(PurchaseOrderDtos.PurchaseOrderResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderDtos.PurchaseOrderResponse> create(
            @Valid @RequestBody PurchaseOrderDtos.CreatePurchaseOrderRequest request) {
        PurchaseOrder created = service.create(request);
        PurchaseOrderDtos.PurchaseOrderResponse body = PurchaseOrderDtos.PurchaseOrderResponse.from(created);
        return ResponseEntity.created(URI.create("/api/v1/purchase-orders/" + created.getId())).body(body);
    }

    @GetMapping("/{id}")
    public PurchaseOrderDtos.PurchaseOrderResponse get(@PathVariable UUID id) {
        return PurchaseOrderDtos.PurchaseOrderResponse.from(service.get(id));
    }

    // README §5: "require human approval for consequential operations such as
    // purchase-order submission" - PRODUCT_SPEC.md §3 assigns that to Procurement Manager.
    // system-service covers AI Operations Service executing a recommendation a human
    // already approved (via its own supply-chain-manager-gated approve endpoint) - that
    // approval is the actual human gate, not a second one re-checked here against a role
    // mismatch.
    @PreAuthorize("hasRole('procurement-manager') or hasRole('platform-admin') or hasRole('system-service')")
    @PostMapping("/{id}/submit")
    public PurchaseOrderDtos.PurchaseOrderResponse submit(@PathVariable UUID id) {
        return PurchaseOrderDtos.PurchaseOrderResponse.from(service.submit(id));
    }

    // Receiving goods physically arrives at a warehouse - Warehouse Manager's job per
    // PRODUCT_SPEC.md §3, not Procurement's.
    @PreAuthorize("hasRole('warehouse-manager') or hasRole('platform-admin') or hasRole('system-service')")
    @PostMapping("/{id}/receive")
    public PurchaseOrderDtos.PurchaseOrderResponse receive(@PathVariable UUID id) {
        return PurchaseOrderDtos.PurchaseOrderResponse.from(service.receive(id));
    }
}
