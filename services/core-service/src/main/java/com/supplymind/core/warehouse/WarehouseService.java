package com.supplymind.core.warehouse;

import com.supplymind.core.common.NotFoundException;
import com.supplymind.core.common.TenantContext;
import com.supplymind.core.common.TenantRequiredException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository repository;

    public WarehouseService(WarehouseRepository repository) {
        this.repository = repository;
    }

    public List<Warehouse> list() {
        return repository.findAllByTenantId(requireTenant());
    }

    public Warehouse get(UUID id) {
        return repository.findByIdAndTenantId(id, requireTenant())
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + id));
    }

    public Warehouse create(WarehouseDtos.CreateWarehouseRequest request) {
        String tenantId = requireTenant();
        if (repository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new IllegalArgumentException("Warehouse code already exists for this tenant: " + request.code());
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setTenantId(tenantId);
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setCity(request.city());
        warehouse.setCountry(request.country());
        warehouse.setCapacityUnits(request.capacityUnits());
        return repository.save(warehouse);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
