package com.supplymind.core.supplier;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierDtos.SupplierResponse> list() {
        return service.list().stream().map(SupplierDtos.SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SupplierDtos.SupplierResponse get(@PathVariable UUID id) {
        return SupplierDtos.SupplierResponse.from(service.get(id));
    }

    @GetMapping("/{id}/performance")
    public SupplierDtos.SupplierPerformanceResponse performance(@PathVariable UUID id) {
        return SupplierDtos.SupplierPerformanceResponse.from(service.get(id));
    }

    // PRODUCT_SPEC.md §3: Procurement Manager owns Suppliers.
    @PreAuthorize("hasRole('procurement-manager') or hasRole('platform-admin')")
    @PostMapping
    public ResponseEntity<SupplierDtos.SupplierResponse> create(@Valid @RequestBody SupplierDtos.CreateSupplierRequest request) {
        Supplier created = service.create(request);
        SupplierDtos.SupplierResponse body = SupplierDtos.SupplierResponse.from(created);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + created.getId())).body(body);
    }
}
