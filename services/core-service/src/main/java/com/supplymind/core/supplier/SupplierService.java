package com.supplymind.core.supplier;

import com.supplymind.core.common.NotFoundException;
import com.supplymind.core.common.TenantContext;
import com.supplymind.core.common.TenantRequiredException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    public List<Supplier> list() {
        return repository.findAllByTenantId(requireTenant());
    }

    public Supplier get(UUID id) {
        return repository.findByIdAndTenantId(id, requireTenant())
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }

    public Supplier create(SupplierDtos.CreateSupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setTenantId(requireTenant());
        supplier.setName(request.name());
        supplier.setCategory(request.category());
        supplier.setOnTimeRate(request.onTimeRate());
        supplier.setAvgLeadTimeDays(request.avgLeadTimeDays());
        return repository.save(supplier);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
