package com.supplymind.core.warehouse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping
    public List<WarehouseDtos.WarehouseResponse> list() {
        return service.list().stream().map(WarehouseDtos.WarehouseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WarehouseDtos.WarehouseResponse get(@PathVariable UUID id) {
        return WarehouseDtos.WarehouseResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<WarehouseDtos.WarehouseResponse> create(@Valid @RequestBody WarehouseDtos.CreateWarehouseRequest request) {
        Warehouse created = service.create(request);
        WarehouseDtos.WarehouseResponse body = WarehouseDtos.WarehouseResponse.from(created);
        return ResponseEntity.created(URI.create("/api/v1/warehouses/" + created.getId())).body(body);
    }
}
