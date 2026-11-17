package com.supplymind.operations.procurement;

import com.supplymind.operations.common.InvalidStateTransitionException;
import com.supplymind.operations.common.NotFoundException;
import com.supplymind.operations.common.TenantContext;
import com.supplymind.operations.common.TenantRequiredException;
import com.supplymind.operations.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Procurement Agent (README §5) may draft a PO's contents, but only a human calling
 * submit() moves it past DRAFT - "it cannot independently authorize or submit them".
 */
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository repository;
    private final InventoryService inventoryService;

    public PurchaseOrderService(PurchaseOrderRepository repository, InventoryService inventoryService) {
        this.repository = repository;
        this.inventoryService = inventoryService;
    }

    public List<PurchaseOrder> list() {
        return repository.findAllByTenantId(requireTenant());
    }

    public PurchaseOrder get(UUID id) {
        return repository.findByIdAndTenantId(id, requireTenant())
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + id));
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrderDtos.CreatePurchaseOrderRequest request) {
        String tenantId = requireTenant();
        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(tenantId);
        po.setSupplierId(request.supplierId());
        po.setWarehouseCode(request.warehouseCode());
        po.setSourceType(request.sourceType());
        po.setSourceId(request.sourceId());

        BigDecimal total = BigDecimal.ZERO;
        for (var itemRequest : request.items()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setTenantId(tenantId);
            item.setProductSku(itemRequest.productSku());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            po.addItem(item);
            total = total.add(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }
        po.setTotalValue(total);
        return repository.save(po);
    }

    @Transactional
    public PurchaseOrder submit(UUID id) {
        PurchaseOrder po = get(id);
        requireStatus(po, PurchaseOrderStatus.DRAFT);
        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        return repository.save(po);
    }

    @Transactional
    public PurchaseOrder receive(UUID id) {
        PurchaseOrder po = get(id);
        requireStatus(po, PurchaseOrderStatus.SUBMITTED);

        for (PurchaseOrderItem item : po.getItems()) {
            inventoryService.receive(po.getTenantId(), po.getWarehouseCode(), item.getProductSku(),
                    item.getQuantity(), po.getId().toString());
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);
        return repository.save(po);
    }

    private void requireStatus(PurchaseOrder po, PurchaseOrderStatus expected) {
        if (po.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "Purchase order " + po.getId() + " is not " + expected + " (status=" + po.getStatus() + ")");
        }
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
