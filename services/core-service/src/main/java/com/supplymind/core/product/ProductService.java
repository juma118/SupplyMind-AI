package com.supplymind.core.product;

import com.supplymind.core.common.NotFoundException;
import com.supplymind.core.common.TenantContext;
import com.supplymind.core.common.TenantRequiredException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> list() {
        return repository.findAllByTenantId(requireTenant());
    }

    public Product get(UUID id) {
        return repository.findByIdAndTenantId(id, requireTenant())
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    public Product create(ProductDtos.CreateProductRequest request) {
        String tenantId = requireTenant();
        if (repository.existsByTenantIdAndSku(tenantId, request.sku())) {
            throw new IllegalArgumentException("SKU already exists for this tenant: " + request.sku());
        }
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setSku(request.sku());
        product.setName(request.name());
        product.setUnitCost(request.unitCost());
        product.setReorderPoint(request.reorderPoint());
        return repository.save(product);
    }

    public Product update(UUID id, ProductDtos.CreateProductRequest request) {
        Product existing = get(id);
        existing.setName(request.name());
        existing.setUnitCost(request.unitCost());
        existing.setReorderPoint(request.reorderPoint());
        return repository.save(existing);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
