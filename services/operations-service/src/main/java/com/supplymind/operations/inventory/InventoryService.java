package com.supplymind.operations.inventory;

import com.supplymind.operations.common.InsufficientStockException;
import com.supplymind.operations.common.TenantContext;
import com.supplymind.operations.common.TenantRequiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * README §7 data-integrity principles enforced here: unique balance per
 * (tenant, warehouse, product), movements kept as an immutable ledger, and optimistic
 * concurrency (via @Version on InventoryBalance) for concurrent updates to the same row.
 */
@Service
public class InventoryService {

    private final InventoryBalanceRepository balanceRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryService(InventoryBalanceRepository balanceRepository, InventoryMovementRepository movementRepository) {
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
    }

    public List<InventoryBalance> list() {
        return balanceRepository.findAllByTenantId(requireTenant());
    }

    public List<InventoryBalance> lowStock() {
        return balanceRepository.findAllByTenantId(requireTenant()).stream()
                .filter(b -> b.getReorderPoint() != null && b.getAvailableUnits() < b.getReorderPoint())
                .toList();
    }

    public List<InventoryBalance> byProduct(String productSku) {
        return balanceRepository.findAllByTenantIdAndProductSku(requireTenant(), productSku);
    }

    @Transactional
    public void transfer(InventoryDtos.TransferRequest request) {
        String tenantId = requireTenant();

        InventoryBalance source = getOrCreate(tenantId, request.sourceWarehouseCode(), request.productSku());
        if (source.getAvailableUnits() < request.quantity()) {
            throw new InsufficientStockException(String.format(
                    "Warehouse %s has only %d available units of %s, cannot transfer %d",
                    request.sourceWarehouseCode(), source.getAvailableUnits(), request.productSku(), request.quantity()));
        }
        source.setAvailableUnits(source.getAvailableUnits() - request.quantity());
        balanceRepository.save(source);
        saveMovement(tenantId, request.sourceWarehouseCode(), request.productSku(), MovementType.TRANSFER_OUT,
                request.quantity(), request.referenceType(), request.referenceId());

        InventoryBalance dest = getOrCreate(tenantId, request.destWarehouseCode(), request.productSku());
        dest.setAvailableUnits(dest.getAvailableUnits() + request.quantity());
        balanceRepository.save(dest);
        saveMovement(tenantId, request.destWarehouseCode(), request.productSku(), MovementType.TRANSFER_IN,
                request.quantity(), request.referenceType(), request.referenceId());
    }

    @Transactional
    public void adjust(InventoryDtos.AdjustmentRequest request) {
        String tenantId = requireTenant();
        InventoryBalance balance = getOrCreate(tenantId, request.warehouseCode(), request.productSku());

        int newAvailable = balance.getAvailableUnits() + request.quantityDelta();
        if (newAvailable < 0) {
            throw new InsufficientStockException(String.format(
                    "Adjustment would make available units negative for %s at %s (current %d, delta %d)",
                    request.productSku(), request.warehouseCode(), balance.getAvailableUnits(), request.quantityDelta()));
        }
        balance.setAvailableUnits(newAvailable);
        balanceRepository.save(balance);
        saveMovement(tenantId, request.warehouseCode(), request.productSku(), MovementType.ADJUSTMENT,
                request.quantityDelta(), "ADJUSTMENT", request.reason());
    }

    @Transactional
    public void receive(String tenantId, String warehouseCode, String productSku, int quantity, String referenceId) {
        InventoryBalance balance = getOrCreate(tenantId, warehouseCode, productSku);
        balance.setAvailableUnits(balance.getAvailableUnits() + quantity);
        balanceRepository.save(balance);
        saveMovement(tenantId, warehouseCode, productSku, MovementType.RECEIPT, quantity, "PURCHASE_ORDER", referenceId);
    }

    private InventoryBalance getOrCreate(String tenantId, String warehouseCode, String productSku) {
        return balanceRepository.findByTenantIdAndWarehouseCodeAndProductSku(tenantId, warehouseCode, productSku)
                .orElseGet(() -> {
                    InventoryBalance balance = new InventoryBalance();
                    balance.setTenantId(tenantId);
                    balance.setWarehouseCode(warehouseCode);
                    balance.setProductSku(productSku);
                    return balanceRepository.save(balance);
                });
    }

    private void saveMovement(String tenantId, String warehouseCode, String productSku, MovementType type,
                               int quantity, String referenceType, String referenceId) {
        InventoryMovement movement = new InventoryMovement();
        movement.setTenantId(tenantId);
        movement.setWarehouseCode(warehouseCode);
        movement.setProductSku(productSku);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movementRepository.save(movement);
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
