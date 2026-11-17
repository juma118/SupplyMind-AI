package com.supplymind.core.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductDtos.ProductResponse> list() {
        return service.list().stream().map(ProductDtos.ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductDtos.ProductResponse get(@PathVariable UUID id) {
        return ProductDtos.ProductResponse.from(service.get(id));
    }

    // PRODUCT_SPEC.md §3: Warehouse Manager owns Products & Inventory.
    @PreAuthorize("hasRole('warehouse-manager') or hasRole('platform-admin')")
    @PostMapping
    public ResponseEntity<ProductDtos.ProductResponse> create(@Valid @RequestBody ProductDtos.CreateProductRequest request) {
        Product created = service.create(request);
        ProductDtos.ProductResponse body = ProductDtos.ProductResponse.from(created);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.getId()))
                .body(body);
    }

    @PreAuthorize("hasRole('warehouse-manager') or hasRole('platform-admin')")
    @PutMapping("/{id}")
    public ProductDtos.ProductResponse update(@PathVariable UUID id,
                                               @Valid @RequestBody ProductDtos.CreateProductRequest request) {
        return ProductDtos.ProductResponse.from(service.update(id, request));
    }
}
